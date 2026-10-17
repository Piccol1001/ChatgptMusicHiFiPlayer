package com.piccol.hifi

import android.opengl.EGL14
import android.opengl.GLES20
import com.piccol.hifi.visualization.ReactiveShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class ReactiveShaderTest {
    @Test
    fun originalEffectsCompileAndRespondToSignalOnAndroidGpu() {
        val d = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        assertTrue(EGL14.eglInitialize(d, version, 0, version, 1))
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val count = IntArray(1)
        assertTrue(
            EGL14.eglChooseConfig(
                d,
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE,
                    4,
                    EGL14.EGL_SURFACE_TYPE,
                    EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RED_SIZE,
                    8,
                    EGL14.EGL_GREEN_SIZE,
                    8,
                    EGL14.EGL_BLUE_SIZE,
                    8,
                    EGL14.EGL_NONE,
                ),
                0,
                configs,
                0,
                1,
                count,
                0,
            )
        )
        assertTrue(count[0] > 0)
        val context =
            EGL14.eglCreateContext(
                d,
                configs[0],
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0,
            )
        val surface =
            EGL14.eglCreatePbufferSurface(
                d,
                configs[0],
                intArrayOf(EGL14.EGL_WIDTH, 32, EGL14.EGL_HEIGHT, 32, EGL14.EGL_NONE),
                0,
            )
        try {
            assertTrue(EGL14.eglMakeCurrent(d, surface, surface, context))
            val p = ReactiveShader.program()
            GLES20.glUseProgram(p)
            GLES20.glViewport(0, 0, 32, 32)
            val vertices =
                ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                    put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
                    position(0)
                }
            val position = GLES20.glGetAttribLocation(p, "position")
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, vertices)
            fun uniform(name: String, value: Float) =
                GLES20.glUniform1f(GLES20.glGetUniformLocation(p, name), value)
            GLES20.glUniform2f(GLES20.glGetUniformLocation(p, "resolution"), 32f, 32f)
            GLES20.glUniform3f(GLES20.glGetUniformLocation(p, "tint"), .4f, .65f, 1f)
            uniform("time", 1f)
            uniform("quality", 8f)
            fun render(active: Boolean): Long {
                uniform("level", if (active) .2f else 0f)
                GLES20.glUniform3f(
                    GLES20.glGetUniformLocation(p, "energy"),
                    if (active) .3f else 0f,
                    if (active) .2f else 0f,
                    if (active) .1f else 0f,
                )
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                val pixels = ByteBuffer.allocateDirect(4096)
                GLES20.glReadPixels(0, 0, 32, 32, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
                assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())
                return (0 until 1024).sumOf { i ->
                    (0..2).sumOf { c -> (pixels.get(i * 4 + c).toInt() and 255).toLong() }
                }
            }
            for (effect in 0..2) {
                uniform("effect", effect.toFloat())
                assertEquals(0L, render(false))
                assertTrue(render(true) > 1000L)
            }
            GLES20.glDeleteProgram(p)
        } finally {
            EGL14.eglMakeCurrent(
                d,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            EGL14.eglDestroySurface(d, surface)
            EGL14.eglDestroyContext(d, context)
            EGL14.eglTerminate(d)
        }
    }
}
