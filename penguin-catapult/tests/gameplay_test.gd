extends SceneTree
var game
var failures := 0
var checks := 0
const DT := 1.0/120.0
func check(value: bool,description: String) -> void:
	checks += 1
	if value:
		print("PASS: ",description)
	else:
		failures += 1
		push_error("FAIL: "+description)
func tick(frames: int=1) -> void:
	for i in range(frames):
		game._physics_process(DT)
func reset() -> void:
	game.start_level()
	game.sound.muted=true
	game.input_lock=0
func action() -> void:
	var e:=InputEventKey.new()
	e.keycode=KEY_SPACE
	e.pressed=true
	game._unhandled_input(e)
func launch_team(count: int) -> void:
	reset()
	for i in range(6000):
		if game.state!=game.State.LAUNCHING:
			break
		if game.launch_wait<=0 and game.attempts<count and game.swing_time/float(game.config.swing_seconds)>=0.72:
			action()
		tick()
	for i in range(300):
		if game.state!=game.State.TRANSITION:
			break
		tick()
	if count>0:
		check(game.state==game.State.BRIEFING,"successful team reaches mission briefing")
		game.ui.continue_requested.emit()
		tick(32)
func play_attack(aim: bool) -> void:
	for i in range(24000):
		if game.state!=game.State.ATTACKING:
			break
		var bird: Dictionary=game.active_bird()
		if aim and not bird.is_empty() and bird.mode=="flight":
			for bear in game.bears:
				if bear.down:
					continue
				var height: float=bear.p.y-float(game.config.bear_height)-float(game.config.penguin_radius)-bird.p.y
				if height<=0:
					continue
				var vy: float=game.config.dive_speed
				var gravity: float=game.config.gravity
				var seconds: float=(sqrt(vy*vy+2*gravity*height)-vy)/gravity
				var press_x: float=bear.p.x-float(game.config.dive_horizontal_speed)*seconds
				if absf(bird.p.x-press_x)<3.0:
					action()
					break
		tick()
	check(game.state==game.State.RESULTS,"bounded attack simulation reaches results")
func _initialize() -> void:
	call_deferred("run")
func run() -> void:
	game=load("res://scenes/main.tscn").instantiate()
	game.test_mode=true
	root.add_child(game)
	await process_frame
	game.set_physics_process(false)
	game.sound.muted=true
	check(game.state==game.State.MENU,"map start screen loads")
	game.ui.start_requested.emit()
	action()
	check(game.attempts==0,"start click input guard")
	reset()
	game.launch_wait=0
	game.swing_time=0.1
	action()
	check(game.successes==0 and game.attempts==1,"early rope launch fails")
	reset()
	game.launch_wait=0
	game.swing_time=float(game.config.swing_seconds)*0.72
	action()
	check(game.successes==1 and game.score==10,"FIRE-window launch succeeds")
	reset()
	game.launch_wait=0
	game.swing_time=float(game.config.swing_seconds)*0.95
	action()
	check(game.successes==0 and game.attempts==1,"late rope launch fails")
	reset()
	game.launch_wait=0
	game.swing_time=float(game.config.swing_seconds)
	tick(2)
	check(game.attempts==1,"unpressed swing misses automatically")
	reset()
	for i in range(9):
		game.swing_time=i*float(game.config.swing_seconds)/8.0
		var anchor:=Vector2(game.config.rope_anchor[0],game.config.rope_anchor[1])
		check(absf(game.swing_position().distance_to(anchor)-float(game.config.rope_length))<0.01,"rope length preserved at sample %d" % i)
	var paused_at: float=game.swing_time
	var e:=InputEventKey.new()
	e.keycode=KEY_ESCAPE
	e.pressed=true
	game._unhandled_input(e)
	tick(120)
	action()
	check(game.state==game.State.PAUSED and game.swing_time==paused_at,"Esc freezes swing and blocks action")
	game._unhandled_input(e)
	check(game.state==game.State.LAUNCHING,"Esc resumes prior stage")
	launch_team(0)
	check(game.state==game.State.RESULTS and not game.winner,"zero launched penguins produces defeat")
	launch_team(6)
	check(game.successes==6 and game.state==game.State.ATTACKING,"all six successful launches feed attack")
	play_attack(true)
	print("Timed gameplay: ",game.hits," bears hit using ",game.successes," penguins")
	check(game.winner and game.hits>=int(game.config.required_hits),"timed dives can reach six-bear quota")
	check(game.spawned==game.successes and game.resolved==game.successes,"quota does not end mission before remaining flights and dives settle")
	game.ui.next_requested.emit()
	check(game.ui.current_menu=="complete","Continue reaches one-mission completion screen")
	launch_team(2)
	play_attack(false)
	check(not game.winner and game.hits==0,"unused flight opportunities miss and defeat")
	launch_team(1)
	tick(20)
	action()
	check(game.active_bird().mode=="dive","Space starts dive")
	var position: Vector2=game.active_bird().p
	game.toggle_pause()
	tick(100)
	check(game.active_bird().p==position,"pause freezes active dive")
	game.toggle_pause()
	play_attack(false)
	check(game.hits==0,"off-target dive gives no false bear hit")
	reset()
	check(game.swept_target(Vector2(420,150),Vector2(420,690))==0,"swept collision catches high-speed target crossing")
	check(game.swept_target(Vector2(100,150),Vector2(100,690))==-1,"swept collision rejects clear miss")
	var initial_blocks: int=game.blocks.filter(func(b):return not b.broken).size()
	game.break_ice(Vector2(420,300),Vector2(420,500))
	check(game.blocks.filter(func(b):return not b.broken).size()<initial_blocks,"fast dive destroys crossed ice")
	for i in range(180):
		game.update_bears(DT)
	check(game.bears[0].down and game.hits==1,"unsupported bear falls and counts once on landing")
	game.knock_bear(0)
	check(game.hits==1,"repeated contact cannot score defeated bear twice")
	reset()
	game.state=game.State.ATTACKING
	game.successes=1
	game.spawned=1
	game.hits=int(game.config.required_hits)
	game.birds.append({"p":Vector2(100,200),"v":Vector2(100,350),"mode":"dive","id":0,"age":0.0,"bounces":0})
	tick(20)
	check(game.state==game.State.ATTACKING,"reaching quota still waits for active last dive")
	play_attack(false)
	check(game.winner and game.resolved==1,"last dive settles before victory")
	reset()
	game.state=game.State.TRANSITION
	game.successes=1
	game.transition_time=float(game.config.transition_seconds)-DT*.5
	game.input_lock=0
	action()
	tick()
	action()
	check(game.state==game.State.BRIEFING and game.birds.is_empty(),"transition press cannot trigger attack or skip briefing")
	game.ui.continue_requested.emit()
	action()
	check(game.birds.is_empty(),"briefing Continue click cannot also dive")
	for i in range(8):
		game.ui.retry_requested.emit()
		check(game.attempts==0 and game.birds.is_empty() and game.hits==0 and game.blocks.all(func(b):return not b.broken),"retry %d resets team, targets and all ice" % (i+1))
	game.launch_wait=0
	game.swing_time=float(game.config.swing_seconds)*.72
	game.input_lock=0
	var mouse:=InputEventMouseButton.new()
	mouse.pressed=true
	mouse.button_index=MOUSE_BUTTON_LEFT
	game._unhandled_input(mouse)
	check(game.successes==1,"left mouse triggers launch")
	var repeated:=InputEventKey.new()
	repeated.pressed=true
	repeated.echo=true
	repeated.keycode=KEY_SPACE
	game.launch_wait=0
	game._unhandled_input(repeated)
	check(game.attempts==1,"held key repeat is ignored")
	var path:="user://progress.cfg"
	var existed:=FileAccess.file_exists(path)
	var backup:=FileAccess.get_file_as_bytes(path) if existed else PackedByteArray()
	var saved=load("res://scripts/local_save.gd").new()
	saved.best_score=999
	saved.completed=true
	saved.muted=true
	check(saved.save_data()==OK,"local save succeeds")
	var restored=load("res://scripts/local_save.gd").new()
	restored.load_data()
	check(restored.best_score==999 and restored.completed and restored.muted,"local settings and progress reload")
	if existed:
		var file:=FileAccess.open(path,FileAccess.WRITE)
		file.store_buffer(backup)
		file.close()
	else:
		DirAccess.remove_absolute(path)
	game.sound.muted=false
	game.sound.play_cue("launch")
	check(game.sound.players.any(func(p):return p.stream!=null),"audio cue generates stream")
	await create_timer(.35).timeout
	game.queue_free()
	await process_frame
	await create_timer(.1).timeout
	print("GAMEPLAY CHECKS: %d passed, %d failed" % [checks-failures,failures])
	quit(1 if failures else 0)
