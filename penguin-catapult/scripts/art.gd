extends RefCounted
# Replace this drawing adapter with sprite scenes when approved reference assets exist.
const INK := Color("193e55")
const SNOW := Color("f6fcff")
const ORANGE := Color("ffae47")

static func ellipse(canvas: CanvasItem, at: Vector2, size: Vector2, color: Color) -> void:
	var points := PackedVector2Array()
	for i in range(40):
		var a := TAU * i / 40.0
		points.append(at + Vector2(cos(a), sin(a)) * size)
	canvas.draw_colored_polygon(points, color)

static func penguin(c: CanvasItem, p: Vector2, angle: float, scale_value: float, flap: float = 0.0) -> void:
	c.draw_set_transform(p, angle, Vector2.ONE * scale_value)
	ellipse(c, Vector2(-21, 3), Vector2(9, 23), INK)
	ellipse(c, Vector2(21, 3 + flap * 6), Vector2(9, 23), INK)
	ellipse(c, Vector2.ZERO, Vector2(22, 31), INK)
	ellipse(c, Vector2(2, 8), Vector2(16, 21), SNOW)
	ellipse(c, Vector2(-8, -13), Vector2(8, 10), SNOW)
	ellipse(c, Vector2(9, -13), Vector2(8, 10), SNOW)
	c.draw_circle(Vector2(-5, -12), 3.2, INK)
	c.draw_circle(Vector2(11, -12), 3.2, INK)
	c.draw_colored_polygon(PackedVector2Array([Vector2(-6,-3), Vector2(15,-3), Vector2(5,8)]), ORANGE)
	ellipse(c, Vector2(-13, 29), Vector2(12, 5), ORANGE)
	ellipse(c, Vector2(13, 29), Vector2(12, 5), ORANGE)
	c.draw_line(Vector2(-19,-1),Vector2(20,-1),Color("ef685c"),5)
	c.draw_line(Vector2(-17,0),Vector2(-30,10),Color("ef685c"),6)
	c.draw_set_transform(Vector2.ZERO)

static func bear(c: CanvasItem, p: Vector2, down: bool, time: float) -> void:
	c.draw_set_transform(p, PI * 0.5 if down else sin(time * 1.8) * 0.025, Vector2.ONE)
	ellipse(c, Vector2.ZERO, Vector2(34, 43), Color("b5d5e2"))
	ellipse(c, Vector2(0,-4), Vector2(31, 39), SNOW)
	c.draw_circle(Vector2(-22,-38),11,Color("b5d5e2"))
	c.draw_circle(Vector2(22,-38),11,Color("b5d5e2"))
	c.draw_circle(Vector2(-22,-38),6,Color("d4e5eb"))
	c.draw_circle(Vector2(22,-38),6,Color("d4e5eb"))
	ellipse(c,Vector2(0,-22),Vector2(31,27),SNOW)
	ellipse(c,Vector2(0,-12),Vector2(18,12),Color("d4e5eb"))
	ellipse(c,Vector2(0,-18),Vector2(8,5),INK)
	for x in [-13,13]:
		if down:
			c.draw_line(Vector2(x-4,-29),Vector2(x+4,-23),INK,2)
			c.draw_line(Vector2(x+4,-29),Vector2(x-4,-23),INK,2)
		else:
			c.draw_circle(Vector2(x,-28),3,INK)
			c.draw_line(Vector2(x-5,-37),Vector2(x+4,-34),INK,3)
	ellipse(c,Vector2(-23,33),Vector2(18,9),SNOW)
	ellipse(c,Vector2(23,33),Vector2(18,9),SNOW)
	c.draw_set_transform(Vector2.ZERO)
