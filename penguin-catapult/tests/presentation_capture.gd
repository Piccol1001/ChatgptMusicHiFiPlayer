extends SceneTree
var game
var failures:=0
var checks:=0
func _initialize() -> void:
	call_deferred("run")
func check(value: bool,description: String) -> void:
	checks+=1
	if value:
		print("PASS: "+description)
	else:
		failures+=1
		push_error(description)
func capture(name_value: String) -> void:
	game.world.queue_redraw()
	await process_frame
	await process_frame
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_png("res://builds/"+name_value+".png")
func click(at: Vector2) -> void:
	var e:=InputEventMouseButton.new()
	e.button_index=MOUSE_BUTTON_LEFT
	e.pressed=true
	e.position=at
	Input.parse_input_event(e)
	await process_frame
	e=InputEventMouseButton.new()
	e.button_index=MOUSE_BUTTON_LEFT
	e.pressed=false
	e.position=at
	Input.parse_input_event(e)
	await process_frame
func run() -> void:
	game=load("res://scenes/main.tscn").instantiate()
	game.test_mode=true
	root.add_child(game)
	await process_frame
	game.set_physics_process(false)
	game.sound.muted=true
	await capture("01-map")
	await click(game.ui.actions.get_child(1).get_global_rect().get_center())
	check(game.ui.current_menu=="help","map How to play button works")
	await capture("02-help")
	await click(game.ui.actions.get_child(0).get_global_rect().get_center())
	check(game.ui.current_menu=="start","help Back returns to map")
	await click(game.ui.actions.get_child(0).get_global_rect().get_center())
	check(game.state==game.State.LAUNCHING and game.attempts==0,"Start does not accidentally launch")
	game.launch_wait=0
	game.swing_time=float(game.config.swing_seconds)*.72
	game.time=1.1
	game.update_hud()
	await capture("03-catapult")
	game.input_lock=0
	await click(Vector2(640,450))
	check(game.successes==1 and game.attempts==1,"playfield click launches swinging penguin")
	await click(Vector2(493,30))
	check(game.state==game.State.PAUSED,"HUD pause icon receives click")
	await capture("04-pause")
	await click(game.ui.actions.get_child(0).get_global_rect().get_center())
	check(game.state==game.State.LAUNCHING and game.attempts==1,"Resume click does not trigger launch")
	game.state=game.State.BRIEFING
	game.successes=6
	game.ui.menu("briefing","Hit 06 bears to complete the mission!\n\n6 penguins made it through.\nDive early: you keep moving forward.")
	game.update_hud()
	await capture("05-briefing")
	await click(game.ui.actions.get_child(0).get_global_rect().get_center())
	check(game.state==game.State.ATTACKING and game.birds.is_empty(),"Continue starts attack without triggering dive")
	game.spawned=1
	game.birds.append({"p":Vector2(220,170),"v":Vector2(220,350),"mode":"dive","id":0,"age":.1,"bounces":0})
	game.update_hud()
	await capture("06-attack")
	game.knock_bear(0)
	game.break_ice(Vector2(400,390),Vector2(450,460))
	game.update_hud()
	await capture("07-impact")
	game.camera_x=1050
	game.birds[0].p=Vector2(1430,240)
	await capture("08-camera")
	game.hits=7
	game.spawned=6
	game.resolved=6
	game.birds[0].mode="done"
	game.score=760
	game.finish_level()
	await capture("09-result")
	await click(game.ui.actions.get_child(0).get_global_rect().get_center())
	check(game.ui.current_menu=="complete","result Continue leads to completion")
	await capture("10-complete")
	for child in game.ui.actions.get_children():
		check(child.get_global_rect().end.y<=704,"completion button stays within viewport")
	root.size=Vector2i(1152,648)
	await create_timer(.2).timeout
	await capture("11-laptop-letterbox")
	var transform:=root.get_final_transform()
	var scales:=transform.get_scale()
	var visible_size:=root.get_visible_rect().size
	check(DisplayServer.window_get_size()==Vector2i(1152,648) and transform.origin.x>100 and absf(scales.x-scales.y)<.002 and absf(visible_size.x/visible_size.y-960.0/704.0)<.001,"wide laptop window preserves aspect ratio and centered side margins")
	game.sound.set_paused(false)
	game.queue_free()
	await process_frame
	await create_timer(.2).timeout
	print("PRESENTATION CHECKS: %d passed, %d failed" % [checks-failures,failures])
	quit(1 if failures else 0)
