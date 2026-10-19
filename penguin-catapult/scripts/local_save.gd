extends RefCounted

const PATH := "user://progress.cfg"
var best_score := 0
var completed := false
var muted := false

func load_data() -> void:
	var config := ConfigFile.new()
	if config.load(PATH) == OK:
		best_score = int(config.get_value("progress", "best_score", 0))
		completed = bool(config.get_value("progress", "completed", false))
		muted = bool(config.get_value("settings", "muted", false))

func save_data() -> Error:
	var config := ConfigFile.new()
	config.set_value("progress", "best_score", best_score)
	config.set_value("progress", "completed", completed)
	config.set_value("settings", "muted", muted)
	return config.save(PATH)
