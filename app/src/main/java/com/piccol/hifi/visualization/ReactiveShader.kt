package com.piccol.hifi.visualization

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/** Original ES 2.0 shaders. No projectM engine or third-party presets. */
object ReactiveShader {
    const val VERTEX = "attribute vec2 position; void main(){gl_Position=vec4(position,0.0,1.0);}"
    const val FRAGMENT =
        """
        precision mediump float;
        uniform vec2 resolution;
        uniform float time, level, effect, quality;
        uniform vec3 energy, tint;
        void main(){
            if(level==0.0 && max(energy.x,max(energy.y,energy.z))==0.0){gl_FragColor=vec4(0.0,0.0,0.0,1.0);return;}
            vec2 uv=(2.0*gl_FragCoord.xy-resolution)/min(resolution.x,resolution.y);
            float r=length(uv),a=atan(uv.y,uv.x),v=0.0;
            if(effect<0.5){
                v=pow(abs(sin(r*(12.0+energy.y*15.0)-time*(0.6+energy.x*6.0)+sin(a*5.0+time)*energy.z*3.0)),12.0)/(1.0+r*3.0);
            }else if(effect<1.5){
                for(int i=0;i<12;i++){
                    if(float(i)>quality)break;
                    float p=float(i)*0.5236+time*(0.3+energy.y);
                    vec2 c=vec2(cos(p),sin(p))*(0.3+energy.x*1.5);
                    v+=0.008/(0.015+length(uv-c));
                }
            }else{
                v=pow(abs(sin(uv.x*(8.0+energy.x*12.0)+time)*sin(uv.y*(9.0+energy.z*15.0)-time)),5.0);
                v+=0.15/(0.05+abs(r-0.5-energy.y*0.3));
            }
            float audible=clamp(level*8.0+max(energy.x,max(energy.y,energy.z))*3.0,0.0,1.0);
            vec3 color=tint*(0.4+0.6*vec3(sin(a+time)*0.5+0.5,sin(a+2.0)*0.5+0.5,1.0));
            gl_FragColor=vec4(clamp(color*v*audible,0.0,1.0),1.0);
        }
    """

    fun program(): Int {
        fun compile(type: Int, code: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
            val ok = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0)
            check(ok[0] != 0) { GLES20.glGetShaderInfoLog(shader) }
            return shader
        }
        val vertex = compile(GLES20.GL_VERTEX_SHADER, VERTEX)
        val fragment = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, ok, 0)
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        check(ok[0] != 0) { GLES20.glGetProgramInfoLog(program) }
        return program
    }
}

data class ReactiveUniforms(
    val time: Float = 0f,
    val level: Float = 0f,
    val bass: Float = 0f,
    val mid: Float = 0f,
    val high: Float = 0f,
    val effect: Int = 0,
    val quality: Int = 1,
    val theme: Int = 0,
)

fun reactiveUniforms(frame: VisualFrame, settings: VisualSettings): ReactiveUniforms {
    var bass = 0f
    var mid = 0f
    var high = 0f
    frame.spectrum.forEachIndexed { i, db ->
        val f =
            if (settings.logarithmic)
                20.0 * (min(20000.0, frame.rate * .5) / 20).pow((i + .5) / frame.spectrum.size)
            else min(20000.0, frame.rate * .5) * (i + .5) / frame.spectrum.size
        val amplitude = 10.0.pow(db / 20.0).toFloat()
        when {
            f < 200 -> bass = max(bass, amplitude)
            f < 3000 -> mid = max(mid, amplitude)
            else -> high = max(high, amplitude)
        }
    }
    return ReactiveUniforms(
        if (settings.reducedMotion) 0f else frame.seconds,
        if (frame.playing) 10.0.pow(max(frame.rmsL, frame.rmsR) / 20.0).toFloat() else 0f,
        if (frame.playing) bass else 0f,
        if (frame.playing) mid else 0f,
        if (frame.playing) high else 0f,
        settings.effect,
        settings.quality.ordinal,
        settings.theme,
    )
}

class ReactiveSurface(context: Context, private val failure: (String) -> Unit) :
    GLSurfaceView(context) {
    @Volatile var uniforms = ReactiveUniforms()
    private var scale = 1f

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = false
        setRenderer(Renderer())
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun quality(quality: VisualQuality) {
        val next =
            when (quality) {
                VisualQuality.LOW -> .5f
                VisualQuality.MEDIUM -> .75f
                VisualQuality.HIGH -> 1f
            }
        if (next != scale) {
            scale = next
            if (width > 0)
                holder.setFixedSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0)
            holder.setFixedSize(
                (w * scale).toInt().coerceAtLeast(1),
                (h * scale).toInt().coerceAtLeast(1),
            )
    }

    private inner class Renderer : GLSurfaceView.Renderer {
        private var program = 0
        private var width = 1
        private var height = 1
        private val vertices =
            ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
                position(0)
            }
        private var position = 0
        private var resolution = 0
        private var time = 0
        private var level = 0
        private var energy = 0
        private var effect = 0
        private var quality = 0
        private var tint = 0

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            try {
                program = ReactiveShader.program()
                position = GLES20.glGetAttribLocation(program, "position")
                resolution = GLES20.glGetUniformLocation(program, "resolution")
                time = GLES20.glGetUniformLocation(program, "time")
                level = GLES20.glGetUniformLocation(program, "level")
                energy = GLES20.glGetUniformLocation(program, "energy")
                effect = GLES20.glGetUniformLocation(program, "effect")
                quality = GLES20.glGetUniformLocation(program, "quality")
                tint = GLES20.glGetUniformLocation(program, "tint")
            } catch (_: Exception) {
                program = 0
                post {
                    failure(
                        "Reactive renderer unavailable on this device. Choose another visualizer."
                    )
                }
            }
        }

        override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
            width = w
            height = h
            GLES20.glViewport(0, 0, w, h)
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            if (program == 0) return
            val u = uniforms
            GLES20.glUseProgram(program)
            GLES20.glUniform2f(resolution, width.toFloat(), height.toFloat())
            GLES20.glUniform1f(time, u.time)
            GLES20.glUniform1f(level, u.level)
            GLES20.glUniform3f(energy, u.bass, u.mid, u.high)
            GLES20.glUniform1f(effect, u.effect.toFloat())
            GLES20.glUniform1f(quality, if (u.quality == 0) 4f else if (u.quality == 1) 8f else 12f)
            when (u.theme) {
                1 -> GLES20.glUniform3f(tint, 1f, .55f, .16f)
                2 -> GLES20.glUniform3f(tint, .25f, 1f, .65f)
                else -> GLES20.glUniform3f(tint, .4f, .65f, 1f)
            }
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, vertices)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }
    }
}
