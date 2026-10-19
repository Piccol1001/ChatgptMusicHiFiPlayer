extends SceneTree

var game
var failures := 0
var checks := 0
const DT := 1.0 / 120.0

func check(condition: bool, description: String) -> void:
	checks += 1
	if condition:
		print("PASS: ",description)
	else:
		failures += 1
		push_error("FAIL: "+description)

func tick(frames: int = 1) -> void:
	for i in range(frames):
		game._physics_process(DT)

func reset() -> void:
	game.start_level()
	game.sound.muted = true
	game.input_lock = 0.0

func action() -> void:
	var event := InputEventKey.new()
	event.keycode = KEY_SPACE
	event.pressed = true
	game._unhandled_input(event)

func launch_team(count: int) -> void:
	var safety := 0
	while game.state == game.State.LAUNCHING and safety < 10000:
		if game.launch_wait <= 0 and game.attempts < count and game.launch_x >= float(game.config.launch_x):
			action()
		tick()
		safety += 1
	while game.state == game.State.TRANSITION:
		tick()
	tick(30)

func run_to_result(aim: bool) -> void:
	var safety := 0
	while game.state == game.State.ATTACKING and safety < 15000:
		if aim:
			for bird in game.birds:
				if bird.mode == "flight" and bird.p.x >= 0:
					for bear in game.bears:
						if not bear.down and absf(bird.p.x-(bear.x-62.0)) < 2.1:
							action()
							break
					break
		tick()
		safety += 1
	check(game.state == game.State.RESULTS,"bounded simulation reaches result")

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	game = load("res://scenes/main.tscn").instantiate()
	game.test_mode = true
	root.add_child(game)
	await process_frame
	game.set_physics_process(false)
	game.sound.muted = true
	check(game.state == game.State.MENU,"start screen loads")
	game.ui.start_requested.emit()
	check(game.state == game.State.LAUNCHING and game.attempts == 0,"start menu enters launch without accidental action")
	action()
	check(game.attempts == 0,"menu entry input guard")
	reset()
	tick(60)
	action()
	check(game.attempts == 1 and game.successes == 0,"early launch fails")
	reset()
	game.launch_wait = 0
	game.launch_x = game.config.launch_x
	action()
	check(game.successes == 1 and game.score == 10,"on-time launch succeeds")
	reset()
	game.launch_wait = 0
	game.launch_x = game.config.launch_x + game.config.launch_half_window + 1
	action()
	check(game.successes == 0 and game.attempts == 1,"late manual launch fails")
	reset()
	game.launch_wait = 0
	game.launch_x = game.config.launch_miss_x
	tick(2)
	check(game.attempts == 1 and game.successes == 0,"unpressed penguin automatically misses")
	var x: float = game.launch_x
	var elapsed: float = game.time
	game.toggle_pause()
	tick(120)
	action()
	check(game.state == game.State.PAUSED and game.launch_x == x and game.time == elapsed,"pause freezes physics and blocks actions")
	game.toggle_pause()
	check(game.state == game.State.LAUNCHING,"pause resumes prior state")
	reset()
	launch_team(0)
	run_to_result(false)
	check(not game.winner and game.successes == 0,"zero successful launches gives defeat")
	reset()
	launch_team(8)
	check(game.successes == 8 and game.state == game.State.ATTACKING,"eight successful launches feed attack stage")
	run_to_result(true)
	check(game.winner and game.remaining_targets() == 0 and game.resolved == 8,"real timed dives can win; all eight opportunities settle")
	check(game.score == 380,"configured score counts launches and three targets")
	game.ui.next_requested.emit()
	check(game.ui.menu_title.text == "LEVEL COMPLETE","next navigation reaches honest single-level completion screen")
	reset()
	launch_team(2)
	run_to_result(false)
	check(not game.winner and game.resolved == 2,"undived flight misses and defeats")
	reset()
	launch_team(1)
	tick(45)
	action()
	var diving := false
	for b in game.birds:
		diving = diving or b.mode == "dive"
	check(diving,"Space initiates dive")
	var pos: Vector2 = game.birds[0].p
	game.toggle_pause()
	tick(60)
	check(game.birds[0].p == pos,"attack pause freezes active dive")
	game.toggle_pause()
	run_to_result(false)
	check(game.remaining_targets() == 3,"off-target dive misses bears")
	reset()
	check(game.swept_target(Vector2(320,100),Vector2(320,1000)) == 0,"swept collision catches high-speed tunneling")
	check(game.swept_target(Vector2(450,100),Vector2(450,1000)) == -1,"swept collision rejects clear misses")
	# Construct a last active dive: even with all targets down, result must wait.
	game.state = game.State.ATTACKING
	game.successes = 1
	game.spawned = 1
	game.resolved = 0
	for bear in game.bears:
		bear.down = true
	game.birds.append({"p":Vector2(100,180),"v":Vector2(0,400),"mode":"dive","id":0})
	tick()
	check(game.state == game.State.ATTACKING,"victory waits for active final dive")
	run_to_result(false)
	check(game.winner and game.resolved == 1,"final dive settles before victory")
	reset()
	game.state = game.State.TRANSITION
	game.transition_time = game.config.transition_seconds - DT*0.5
	game.successes = 1
	game.input_lock = 0
	action()
	check(game.birds.is_empty(),"transition input cannot launch or dive")
	tick()
	action()
	check(game.state == game.State.ATTACKING and game.birds.is_empty(),"stage boundary input guard")
	for i in range(12):
		game.ui.retry_requested.emit()
		check(game.attempts == 0 and game.birds.is_empty() and game.bears.size() == 3 and game.score == 0,"retry %d resets level state" % (i+1))
	var mouse := InputEventMouseButton.new()
	mouse.button_index = MOUSE_BUTTON_LEFT
	mouse.pressed = true
	game.launch_wait = 0
	game.input_lock = 0
	game.launch_x = game.config.launch_x
	game._unhandled_input(mouse)
	check(game.successes == 1,"left mouse triggers gameplay action")
	var repeated := InputEventKey.new()
	repeated.pressed = true
	repeated.echo = true
	repeated.keycode = KEY_SPACE
	game.launch_wait = 0
	game._unhandled_input(repeated)
	check(game.attempts == 1,"held key repeat does not launch again")
	game.sound.muted = false
	game.sound.play_cue("launch")
	check(game.sound.players.any(func(p): return p.stream != null),"original synthesized audio stream generated")
	var path := "user://progress.cfg"
	var had_file := FileAccess.file_exists(path)
	var previous := FileAccess.get_file_as_bytes(path) if had_file else PackedByteArray()
	var saved = load("res://scripts/local_save.gd").new()
	saved.best_score = 777
	saved.completed = true
	saved.muted = true
	check(saved.save_data() == OK,"local progress writes successfully")
	var restored = load("res://scripts/local_save.gd").new()
	restored.load_data()
	check(restored.best_score == 777 and restored.completed and restored.muted,"settings and progress survive reload")
	if had_file:
		var file := FileAccess.open(path,FileAccess.WRITE)
		file.store_buffer(previous)
		file.close()
	else:
		DirAccess.remove_absolute(path)
	await create_timer(0.35).timeout
	game.queue_free()
	await process_frame
	await create_timer(0.1).timeout
	print("GAMEPLAY CHECKS: %d passed, %d failed" % [checks-failures,failures])
	quit(1 if failures else 0)
