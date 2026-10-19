extends Node2D
const Art = preload("res://scripts/art.gd")
var game
var font = preload("res://assets/LilitaOne-Regular.ttf")

func _ready() -> void:
	texture_repeat = CanvasItem.TEXTURE_REPEAT_ENABLED

func _draw() -> void:
	if game.config.is_empty():
		return
	var mode: int = game.before_pause if game.state == game.State.PAUSED else game.state
	if mode == game.State.MENU:
		draw_map()
	elif mode == game.State.LAUNCHING:
		draw_launch()
	else:
		draw_attack()

func text(at: Vector2,value: String,size: int,color: Color) -> void:
	draw_string_outline(font,at,value,HORIZONTAL_ALIGNMENT_LEFT,-1,size,5,Color("466a87"))
	draw_string(font,at,value,HORIZONTAL_ALIGNMENT_LEFT,-1,size,color)

func draw_map() -> void:
	draw_texture_rect(Art.SUNSET,Rect2(0,0,960,704),false)
	draw_rect(Rect2(0,125,960,579),Color("278fc9"))
	var island := PackedVector2Array([Vector2(15,205),Vector2(140,150),Vector2(270,185),Vector2(355,250),Vector2(310,320),Vector2(395,370),Vector2(367,458),Vector2(280,490),Vector2(200,605),Vector2(80,590),Vector2(10,520)])
	draw_colored_polygon(island,Color("b4dfed"))
	var inner := PackedVector2Array()
	for p in island:
		inner.append(Vector2(200,370)+(p-Vector2(200,370))*0.97-Vector2(0,8))
	draw_colored_polygon(inner,Color("f1fcff"))
	island = PackedVector2Array([Vector2(590,235),Vector2(700,192),Vector2(855,220),Vector2(960,310),Vector2(945,555),Vector2(800,645),Vector2(640,595),Vector2(552,430)])
	draw_colored_polygon(island,Color("b1dce9"))
	inner.clear()
	for p in island:
		inner.append(Vector2(760,420)+(p-Vector2(760,420))*0.97-Vector2(0,9))
	draw_colored_polygon(inner,Color("f1fcff"))
	for i in range(48):
		var p := Vector2(float((i*139)%950),float(160+(i*71)%525))
		draw_colored_polygon(PackedVector2Array([p+Vector2(-6,0),p+Vector2(1,-6),p+Vector2(10,0),p+Vector2(6,7)]),Color("b4e5ee"))
	for x in [70,125,215,695,780,850]:
		var p := Vector2(x,390+sin(float(x))*140)
		draw_colored_polygon(PackedVector2Array([p+Vector2(-33,25),p+Vector2(0,-45),p+Vector2(35,25)]),Color("94b9c8"))
		draw_colored_polygon(PackedVector2Array([p+Vector2(-17,-8),p+Vector2(0,-45),p+Vector2(18,-6),p+Vector2(2,-15)]),Color.WHITE)
	for i in range(9):
		var p := Vector2(245+i*51,550+sin(i*0.55)*24)
		Art.ellipse(self,p,Vector2(13,10),Color("617d8d"))
		Art.ellipse(self,p-Vector2(0,2),Vector2(10,7),Color("dce6e8"))
	Art.ellipse(self,Vector2(210,545),Vector2(29,17),Color("d59a26"))
	Art.ellipse(self,Vector2(210,539),Vector2(25,15),Color("f8e562"))
	Art.penguin(self,Vector2(210,511),0,1.0,sin(game.time*5))
	Art.bear(self,Vector2(770,539),false,game.time)
	for x in [115,150,185]:
		Art.ellipse(self,Vector2(x,330),Vector2(20,29),Color("c4e7fb"))
		Art.ellipse(self,Vector2(x,337),Vector2(7,16),Color("74a7ca"))
	text(Vector2(124,610),"MISSION 1",22,Color.WHITE)
	text(Vector2(675,580),"ICEBREAKER BAY",20,Color.WHITE)

func draw_launch() -> void:
	draw_texture_rect(Art.VILLAGE,Rect2(0,0,960,704),false)
	Art.cliff(self,PackedVector2Array([Vector2(-5,325),Vector2(285,325),Vector2(398,382),Vector2(398,573),Vector2(445,620),Vector2(750,620),Vector2(800,573),Vector2(800,394),Vector2(866,340),Vector2(965,404),Vector2(965,710),Vector2(-5,710),Vector2(-5,325)]))
	Art.cliff(self,PackedVector2Array([Vector2(-5,-5),Vector2(872,-5),Vector2(715,127),Vector2(-5,127),Vector2(-5,-5)]))
	draw_texture_rect(Art.FROST,Rect2(0,0,570,120),true,Color(1,1,1,.83))
	draw_texture_rect(Art.FROST,Rect2(0,445,328,259),true,Color(1,1,1,.88))
	draw_texture_rect(Art.FROST,Rect2(874,472,86,232),true,Color(1,1,1,.82))
	# Frost along the cavern ceiling and the yellow-edged rock rims.
	for i in range(21):
		var x := i*34.0+8
		draw_colored_polygon(PackedVector2Array([Vector2(x,127),Vector2(x+15,127),Vector2(x+8,141+(i*11)%25)]),Color("e2faff"))
	draw_polyline(PackedVector2Array([Vector2(0,326),Vector2(285,326),Vector2(397,382),Vector2(397,573),Vector2(444,620),Vector2(750,620),Vector2(800,573),Vector2(800,394),Vector2(866,340),Vector2(960,404)]),Color("d7c95e"),7,true)
	draw_line(Vector2(0,282),Vector2(580,282),Color("99865f"),9,true)
	draw_line(Vector2(0,307),Vector2(580,307),Color("7c765f"),5,true)
	for i in range(20):
		var x := i*30.0
		draw_line(Vector2(x,283),Vector2(x+15,306),Color("8d8669"),4,true)
		draw_line(Vector2(x+15,306),Vector2(x+30,283),Color("8d8669"),4,true)
	draw_line(Vector2(0,279),Vector2(580,279),Color("f0e8c7"),4,true)
	var queued := maxi(0,int(game.config.penguins)-game.attempts-1)
	for i in range(queued):
		var p := Vector2(546-i*53,249+sin(game.time*5+i)*2)
		Art.penguin(self,p,0,0.68,sin(game.time*5+i))
	if game.attempts < int(game.config.penguins) and game.launch_wait <= 0:
		var p: Vector2 = game.swing_position()
		var anchor := Vector2(game.config.rope_anchor[0],game.config.rope_anchor[1])
		draw_line(anchor,p-Vector2(0,30),Color("8a7951"),4,true)
		draw_line(anchor+Vector2(2,0),p+Vector2(2,-30),Color("d4c08c"),1,true)
		Art.penguin(self,p,clampf((p.x-anchor.x)/220,-0.8,0.8),0.75)
		if game.launch_ready():
			draw_set_transform(Vector2(450,365),-0.13)
			text(Vector2.ZERO,"FIRE!",49,Color("fff197"))
			draw_set_transform(Vector2.ZERO)
	draw_catapult()
	for ghost in game.ghosts:
		Art.penguin(self,ghost.p,ghost.angle,0.75)
	for e in game.effects:
		draw_circle(e.p,3+e.life*4,e.color)

func draw_catapult() -> void:
	var pivot := Vector2(600,528)
	var kick: float = game.catapult_kick
	draw_line(Vector2(545,613),pivot,Color("8c6550"),20,true)
	draw_line(pivot,Vector2(658,613),Color("8c6550"),20,true)
	draw_line(Vector2(548,608),Vector2(655,608),Color("c09a72"),13,true)
	for x in [548,655]:
		draw_circle(Vector2(x,612),19,Color("795a47"))
		draw_circle(Vector2(x-2,610),13,Color("b68f67"))
		draw_circle(Vector2(x-2,610),5,Color("89634b"))
	var end := pivot+Vector2(-104,39).rotated(kick*4)
	draw_line(pivot+Vector2(48,-25),end,Color("9c7953"),13,true)
	draw_line(pivot+Vector2(48,-29),end-Vector2(0,4),Color("d3b488"),3,true)
	draw_circle(pivot,23,Color("b98d66"))
	draw_circle(pivot,12,Color("977154"))
	Art.ellipse(self,end,Vector2(25,16),Color("7a8b8d"))
	Art.ellipse(self,end-Vector2(3,5),Vector2(23,13),Color("e8f8fa"))
	for p in [Vector2(-12,-8),Vector2(0,-15),Vector2(11,-7)]:
		draw_circle(end+p,8,Color("f8ffff"))
	Art.penguin(self,Vector2(674,548),0,0.68,sin(game.time*3))
	draw_line(Vector2(650,580),Vector2(698,580),Color("ab8457"),7,true)

func draw_attack() -> void:
	draw_texture_rect(Art.SUNSET,Rect2(0,0,960,704),false)
	var offset := Vector2(-game.camera_x,0)
	if game.shake>0 and game.state!=game.State.PAUSED:
		offset += Vector2(sin(game.time*93),cos(game.time*87))*game.shake
	draw_set_transform(offset)
	var width: float = game.config.world_width
	Art.cliff(self,PackedVector2Array([Vector2(-30,270),Vector2(25,300),Vector2(25,580),Vector2(100,650),Vector2(width-100,650),Vector2(width-25,575),Vector2(width-25,280),Vector2(width+30,245),Vector2(width+30,730),Vector2(-30,730),Vector2(-30,270)]))
	draw_polyline(PackedVector2Array([Vector2(24,300),Vector2(24,580),Vector2(100,650),Vector2(width-100,650),Vector2(width-25,575),Vector2(width-25,280)]),Color("effcff"),6,true)
	for block in game.blocks:
		if not block.broken:
			Art.ice_block(self,block.r)
	for bear in game.bears:
		var age: float = game.time-bear.down_time
		if bear.down:
			if age<0.95:
				Art.bear(self,bear.p+Vector2(age*28,-sin(age*PI)*55),true,game.time)
			continue
		Art.bear(self,bear.p,false,game.time)
	for bird in game.birds:
		if bird.mode=="done":
			continue
		var a := PI/2
		if bird.mode!="flight":
			a = Vector2(bird.v).angle()+PI/2
		for i in range(3):
			var p: Vector2 = bird.p-Vector2(bird.v).normalized()*float(32+i*12)
			draw_circle(p,3-i*0.65,Color(1,0.95,0.5,0.75-i*.15))
		Art.penguin(self,bird.p,a,0.78,sin(game.time*17))
	for e in game.effects:
		var color: Color = e.color
		color.a = clampf(e.life*2,0,1)
		draw_colored_polygon(PackedVector2Array([e.p+Vector2(-4,0),e.p+Vector2(0,-6),e.p+Vector2(5,0),e.p+Vector2(0,4)]),color)
	draw_set_transform(Vector2.ZERO)
