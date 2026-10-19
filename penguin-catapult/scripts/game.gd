extends Node2D

const Art = preload("res://scripts/art.gd")
const HUD = preload("res://scripts/hud.gd")
const Sound = preload("res://scripts/sound.gd")
const SaveData = preload("res://scripts/local_save.gd")
enum State { MENU, LAUNCHING, TRANSITION, ATTACKING, RESULTS, PAUSED }
var state: State = State.MENU
var before_pause: State = State.MENU
var config: Dictionary
var ui: CanvasLayer
var sound: Node
var save_data = SaveData.new()
var camera: Camera2D
var time := 0.0
var input_lock := 0.0
var launch_x := -60.0
var launch_wait := 0.0
var attempts := 0
var successes := 0
var score := 0
var transition_time := 0.0
var attack_time := 0.0
var next_spawn := 0.0
var spawned := 0
var resolved := 0
var result_wait := 0.0
var birds: Array[Dictionary] = []
var bears: Array[Dictionary] = []
var effects: Array[Dictionary] = []
var launch_ghosts: Array[Dictionary] = []
var message := ""
var message_time := 0.0
var catapult_kick := 0.0
var shake := 0.0
var winner := false
var test_mode := false

func _ready() -> void:
	config = JSON.parse_string(FileAccess.get_file_as_string("res://levels/level_01.json"))
	save_data.load_data()
	camera = Camera2D.new()
	camera.position = Vector2(640,360)
	add_child(camera)
	sound = Sound.new()
	add_child(sound)
	sound.muted = save_data.muted
	ui = HUD.new()
	add_child(ui)
	ui.start_requested.connect(start_level)
	ui.retry_requested.connect(start_level)
	ui.resume_requested.connect(toggle_pause)
	ui.pause_requested.connect(toggle_pause)
	ui.home_requested.connect(show_home)
	ui.sound_requested.connect(toggle_sound)
	ui.next_requested.connect(show_completion)
	show_home()

func toggle_sound() -> void:
	save_data.muted = not save_data.muted
	sound.muted = save_data.muted
	save_data.save_data()
	show_home()

func show_home() -> void:
	state = State.MENU
	sound.set_paused(false)
	camera.position = Vector2(640,360)
	input_lock = 0.25
	ui.menu("start", "Space / left click: launch, then dive.\nLaunch as each penguin enters the gold cradle.\nEsc: pause.  •  Sound: %s  •  Best: %d" % ["OFF" if save_data.muted else "ON", save_data.best_score])
	update_hud()

func start_level() -> void:
	state = State.LAUNCHING
	input_lock = 0.25
	time = 0.0
	attempts = 0
	successes = 0
	score = 0
	launch_x = config.launch_start_x
	launch_wait = 0.4
	transition_time = 0.0
	attack_time = 0.0
	next_spawn = 0.0
	spawned = 0
	resolved = 0
	result_wait = 0.0
	catapult_kick = 0.0
	shake = 0.0
	winner = false
	birds.clear()
	bears.clear()
	effects.clear()
	launch_ghosts.clear()
	for x in config.bears:
		bears.append({"x": float(x), "down": false})
	camera.position = Vector2(640,360)
	ui.hide_menu()
	sound.set_paused(false)
	feedback("Time your launch inside the gold cradle!", 2.5)
	update_hud()

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_ESCAPE:
		toggle_pause()
		get_viewport().set_input_as_handled()
		return
	var is_action: bool = (event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_SPACE) or (event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT)
	if is_action:
		act()
		get_viewport().set_input_as_handled()

func act() -> void:
	if input_lock > 0.0:
		return
	match state:
		State.LAUNCHING:
			if launch_wait <= 0.0 and attempts < int(config.penguins):
				finish_launch(absf(launch_x - float(config.launch_x)) <= float(config.launch_half_window))
		State.ATTACKING:
			for bird in birds:
				if bird.mode == "flight" and bird.p.x >= 0.0:
					bird.mode = "dive"
					bird.v = Vector2(config.dive_horizontal_speed, config.dive_speed)
					sound.play_cue("dive")
					feedback("DIVE!",0.5)
					break

func toggle_pause() -> void:
	if state == State.PAUSED:
		state = before_pause
		ui.hide_menu()
		input_lock = 0.2
		sound.set_paused(false)
	elif state in [State.LAUNCHING, State.TRANSITION, State.ATTACKING]:
		before_pause = state
		state = State.PAUSED
		sound.set_paused(true)
		ui.menu("pause", "The snow can wait.\nSpace or left click performs the current action.\nEsc resumes the exact same moment.")

func feedback(text_value: String, duration: float = 1.0) -> void:
	message = text_value
	message_time = duration

func finish_launch(success: bool) -> void:
	attempts += 1
	catapult_kick = 0.45
	if success:
		successes += 1
		score += int(config.points_per_launch)
		launch_ghosts.append({"p": Vector2(config.launch_x,515), "v": Vector2(420,-720), "life": 1.8, "success": true})
		feedback("GREAT LAUNCH!  +1 penguin",0.9)
		sound.play_cue("launch")
		burst(Vector2(config.launch_x,540),Color("ffbd59"),12)
	else:
		launch_ghosts.append({"p": Vector2(launch_x,535), "v": Vector2(100,-120), "life": 0.7, "success": false})
		feedback("Too early!" if launch_x < float(config.launch_x) else "Too late!",0.9)
		sound.play_cue("miss")
	launch_wait = float(config.launch_gap)
	launch_x = config.launch_start_x

func _physics_process(delta: float) -> void:
	input_lock = maxf(0.0,input_lock-delta)
	if state == State.PAUSED:
		queue_redraw()
		return
	if state in [State.MENU,State.RESULTS]:
		queue_redraw()
		return
	time += delta
	message_time -= delta
	catapult_kick = maxf(0.0,catapult_kick-delta)
	shake = maxf(0.0,shake-delta*22.0)
	camera.offset = Vector2(sin(time*103),cos(time*91))*shake
	update_effects(delta)
	match state:
		State.LAUNCHING:
			if launch_wait > 0:
				launch_wait -= delta
			elif attempts >= int(config.penguins):
				state = State.TRANSITION
				transition_time = 0.0
				input_lock = float(config.transition_seconds) + 0.2
				feedback("%d PENGUINS AIRBORNE • GET READY TO DIVE" % successes,3.0)
			else:
				launch_x += float(config.approach_speed)*delta
				if launch_x > float(config.launch_miss_x):
					finish_launch(false)
		State.TRANSITION:
			transition_time += delta
			var ratio := clampf(transition_time / float(config.transition_seconds),0,1)
			camera.position.x = 640.0 + float(config.attack_offset)*smoothstep(0.0,1.0,ratio)
			if ratio >= 1.0:
				state = State.ATTACKING
				input_lock = 0.2
				feedback("Dive the marked penguin onto a bear!",2.0)
		State.ATTACKING:
			update_attack(delta)
	update_hud()
	queue_redraw()

func update_attack(delta: float) -> void:
	attack_time += delta
	if spawned < successes and attack_time >= next_spawn:
		birds.append({"p": Vector2(-65,config.flight_y),"v": Vector2(config.flight_speed,0),"mode": "flight", "id": spawned})
		spawned += 1
		next_spawn += float(config.flight_spacing_seconds)
	for bird in birds:
		if bird.mode == "done":
			continue
		var old: Vector2 = bird.p
		if bird.mode == "dive":
			bird.v.y += float(config.gravity)*delta
		bird.p += bird.v*delta
		if bird.mode == "dive":
			var hit := swept_target(old,bird.p)
			if hit >= 0:
				bears[hit].down = true
				score += int(config.points_per_bear)
				resolve_bird(bird,true)
				continue
			if bird.p.y >= float(config.ground_y)-10:
				resolve_bird(bird,false)
		elif bird.p.x > 1350:
			resolve_bird(bird,false)
	# Once the objective is clear, retire unused flights but settle every active dive.
	if remaining_targets() == 0:
		resolved += successes-spawned
		spawned = successes
		for bird in birds:
			if bird.mode == "flight":
				bird.mode = "done"
				resolved += 1
	# No defeat while another penguin still has a flight or a dive to resolve.
	if spawned == successes and resolved == successes:
		result_wait += delta
		if result_wait >= float(config.result_delay):
			finish_level()

func swept_target(from: Vector2, to: Vector2) -> int:
	var nearest := INF
	var result := -1
	for i in range(bears.size()):
		if bears[i].down:
			continue
		var rect := Rect2(Vector2(bears[i].x-float(config.bear_half_width),float(config.ground_y)-float(config.bear_height)),Vector2(float(config.bear_half_width)*2,float(config.bear_height))).grow(float(config.penguin_radius))
		var fraction := segment_rect(from,to,rect)
		if fraction >= 0 and fraction < nearest:
			nearest = fraction
			result = i
	return result

# Slab intersection checks the complete movement segment, even at extreme dive speeds.
func segment_rect(a: Vector2, b: Vector2, rect: Rect2) -> float:
	var low := 0.0
	var high := 1.0
	var movement := b-a
	for axis in range(2):
		if absf(movement[axis]) < 0.00001:
			if a[axis] < rect.position[axis] or a[axis] > rect.end[axis]:
				return -1.0
		else:
			var t1: float = (rect.position[axis]-a[axis])/movement[axis]
			var t2: float = (rect.end[axis]-a[axis])/movement[axis]
			low = maxf(low,minf(t1,t2))
			high = minf(high,maxf(t1,t2))
			if low > high:
				return -1.0
	return low

func resolve_bird(bird: Dictionary, hit: bool) -> void:
	bird.mode = "done"
	resolved += 1
	var point := Vector2(float(config.attack_offset)+bird.p.x,minf(bird.p.y,float(config.ground_y)-12))
	burst(point,Color("ffbe5b") if hit else Color.WHITE,22 if hit else 10)
	if hit:
		shake = 7.0
		feedback("KAPOW!  +%d" % int(config.points_per_bear),0.8)
		sound.play_cue("hit")
	else:
		feedback("Snow splash!" if bird.p.y > 400 else "Flew past!",0.6)
		sound.play_cue("miss")

func remaining_targets() -> int:
	var count := 0
	for bear in bears:
		if not bear.down:
			count += 1
	return count

func finish_level() -> void:
	state = State.RESULTS
	camera.offset = Vector2.ZERO
	winner = remaining_targets() == 0
	if not test_mode:
		save_data.best_score = maxi(save_data.best_score,score)
		save_data.completed = save_data.completed or winner
		save_data.save_data()
	sound.play_cue("win" if winner else "miss")
	var copy := "%d / %d launched  •  %d / %d bears hit\nScore %d  •  Best %d\n%s" % [successes,int(config.penguins),bears.size()-remaining_targets(),bears.size(),score,maxi(score,save_data.best_score),"A clean sweep of the snowfield." if winner else "Try a little earlier: dives carry forward."]
	if successes == 0:
		copy = "No penguins made it to the attack.\nPress when a penguin reaches the gold cradle.\nRetry for a fresh team of eight."
	ui.menu("result",copy,winner)

func show_completion() -> void:
	ui.menu("complete","You finished the first playable level!\nThis build contains one level.\nReplay to improve your launch timing and score.")

func update_hud() -> void:
	match state:
		State.LAUNCHING:
			ui.hud.text = "01  /  CATAPULT     •     Team %d / %d     •     Airborne %d" % [attempts,int(config.penguins),successes]
			ui.detail.text = "SPACE / CLICK  Launch in the gold cradle     |     Objective: knock out all %d bears" % config.bears.size()
		State.TRANSITION:
			ui.hud.text = "02  /  INCOMING     •     %d penguins ready" % successes
			ui.detail.text = "Crossing the snowfield… Get ready to time your dives."
		State.ATTACKING,State.RESULTS:
			ui.hud.text = "02  /  ATTACK     •     Penguins %d     •     Bears %d     •     Score %d" % [successes-resolved,remaining_targets(),score]
			ui.detail.text = "SPACE / CLICK  Dive the marked penguin     |     Clear every bear. All dives resolve before results."
		State.MENU:
			ui.hud.text = "SNOWBOUND     /     LEVEL 01"
			ui.detail.text = "Eight penguins. Three bears. Two perfectly timed moves."
	ui.feedback.text = message if message_time > 0 and state not in [State.MENU,State.RESULTS] else ""

func burst(p: Vector2,color: Color,count: int) -> void:
	for i in range(count):
		var angle := TAU * i / count
		effects.append({"p":p,"v":Vector2(cos(angle),sin(angle))*float(100+(i*37)%190),"life":0.6,"color":color})

func update_effects(delta: float) -> void:
	for e in effects:
		e.life -= delta
		e.v.y += 500*delta
		e.p += e.v*delta
	effects = effects.filter(func(e): return e.life > 0)
	for ghost in launch_ghosts:
		ghost.life -= delta
		ghost.v.y += 600*delta
		ghost.p += ghost.v*delta
	launch_ghosts = launch_ghosts.filter(func(e): return e.life > 0)

func _draw() -> void:
	if config == null or config.is_empty():
		return
	draw_landscape()
	draw_catapult()
	if state == State.LAUNCHING and attempts < int(config.penguins) and launch_wait <= 0:
		Art.ellipse(self,Vector2(launch_x,570),Vector2(23,5),Color(0.13,0.36,0.48,0.18))
		Art.penguin(self,Vector2(launch_x,533-17*clampf(1.0-absf(launch_x-float(config.launch_x))/65.0,0,1)+sin(time*16)*3),sin(time*14)*0.07,1.0,sin(time*12))
		for i in range(mini(4,int(config.penguins)-attempts-1)):
			Art.penguin(self,Vector2(launch_x-75*(i+1),537),0,0.85,sin(time*12+i))
	for ghost in launch_ghosts:
		Art.penguin(self,ghost.p,time*8,0.85)
	var offset: float = config.attack_offset
	for bear in bears:
		Art.ellipse(self,Vector2(offset+bear.x,571),Vector2(48,8),Color(0.13,0.36,0.48,0.14))
		Art.bear(self,Vector2(offset+bear.x,530 if not bear.down else 550),bear.down,time)
		if bear.down:
			for j in range(3):
				var a := time*3+j*TAU/3
				draw_circle(Vector2(offset+bear.x+cos(a)*28,488+sin(a)*8),4,Color("f4b84c"))
	var marked := false
	for bird in birds:
		if bird.mode == "done":
			continue
		var p: Vector2 = bird.p + Vector2(offset,0)
		if bird.mode == "flight":
			Art.penguin(self,p+Vector2(0,sin(time*8+bird.id)*3),PI*0.37,0.8,sin(time*18))
			if not marked and bird.p.x >= 0:
				marked = true
				draw_colored_polygon(PackedVector2Array([p+Vector2(-11,-60),p+Vector2(11,-60),p+Vector2(0,-44)]),Color("ef685c"))
		else:
			for i in range(3):
				draw_line(p-Vector2(10+i*6,28+i*16),p-Vector2(18+i*6,58+i*16),Color(1,1,1,0.6),3,true)
			Art.penguin(self,p,PI-0.25,0.8)
	for e in effects:
		var tint: Color = e.color
		tint.a = clampf(e.life*2,0,1)
		draw_circle(e.p,3+e.life*5,tint)

func draw_landscape() -> void:
	draw_rect(Rect2(-1280,-200,5120,1000),Color("b1e4f1"))
	for i in range(14):
		var x := float(i*290-300)
		draw_colored_polygon(PackedVector2Array([Vector2(x,520),Vector2(x+165,210+(i%3)*35),Vector2(x+380,520)]),Color("81bfda"))
		draw_colored_polygon(PackedVector2Array([Vector2(x+98,330+(i%3)*25),Vector2(x+165,210+(i%3)*35),Vector2(x+250,335+(i%3)*25),Vector2(x+177,310+(i%3)*25)]),Color("e7f6fc"))
	for i in range(11):
		var p := Vector2(i*330-100,140+(i%3)*45)
		Art.ellipse(self,p,Vector2(74,18),Color("d9f2fa"))
		Art.ellipse(self,p+Vector2(18,-15),Vector2(36,24),Color("d9f2fa"))
	Art.ellipse(self,Vector2(500,600),Vector2(1000,180),Color("dceff7"))
	Art.ellipse(self,Vector2(2000,630),Vector2(1000,200),Color("eaf8fd"))
	draw_rect(Rect2(-1280,570,5120,400),Color("effaff"))
	draw_rect(Rect2(-1280,570,5120,8),Color.WHITE)
	draw_rect(Rect2(-1280,680,5120,60),Color("cee7f1"))
	for i in range(60):
		var x := float((i*191)%3600-300)
		var y := float(592+(i*17)%75)
		draw_line(Vector2(x,y),Vector2(x+15,y-2),Color("d2e8f2"),2,true)
	# Signposts frame the playable areas without changing the camera zoom.
	for offset in [0.0,float(config.attack_offset)]:
		draw_line(Vector2(offset+1140,570),Vector2(offset+1140,474),Color("8e6855"),9)
		draw_style_box(sign_style(),Rect2(offset+1064,468,152,43))
		draw_string(ThemeDB.fallback_font,Vector2(offset+1080,496),"LAUNCH" if offset == 0 else "BEAR BAY",HORIZONTAL_ALIGNMENT_LEFT,-1,20,Color("f5f9f2"))

func sign_style() -> StyleBoxFlat:
	var s := StyleBoxFlat.new()
	s.bg_color = Color("367c8e")
	s.corner_radius_top_left = 6
	s.corner_radius_top_right = 6
	s.corner_radius_bottom_left = 6
	s.corner_radius_bottom_right = 6
	return s

func draw_catapult() -> void:
	var x: float = config.launch_x
	var window: float = config.launch_half_window
	draw_rect(Rect2(x-window,560,window*2,10),Color("ffbf58"))
	draw_arc(Vector2(x,538),43,PI,TAU,24,Color("ffbf58"),3,true)
	for base in [x+45,x+105]:
		draw_line(Vector2(base,568),Vector2(x+80,516),Color("644e48"),15,true)
		draw_circle(Vector2(base,568),12,Color("37566b"))
	var pivot := Vector2(x+80,526)
	var angle := -0.28+catapult_kick*3
	var end := pivot+Vector2(-80,0).rotated(angle)
	draw_line(pivot,end,Color("97745c"),13,true)
	draw_line(pivot+Vector2(25,0),end+Vector2(-8,0),Color("dab78c"),3,true)
	draw_arc(end,19,0,PI,16,Color("644e48"),7,true)
	draw_circle(pivot,9,Color("e5bd75"))
