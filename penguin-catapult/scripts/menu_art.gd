extends Node2D
const Art = preload("res://scripts/art.gd")
var phase:=0.0
func _process(delta: float) -> void:
	phase+=delta
	if visible:
		queue_redraw()
func _draw() -> void:
	Art.ellipse(self,Vector2(0,32),Vector2(95,13),Color(1,1,1,.65))
	Art.bear(self,Vector2(-44,20),true,0)
	Art.penguin(self,Vector2(2,-19+sin(phase*4)*4),0,.85,sin(phase*4))
