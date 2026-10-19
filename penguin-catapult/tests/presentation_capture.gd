extends SceneTree
var game
var failures := 0

func _initialize() -> void:
	call_deferred("run")

func capture(name_value: String) -> void:
	await process_frame
	await process_frame
	await RenderingServer.frame_post_draw
	var picture := root.get_texture().get_image()
	picture.save_png("res://builds/"+name_value+".png")

func click(at: Vector2) -> void:
	var event := InputEventMouseButton.new()
	event.button_index = MOUSE_BUTTON_LEFT
	event.pressed = true
	event.position = at
	Input.parse_input_event(event)
	await process_frame
	event = InputEventMouseButton.new()
	event.button_index = MOUSE_BUTTON_LEFT
	event.pressed = false
	event.position = at
	Input.parse_input_event(event)
	await process_frame

func check(value: bool, description: String) -> void:
	if value:
		print("PASS: "+description)
	else:
		failures += 1
		push_error(description)

func run() -> void:
	game = load("res://scenes/main.tscn").instantiate()
	game.test_mode = true
	root.add_child(game)
	await process_frame
	game.set_physics_process(false)
	game.sound.muted = true
	await capture("01-start")
	await click(game.ui.actions.get_child(0).get_global_rect().get_center())
	check(game.state == game.State.LAUNCHING and game.attempts == 0,"real start button click does not trigger launch")
	game.launch_wait = 0
	game.launch_x = 306
	game.time = 0.8
	game.update_hud()
	game.queue_redraw()
	await capture("02-launch")
	game.input_lock = 0
	await click(Vector2(600,400))
	check(game.successes == 1 and game.attempts == 1,"real playfield click launches penguin")
	game.toggle_pause()
	await capture("03-pause")
	var success_count: int = game.successes
	await click(game.ui.actions.get_child(0).get_global_rect().get_center())
	check(game.state == game.State.LAUNCHING and game.successes == success_count,"real resume click does not trigger action")
	game.state = game.State.ATTACKING
	game.camera.position.x = 640+game.config.attack_offset
	game.successes = 5
	game.spawned = 5
	for i in range(5):
		game.birds.append({"p":Vector2(190+i*175,175),"v":Vector2(220,0),"mode":"flight","id":i})
	game.birds[0].mode = "dive"
	game.birds[0].p = Vector2(270,310)
	game.update_hud()
	game.queue_redraw()
	await capture("04-attack")
	for bear in game.bears:
		bear.down = true
	game.resolved = game.successes
	for bird in game.birds:
		bird.mode = "done"
	game.finish_level()
	game.queue_redraw()
	await capture("05-result")
	await click(game.ui.actions.get_child(0).get_global_rect().get_center())
	check(game.ui.menu_title.text == "LEVEL COMPLETE","real next button opens completion screen")
	await capture("06-complete")
	game.queue_free()
	await process_frame
	await process_frame
	print("PRESENTATION / MOUSE CHECKS: %d failed" % failures)
	quit(1 if failures else 0)
