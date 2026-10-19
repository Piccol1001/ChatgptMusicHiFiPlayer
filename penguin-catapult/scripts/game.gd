extends Node2D
const HUD = preload("res://scripts/hud.gd")
const Sound = preload("res://scripts/sound.gd")
const SaveData = preload("res://scripts/local_save.gd")
const World = preload("res://scripts/world.gd")
enum State { MENU, LAUNCHING, TRANSITION, BRIEFING, ATTACKING, RESULTS, PAUSED }
var state: State = State.MENU
var before_pause: State = State.MENU
var config: Dictionary
var ui
var sound
var world
var save_data = SaveData.new()
var test_mode := false
var time := 0.0
var input_lock := 0.0
var attempts := 0
var successes := 0
var score := 0
var hits := 0
var swing_time := 0.0
var launch_wait := 0.0
var catapult_kick := 0.0
var transition_time := 0.0
var attack_time := 0.0
var spawned := 0
var resolved := 0
var team_wait := 0.0
var result_wait := 0.0
var camera_x := 0.0
var shake := 0.0
var birds: Array[Dictionary] = []
var bears: Array[Dictionary] = []
var blocks: Array[Dictionary] = []
var effects: Array[Dictionary] = []
var ghosts: Array[Dictionary] = []
var message := ""
var message_time := 0.0
var winner := false

func _ready() -> void:
	config = JSON.parse_string(FileAccess.get_file_as_string("res://levels/level_01.json"))
	save_data.load_data()
	world = World.new()
	world.game = self
	add_child(world)
	sound = Sound.new()
	add_child(sound)
	sound.muted = save_data.muted
	ui = HUD.new()
	ui.game = self
	add_child(ui)
	ui.start_requested.connect(start_level)
	ui.retry_requested.connect(start_level)
	ui.resume_requested.connect(toggle_pause)
	ui.pause_requested.connect(toggle_pause)
	ui.home_requested.connect(show_home)
	ui.sound_requested.connect(toggle_sound)
	ui.next_requested.connect(show_completion)
	ui.continue_requested.connect(begin_attack)
	ui.help_requested.connect(show_help)
	show_home()

func show_home() -> void:
	state = State.MENU
	input_lock = 0.25
	camera_x = 0
	sound.set_paused(false)
	ui.menu("start", "One mission. Six penguins.\nSwing, launch, then dive through the ice.")
	update_hud()

func toggle_sound() -> void:
	save_data.muted = not save_data.muted
	sound.muted = save_data.muted
	if not test_mode:
		save_data.save_data()
	ui.refresh_sound()

func start_level() -> void:
	state = State.LAUNCHING
	input_lock = 0.25
	time = 0.0
	attempts = 0
	successes = 0
	score = 0
	hits = 0
	swing_time = 0.0
	launch_wait = 0.65
	catapult_kick = 0
	transition_time = 0
	attack_time = 0
	spawned = 0
	resolved = 0
	team_wait = 0.3
	result_wait = 0
	camera_x = 0
	shake = 0
	winner = false
	birds.clear()
	bears.clear()
	blocks.clear()
	effects.clear()
	ghosts.clear()
	for p in config.bears:
		bears.append({"p":Vector2(p[0],p[1]),"down":false,"falling":false,"v":0.0,"fall_start":float(p[1]),"down_time":0.0})
	var tile: float = config.tile_size
	for row in config.platforms:
		for i in range(int(row[2])):
			blocks.append({"r":Rect2(row[0]+i*tile,row[1],tile,tile),"broken":false})
	for row in config.pillars:
		for i in range(int(row[2])):
			blocks.append({"r":Rect2(row[0],row[1]+i*tile,tile,tile),"broken":false})
	ui.hide_menu()
	sound.set_paused(false)
	feedback("Wait for FIRE!  •  Space / click",2.5)
	update_hud()

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_ESCAPE:
		if state == State.BRIEFING:
			return
		toggle_pause()
		get_viewport().set_input_as_handled()
		return
	var action: bool = (event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_SPACE) or (event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT)
	if action:
		act()
		get_viewport().set_input_as_handled()

func act() -> void:
	if input_lock > 0:
		return
	if state == State.LAUNCHING and launch_wait <= 0 and attempts < int(config.penguins):
		finish_launch(launch_ready())
	elif state == State.ATTACKING:
		var bird := active_bird()
		if not bird.is_empty() and bird.mode == "flight":
			bird.mode = "dive"
			bird.v = Vector2(config.dive_horizontal_speed,config.dive_speed)
			sound.play_cue("dive")
			feedback("",0)

func launch_ready() -> bool:
	var t := swing_time/float(config.swing_seconds)
	return t >= float(config.launch_window_start) and t <= float(config.launch_window_end)

func swing_position() -> Vector2:
	var t := clampf(swing_time/float(config.swing_seconds),0,1)
	var angle := lerpf(config.swing_start_angle,config.swing_end_angle,smoothstep(0,1,t))
	return Vector2(config.rope_anchor[0],config.rope_anchor[1])+Vector2(sin(angle),cos(angle))*float(config.rope_length)

func finish_launch(success: bool) -> void:
	attempts += 1
	catapult_kick = 0.4
	var position := swing_position()
	if success:
		successes += 1
		score += int(config.points_per_launch)
		ghosts.append({"p":position,"v":Vector2(460,-740),"life":1.5,"angle":0.5})
		feedback("OK!",0.7)
		sound.play_cue("launch")
		burst(position,Color("fff47b"),16)
	else:
		ghosts.append({"p":position,"v":Vector2(60,180),"life":0.8,"angle":0.0})
		feedback("Too soon!" if swing_time/float(config.swing_seconds) < float(config.launch_window_start) else "Miss!",0.85)
		sound.play_cue("miss")
	swing_time = 0
	launch_wait = float(config.launch_gap)

func begin_attack() -> void:
	if state != State.BRIEFING:
		return
	state = State.ATTACKING
	input_lock = 0.25
	ui.hide_menu()
	feedback("Space / click to dive. Keep an eye on the ice!",2.2)
	update_hud()

func toggle_pause() -> void:
	if state == State.PAUSED:
		state = before_pause
		ui.hide_menu()
		input_lock = 0.2
		sound.set_paused(false)
	elif state in [State.LAUNCHING,State.TRANSITION,State.ATTACKING]:
		before_pause = state
		state = State.PAUSED
		sound.set_paused(true)
		ui.menu("pause","Space / click: launch or dive\nEsc: resume\nAll movement is paused.")

func show_help() -> void:
	if state == State.MENU:
		ui.menu("help","CATAPULT\nPress Space or click when FIRE! appears.\n\nATTACK\nPress once to dive. Ice breaks and bears fall.\nHit %d bears before your team runs out." % int(config.required_hits))
	elif state in [State.LAUNCHING,State.ATTACKING]:
		toggle_pause()

func feedback(text_value: String,duration: float=1.0) -> void:
	message = text_value
	message_time = duration

func _physics_process(delta: float) -> void:
	input_lock = maxf(0,input_lock-delta)
	if state == State.PAUSED:
		return
	time += delta
	world.queue_redraw()
	if state in [State.MENU,State.RESULTS,State.BRIEFING]:
		return
	message_time = maxf(0,message_time-delta)
	catapult_kick = maxf(0,catapult_kick-delta)
	shake = maxf(0,shake-delta*20)
	update_effects(delta)
	match state:
		State.LAUNCHING:
			if launch_wait > 0:
				launch_wait -= delta
			elif attempts >= int(config.penguins):
				state = State.TRANSITION
				transition_time = 0
				input_lock = float(config.transition_seconds)+0.25
				feedback("AIRBORNE!",1.2)
			else:
				swing_time += delta
				if swing_time > float(config.swing_seconds):
					finish_launch(false)
		State.TRANSITION:
			transition_time += delta
			if transition_time >= float(config.transition_seconds):
				if successes == 0:
					finish_level()
				else:
					state = State.BRIEFING
					ui.menu("briefing","Hit %02d bears to complete the mission!\n\n%d penguins made it through.\nDive early: you keep moving forward." % [int(config.required_hits),successes])
		State.ATTACKING:
			update_attack(delta)
	update_hud()

func active_bird() -> Dictionary:
	for bird in birds:
		if bird.mode != "done":
			return bird
	return {}

func update_attack(delta: float) -> void:
	attack_time += delta
	var bird := active_bird()
	if bird.is_empty():
		team_wait -= delta
		if spawned < successes and team_wait <= 0:
			birds.append({"p":Vector2(50,config.flight_y),"v":Vector2(config.flight_speed,0),"mode":"flight","id":spawned,"age":0.0,"bounces":0})
			spawned += 1
			bird = active_bird()
	if not bird.is_empty():
		bird.age += delta
		var old: Vector2 = bird.p
		if bird.mode != "flight":
			bird.v.y += float(config.gravity)*delta
		bird.p += bird.v*delta
		if bird.mode != "flight":
			break_ice(old,bird.p)
			var target := swept_target(old,bird.p)
			if target >= 0:
				knock_bear(target)
				bird.bounces += 1
				if bird.bounces <= int(config.max_bounces):
					bird.mode = "bounce"
					bird.v = Vector2(config.bounce_horizontal_speed,-float(config.bounce_speed))
				else:
					resolve_bird(bird)
			if bird.p.y >= float(config.ground_y)+18:
				burst(bird.p,Color("c3f9ff"),18)
				resolve_bird(bird)
		if bird.p.x > float(config.world_width)+40 or bird.age > float(config.bird_timeout):
			resolve_bird(bird)
		var target_x := clampf(bird.p.x-280.0,0,float(config.world_width)-960)
		camera_x = lerpf(camera_x,target_x,minf(1,delta*7))
	else:
		camera_x = lerpf(camera_x,0,minf(1,delta*9))
	update_bears(delta)
	if spawned == successes and resolved == successes and not bears.any(func(b): return b.falling and not b.down):
		result_wait += delta
		if result_wait >= float(config.result_delay):
			finish_level()

func resolve_bird(bird: Dictionary) -> void:
	if bird.mode == "done":
		return
	bird.mode = "done"
	resolved += 1
	team_wait = float(config.team_gap)

func swept_target(from: Vector2,to: Vector2) -> int:
	var nearest := INF
	var target := -1
	for i in range(bears.size()):
		if bears[i].down:
			continue
		var p: Vector2 = bears[i].p
		var rect := Rect2(p-Vector2(config.bear_half_width,config.bear_height),Vector2(float(config.bear_half_width)*2,config.bear_height)).grow(float(config.penguin_radius))
		var t := segment_rect(from,to,rect)
		if t >= 0 and t < nearest:
			nearest = t
			target = i
	return target

func segment_rect(a: Vector2,b: Vector2,rect: Rect2) -> float:
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

func break_ice(a: Vector2,b: Vector2) -> void:
	for block in blocks:
		if not block.broken and segment_rect(a,b,block.r.grow(8)) >= 0:
			block.broken = true
			burst(block.r.get_center(),Color("39d9ff"),12)
			shake = maxf(shake,2.0)

func knock_bear(index: int) -> void:
	var bear: Dictionary = bears[index]
	if bear.down:
		return
	bear.down = true
	bear.falling = false
	bear.down_time = time
	hits += 1
	score += int(config.points_per_bear)
	shake = 5.0
	burst(bear.p-Vector2(0,45),Color("ffe35a"),18)
	feedback("%02d / %02d" % [hits,int(config.required_hits)],0.8)
	sound.play_cue("hit")

func update_bears(delta: float) -> void:
	for i in range(bears.size()):
		var bear: Dictionary = bears[i]
		if bear.down:
			continue
		var supported: bool = bear.p.y >= float(config.ground_y)-1
		for block in blocks:
			if not block.broken and absf(bear.p.y-block.r.position.y) < 2 and bear.p.x >= block.r.position.x and bear.p.x <= block.r.end.x:
				supported = true
		if supported and not bear.falling:
			continue
		if not bear.falling:
			bear.falling = true
			bear.fall_start = bear.p.y
			bear.v = 0.0
		var old_y: float = bear.p.y
		bear.v += float(config.gravity)*delta
		bear.p.y += bear.v*delta
		var landing: float = config.ground_y
		for block in blocks:
			if not block.broken and bear.p.x >= block.r.position.x and bear.p.x <= block.r.end.x and old_y <= block.r.position.y+1 and bear.p.y >= block.r.position.y:
				landing = minf(landing,block.r.position.y)
		if bear.p.y >= landing:
			bear.p.y = landing
			bear.falling = false
			if landing-float(bear.fall_start) >= float(config.fall_ko_distance):
				knock_bear(i)

func remaining_targets() -> int:
	return maxi(0,int(config.required_hits)-hits)

func finish_level() -> void:
	state = State.RESULTS
	winner = hits >= int(config.required_hits)
	if not test_mode:
		save_data.best_score = maxi(save_data.best_score,score)
		save_data.completed = save_data.completed or winner
		save_data.save_data()
	sound.play_cue("win" if winner else "miss")
	var copy := "Bears hit: %02d / %02d\n\nBest score: %d" % [hits,int(config.required_hits),maxi(score,save_data.best_score)]
	if successes == 0:
		copy = "No penguins launched.\nWait until FIRE! appears, then press once."
	ui.menu("result",copy,winner)
	update_hud()

func show_completion() -> void:
	ui.menu("complete","Mission 1 complete!\n\nThis build contains one complete mission.\nReplay to improve your launch timing.")

func update_hud() -> void:
	var launch := state == State.LAUNCHING or (state == State.PAUSED and before_pause == State.LAUNCHING)
	ui.set_status(int(config.penguins)-attempts if launch else successes-spawned,hits,int(config.required_hits),save_data.best_score,not launch and state != State.MENU)
	ui.feedback.text = message if message_time > 0 and state not in [State.MENU,State.RESULTS,State.BRIEFING] else ""

func burst(p: Vector2,color: Color,count: int) -> void:
	for i in range(count):
		var angle := TAU*i/count
		effects.append({"p":p,"v":Vector2(cos(angle),sin(angle))*float(80+(i*37)%160),"life":0.65,"color":color})

func update_effects(delta: float) -> void:
	for e in effects:
		e.life -= delta
		e.v.y += 420*delta
		e.p += e.v*delta
	effects = effects.filter(func(e): return e.life > 0)
	for ghost in ghosts:
		ghost.life -= delta
		ghost.v.y += 240*delta
		ghost.p += ghost.v*delta
	ghosts = ghosts.filter(func(e): return e.life > 0)
