# M2.7b screenshot camera (runs every tick through the minecraft:tick function tag).
# Parks every player in spectator mode at a fixed viewpoint so the headless quickPlay client
# photographs the rig from a deterministic angle instead of whatever view the saved player had.
# Spawn-relative like the rig: the rig stands at (spawn.x, surface+1, spawn.z+30 .. spawn.z+34);
# the camera hovers 3.5 blocks west of the engine, 1.5 above the rig plane and 1.5 south of it,
# looking back east/south along the chain (yaw -75 ~ east-slightly-south; pitch +20 downward —
# Minecraft tp pitch is positive downward, a negative value would aim the camera up at the sky,
# which is exactly what the first evidence session's mis-signed shot showed). Close enough that
# the stone engine's piston collar reads clearly against the pipe arm at full extension. The
# y+3 base offset is relative to the load-tag sender at the world spawn (one below the
# surface), so ~2.5 lands the eye 1.5 blocks above the rig plane.
# This is evidence tooling, not gameplay: it only ever runs in dev-run worlds that load this
# mod's built-in datapack.
gamemode spectator @a
execute positioned ~ ~3 ~ run tp @a ~-4.2 ~3 ~31.5 -75 25
