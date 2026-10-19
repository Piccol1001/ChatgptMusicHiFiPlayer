extends Node
# Original synthesized sounds, generated in memory. No external audio dependency.
var muted := false
var players: Array[AudioStreamPlayer] = []

func _ready() -> void:
	for i in range(8):
		var player := AudioStreamPlayer.new()
		add_child(player)
		players.append(player)

func play_cue(cue: String) -> void:
	if muted:
		return
	var specs := {"launch": [260.0, 900.0, 0.23], "dive": [900.0, 180.0, 0.25], "hit": [150.0, 45.0, 0.24], "miss": [220.0, 90.0, 0.3], "win": [500.0, 1000.0, 0.5]}
	var spec: Array = specs.get(cue, specs["miss"])
	var rate := 22050
	var length := int(float(spec[2]) * rate)
	var bytes := PackedByteArray()
	bytes.resize(length * 2)
	var phase := 0.0
	for i in range(length):
		var t := float(i) / length
		phase += TAU * lerpf(spec[0], spec[1], t) / rate
		var sample := sin(phase) * (1.0 - t) * minf(t * 30.0, 1.0) * 0.23
		bytes.encode_s16(i * 2, int(sample * 32767))
	var stream := AudioStreamWAV.new()
	stream.format = AudioStreamWAV.FORMAT_16_BITS
	stream.mix_rate = rate
	stream.data = bytes
	for player in players:
		if not player.playing:
			player.stream = stream
			player.play()
			break

func set_paused(value: bool) -> void:
	for player in players:
		player.stream_paused = value

func _exit_tree() -> void:
	for player in players:
		player.stop()
		player.stream = null
