# M2.11 screenshot camera: parks every player in spectator mode at the m211 rig's viewpoint so
# the headless quickPlay client photographs the gate rig from a deterministic angle. Spawn-
# relative like the rig: the rig stands at (spawn.x, surface+1, spawn.z+40 .. +42); the camera
# hovers 3 east / 2.2 south of the pipe, 0.4 above the rig plane, looking back north-west/down
# at the gate plate (gate on the pipe's EAST face): the lit plate center frame, the redstone
# block on top, and the dust trail into the lit lamp to its right.
#
# Evidence tooling, not gameplay: it only runs in dev-run worlds that load this mod's built-in
# datapack. Deliberately NOT wired into the minecraft:tick function tag (that tag stays owned by
# the M2.7b camera): for an m211 evidence session, add "buildcraftcore:m211/camera_lock" to
# data/minecraft/tags/function/tick.json (after the m27b entry), run the client, shoot, revert.
gamemode spectator @a
execute positioned ~ ~1 ~40 run tp @a ~2.4 ~1 ~3.2 137 12
