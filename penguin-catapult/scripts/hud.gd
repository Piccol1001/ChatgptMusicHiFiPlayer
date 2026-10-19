extends CanvasLayer
const Art = preload("res://scripts/art.gd")
const DISPLAY_FONT = preload("res://assets/LilitaOne-Regular.ttf")
const BODY_FONT = preload("res://assets/BalsamiqSans-Regular.ttf")
signal start_requested
signal retry_requested
signal resume_requested
signal home_requested
signal sound_requested
signal next_requested
signal pause_requested
signal continue_requested
signal help_requested
var game
var top: Control
var shade: Control
var card: Panel
var menu_title: Label
var menu_copy: Label
var actions: VBoxContainer
var feedback: Label
var team_count: Label
var target_count: Label
var total: Label
var bear_group: Control
var sound_button: Button
var sticker: TextureRect
var title_logo: Label
var subtitle: Label
var celebration: Node2D
var current_menu := ""

func label(value: String,size_value: int,color: Color=Color.WHITE) -> Label:
	var item := Label.new()
	item.text = value
	item.add_theme_font_override("font",DISPLAY_FONT)
	item.add_theme_font_size_override("font_size",size_value)
	item.add_theme_color_override("font_color",color)
	item.add_theme_color_override("font_outline_color",Color("4f7890"))
	item.add_theme_constant_override("outline_size",4)
	item.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	item.mouse_filter = Control.MOUSE_FILTER_IGNORE
	return item

func style(color: Color,border: Color,radius: int=8) -> StyleBoxFlat:
	var s := StyleBoxFlat.new()
	s.bg_color=color
	s.border_color=border
	s.set_border_width_all(2)
	s.set_corner_radius_all(radius)
	s.shadow_color=Color(0.04,.23,.34,.35)
	s.shadow_size=3
	s.shadow_offset=Vector2(1,3)
	return s

func icon_button(text_value: String,at: Vector2,callback: Callable) -> Button:
	var b := Button.new()
	b.text=text_value
	b.add_theme_font_override("font",DISPLAY_FONT)
	b.position=at
	b.size=Vector2(36,36)
	b.add_theme_font_size_override("font_size",24)
	b.add_theme_color_override("font_color",Color("e5ffff"))
	b.add_theme_stylebox_override("normal",style(Color("298da7"),Color("c4ffff"),2))
	b.add_theme_stylebox_override("hover",style(Color("53b4c9"),Color.WHITE,2))
	b.add_theme_stylebox_override("pressed",style(Color("20728e"),Color.WHITE,2))
	b.focus_mode=Control.FOCUS_NONE
	b.pressed.connect(callback)
	top.add_child(b)
	return b

func badge(parent: Control,at: Vector2,texture: Texture2D) -> void:
	var back := Panel.new()
	back.position=at
	back.size=Vector2(64,64)
	back.mouse_filter=Control.MOUSE_FILTER_IGNORE
	back.add_theme_stylebox_override("panel",style(Color("fffef1"),Color("53697d"),32))
	parent.add_child(back)
	var pic := TextureRect.new()
	var atlas:=AtlasTexture.new()
	atlas.atlas=texture
	atlas.region=Rect2(9,5,62,62) if texture==Art.PENGUIN else Rect2(1,0,72,63)
	atlas.filter_clip=true
	pic.texture=atlas
	pic.expand_mode=TextureRect.EXPAND_IGNORE_SIZE
	pic.position=at+Vector2(5,5)
	pic.size=Vector2(54,54)
	pic.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	pic.mouse_filter=Control.MOUSE_FILTER_IGNORE
	parent.add_child(pic)

func _ready() -> void:
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter=Control.MOUSE_FILTER_IGNORE
	add_child(root)
	top=Control.new()
	top.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	top.mouse_filter=Control.MOUSE_FILTER_IGNORE
	root.add_child(top)
	badge(top,Vector2(38,10),Art.PENGUIN)
	team_count=label("x 06",35,Color("a9edff"))
	team_count.position=Vector2(105,20)
	team_count.size=Vector2(105,44)
	top.add_child(team_count)
	sound_button=icon_button("♪",Vector2(396,14),func(): sound_requested.emit())
	sound_button.tooltip_text="Toggle sound"
	var help := icon_button("?",Vector2(436,14),func(): help_requested.emit())
	help.tooltip_text="Controls"
	var pause := icon_button("II",Vector2(476,14),func(): pause_requested.emit())
	pause.tooltip_text="Pause / Esc"
	bear_group=Control.new()
	bear_group.mouse_filter=Control.MOUSE_FILTER_IGNORE
	top.add_child(bear_group)
	badge(bear_group,Vector2(595,10),Art.BEAR)
	total=label("TOTAL  0",17,Color.WHITE)
	total.position=Vector2(665,10)
	total.size=Vector2(230,25)
	bear_group.add_child(total)
	target_count=label("00 / 06",33,Color("a9edff"))
	target_count.position=Vector2(665,35)
	target_count.size=Vector2(230,42)
	bear_group.add_child(target_count)
	feedback=label("",23,Color("fff9cf"))
	feedback.position=Vector2(60,661)
	feedback.size=Vector2(840,33)
	root.add_child(feedback)
	shade=Control.new()
	shade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	shade.mouse_filter=Control.MOUSE_FILTER_STOP
	root.add_child(shade)
	card=Panel.new()
	card.position=Vector2(210,103)
	card.size=Vector2(540,530)
	card.add_theme_stylebox_override("panel",style(Color("b6e7f8"),Color("418caf"),22))
	shade.add_child(card)
	var inner:=Panel.new()
	inner.position=Vector2(12,12)
	inner.size=Vector2(516,506)
	inner.mouse_filter=Control.MOUSE_FILTER_IGNORE
	inner.add_theme_stylebox_override("panel",style(Color("e5f4fb"),Color.WHITE,14))
	card.add_child(inner)
	menu_title=label("Mission 1",28)
	menu_title.position=Vector2(20,25)
	menu_title.size=Vector2(500,45)
	card.add_child(menu_title)
	sticker=TextureRect.new()
	sticker.texture=Art.PENGUIN
	sticker.position=Vector2(221,83)
	sticker.size=Vector2(98,126)
	sticker.expand_mode=TextureRect.EXPAND_IGNORE_SIZE
	sticker.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	sticker.mouse_filter=Control.MOUSE_FILTER_IGNORE
	card.add_child(sticker)
	celebration=load("res://scripts/menu_art.gd").new()
	celebration.position=Vector2(270,174)
	card.add_child(celebration)
	menu_copy=label("",22)
	menu_copy.add_theme_font_override("font",BODY_FONT)
	menu_copy.add_theme_constant_override("outline_size",3)
	menu_copy.position=Vector2(28,225)
	menu_copy.size=Vector2(484,145)
	menu_copy.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
	card.add_child(menu_copy)
	actions=VBoxContainer.new()
	actions.position=Vector2(36,379)
	actions.size=Vector2(468,126)
	actions.add_theme_constant_override("separation",7)
	card.add_child(actions)
	title_logo=label("PENGUIN",66)
	title_logo.position=Vector2(80,35)
	title_logo.size=Vector2(800,92)
	title_logo.add_theme_constant_override("outline_size",10)
	title_logo.add_theme_color_override("font_outline_color",Color("253748"))
	shade.add_child(title_logo)
	subtitle=label("CATAPULT",78,Color("ffca42"))
	subtitle.position=Vector2(80,110)
	subtitle.size=Vector2(800,100)
	subtitle.add_theme_constant_override("outline_size",11)
	subtitle.add_theme_color_override("font_outline_color",Color("614422"))
	shade.add_child(subtitle)
	refresh_sound()

func refresh_sound() -> void:
	sound_button.text="×" if game.save_data.muted else "♪"

func set_status(team: int,hits: int,quota: int,best: int,attack: bool) -> void:
	top.visible=game.state!=game.State.MENU
	bear_group.visible=attack
	team_count.text="x %02d" % maxi(0,team)
	target_count.text="%02d / %02d" % [hits,quota]
	total.text="TOTAL  %02d" % hits

func button(value: String,callback: Callable) -> void:
	var b:=Button.new()
	b.text=value
	b.add_theme_font_override("font",DISPLAY_FONT)
	b.custom_minimum_size.y=36
	b.add_theme_font_size_override("font_size",22)
	b.add_theme_color_override("font_color",Color.WHITE)
	b.add_theme_color_override("font_outline_color",Color("4e8099"))
	b.add_theme_constant_override("outline_size",4)
	b.add_theme_stylebox_override("normal",style(Color("b7e7f4"),Color("6ba4bd"),16))
	b.add_theme_stylebox_override("hover",style(Color("d7f5ff"),Color("367f9f"),16))
	b.add_theme_stylebox_override("pressed",style(Color("83c6de"),Color("367f9f"),16))
	b.focus_mode=Control.FOCUS_NONE
	b.pressed.connect(callback)
	actions.add_child(b)

func menu(kind: String,copy: String,won: bool=false) -> void:
	current_menu=kind
	shade.show()
	for c in actions.get_children():
		actions.remove_child(c)
		c.queue_free()
	menu_copy.text=copy
	sticker.show()
	celebration.visible=kind in ["result","complete"]
	if celebration.visible:
		sticker.hide()
	sticker.texture=Art.PENGUIN
	sticker.position=Vector2(221,83)
	sticker.size=Vector2(98,126)
	card.position=Vector2(210,103)
	card.size=Vector2(540,530)
	title_logo.visible=kind=="start"
	subtitle.visible=kind=="start"
	if kind=="start":
		card.position=Vector2(520,452)
		card.size=Vector2(400,235)
		card.get_child(0).size=Vector2(376,211)
		menu_title.position=Vector2(0,10)
		menu_title.size=Vector2(400,34)
		menu_title.text="ICEBREAKER BAY"
		sticker.hide()
		menu_copy.position=Vector2(16,46)
		menu_copy.size=Vector2(368,32)
		menu_copy.text="%d penguins • Hit %d bears" % [int(game.config.penguins),int(game.config.required_hits)]
		menu_copy.add_theme_font_size_override("font_size",20)
		actions.position=Vector2(24,86)
		actions.size=Vector2(352,125)
		button("Start Mission 1",func(): start_requested.emit())
		button("How to play",func(): help_requested.emit())
		button("Sound: %s" % ("Off" if game.save_data.muted else "On"),func(): sound_requested.emit(); menu("start",copy))
		return
	menu_copy.add_theme_font_size_override("font_size",22)
	card.get_child(0).size=Vector2(516,506)
	menu_title.position=Vector2(20,25)
	menu_title.size=Vector2(500,45)
	menu_copy.position=Vector2(28,225)
	menu_copy.size=Vector2(484,145)
	actions.position=Vector2(36,379)
	actions.size=Vector2(468,126)
	menu_title.text="Mission 1"
	match kind:
		"briefing":
			button("Continue",func(): continue_requested.emit())
		"pause":
			menu_title.text="Paused"
			button("Resume",func(): resume_requested.emit())
			button("Retry",func(): retry_requested.emit())
			button("Map",func(): home_requested.emit())
		"result":
			menu_title.text="Congratulations!" if won else "Try again!"
			if won:
				button("Continue",func(): next_requested.emit())
			button("Retry",func(): retry_requested.emit())
			button("Map",func(): home_requested.emit())
		"complete":
			menu_title.text="Mission complete!"
			button("Replay Mission 1",func(): retry_requested.emit())
			button("Map",func(): home_requested.emit())
		"help":
			menu_title.text="How to play"
			sticker.position=Vector2(221,72)
			sticker.size=Vector2(98,92)
			menu_copy.position=Vector2(28,175)
			menu_copy.size=Vector2(484,194)
			button("Back to map",func(): home_requested.emit())

func hide_menu() -> void:
	shade.hide()
