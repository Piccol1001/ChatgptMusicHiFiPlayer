extends CanvasLayer

signal start_requested
signal retry_requested
signal resume_requested
signal home_requested
signal sound_requested
signal next_requested
signal pause_requested

var hud: Label
var detail: Label
var feedback: Label
var shade: ColorRect
var panel: VBoxContainer
var menu_title: Label
var menu_copy: Label
var actions: VBoxContainer
var sound_button: Button

func label(text_value: String, size: int, color: Color = Color("193e55")) -> Label:
	var item := Label.new()
	item.text = text_value
	item.add_theme_font_size_override("font_size", size)
	item.add_theme_color_override("font_color", color)
	item.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	return item

func _ready() -> void:
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(root)
	var bar := ColorRect.new()
	bar.color = Color("193e55")
	bar.position = Vector2(24,20)
	bar.size = Vector2(1232,76)
	bar.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(bar)
	hud = label("",24,Color.WHITE)
	hud.position = Vector2(44,28)
	hud.size = Vector2(1000,30)
	hud.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
	root.add_child(hud)
	detail = label("",16,Color("a9dfed"))
	detail.position = Vector2(44,62)
	detail.size = Vector2(1080,26)
	detail.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
	root.add_child(detail)
	var pause_button := Button.new()
	pause_button.text = "II   PAUSE"
	pause_button.position = Vector2(1110,35)
	pause_button.size = Vector2(126,45)
	pause_button.focus_mode = Control.FOCUS_NONE
	pause_button.pressed.connect(func(): pause_requested.emit())
	root.add_child(pause_button)
	feedback = label("",28)
	feedback.position = Vector2(80,627)
	feedback.size = Vector2(1120,50)
	root.add_child(feedback)
	shade = ColorRect.new()
	shade.color = Color(0.035,0.12,0.19,0.76)
	shade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.add_child(shade)
	var card := PanelContainer.new()
	card.position = Vector2(330,128)
	card.size = Vector2(620,480)
	var style := StyleBoxFlat.new()
	style.bg_color = Color("f3fbff")
	style.corner_radius_top_left = 24
	style.corner_radius_top_right = 24
	style.corner_radius_bottom_left = 24
	style.corner_radius_bottom_right = 24
	style.content_margin_left = 38
	style.content_margin_right = 38
	style.content_margin_top = 28
	style.content_margin_bottom = 28
	card.add_theme_stylebox_override("panel",style)
	shade.add_child(card)
	panel = VBoxContainer.new()
	panel.add_theme_constant_override("separation",14)
	card.add_child(panel)
	panel.add_child(label("P E N G U I N   C A T A P U L T",17,Color("31899b")))
	menu_title = label("SNOWBOUND",44)
	panel.add_child(menu_title)
	menu_copy = label("",19)
	menu_copy.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	menu_copy.custom_minimum_size.y = 100
	panel.add_child(menu_copy)
	actions = VBoxContainer.new()
	actions.add_theme_constant_override("separation",8)
	panel.add_child(actions)

func button(text_value: String, callback: Callable) -> void:
	var b := Button.new()
	b.text = text_value
	b.custom_minimum_size.y = 43
	b.add_theme_font_size_override("font_size",19)
	for variant in ["normal", "hover", "pressed"]:
		var style := StyleBoxFlat.new()
		style.bg_color = Color("287c91") if variant == "normal" else Color("369db2")
		style.corner_radius_top_left = 8
		style.corner_radius_top_right = 8
		style.corner_radius_bottom_left = 8
		style.corner_radius_bottom_right = 8
		b.add_theme_stylebox_override(variant,style)
	b.add_theme_color_override("font_color",Color.WHITE)
	b.focus_mode = Control.FOCUS_NONE
	b.pressed.connect(callback)
	actions.add_child(b)

func menu(kind: String, copy: String, won: bool = false) -> void:
	shade.show()
	for child in actions.get_children():
		actions.remove_child(child)
		child.queue_free()
	menu_copy.text = copy
	match kind:
		"start":
			menu_title.text = "SNOWBOUND"
			button("START LEVEL 01",func(): start_requested.emit())
			button("SOUND ON / OFF",func(): sound_requested.emit())
		"pause":
			menu_title.text = "TAKE A BREATHER"
			button("RESUME",func(): resume_requested.emit())
			button("RETRY LEVEL",func(): retry_requested.emit())
			button("MAIN MENU",func(): home_requested.emit())
		"result":
			menu_title.text = "BEARS BEATEN!" if won else "SNOW CLOSE!"
			if won:
				button("NEXT → LEVEL COMPLETE",func(): next_requested.emit())
			button("RETRY LEVEL",func(): retry_requested.emit())
			button("MAIN MENU",func(): home_requested.emit())
		"complete":
			menu_title.text = "LEVEL COMPLETE"
			button("PLAY LEVEL 01 AGAIN",func(): retry_requested.emit())
			button("MAIN MENU",func(): home_requested.emit())

func hide_menu() -> void:
	shade.hide()
