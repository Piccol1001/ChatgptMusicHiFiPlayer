extends RefCounted
const PENGUIN = preload("res://assets/penguin.svg")
const BEAR = preload("res://assets/bear.svg")
const STONE = preload("res://assets/stone.svg")
const FROST = preload("res://assets/frost.svg")
const ICE = preload("res://assets/ice.svg")
const SUNSET = preload("res://assets/sunset.svg")
const VILLAGE = preload("res://assets/village.svg")

static func ellipse(c: CanvasItem, at: Vector2, size: Vector2, color: Color) -> void:
	var pts := PackedVector2Array()
	for i in range(48):
		pts.append(at+Vector2(cos(TAU*i/48.0),sin(TAU*i/48.0))*size)
	c.draw_colored_polygon(pts,color)

static func penguin(c: CanvasItem, p: Vector2, angle: float, s: float, flap: float = 0.0) -> void:
	c.draw_set_transform(p,angle,Vector2.ONE*s)
	c.draw_texture_rect(PENGUIN,Rect2(-27,-45,54,76),false)
	if absf(flap)>0.4:
		c.draw_line(Vector2(-18,0),Vector2(-30,-10-flap*8),Color("20313b"),5,true)
	c.draw_set_transform(Vector2.ZERO)

static func bear(c: CanvasItem, p: Vector2, down: bool, time: float) -> void:
	c.draw_set_transform(p,PI*0.5 if down else sin(time*1.8)*0.025,Vector2.ONE)
	c.draw_texture_rect(BEAR,Rect2(-36,-98,72,102),false)
	c.draw_set_transform(Vector2.ZERO)

static func polygon_texture(c: CanvasItem, points: PackedVector2Array, texture: Texture2D) -> void:
	var uv := PackedVector2Array()
	for p in points:
		uv.append(p/Vector2(texture.get_size()))
	c.draw_polygon(points,PackedColorArray([Color.WHITE]),uv,texture)

static func cliff(c: CanvasItem, points: PackedVector2Array) -> void:
	polygon_texture(c,points,STONE)
	c.draw_polyline(points,Color("49646b"),4,true)
	c.draw_polyline(points,Color("d4c756"),2,true)

static func ice_block(c: CanvasItem, rect: Rect2) -> void:
	c.draw_texture_rect(ICE,rect,true)
	c.draw_rect(rect,Color("087db8"),false,1.5)
	c.draw_line(rect.position+Vector2(1,1),Vector2(rect.end.x-1,rect.position.y+1),Color("c6faff"),3,true)
	c.draw_line(rect.position+Vector2(2,3),Vector2(rect.position.x+2,rect.end.y-3),Color("98efff"),2,true)
