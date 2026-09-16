# The M2.12 quarry evidence rig, executed 56 blocks south of and one block above the world spawn's
# ground surface (see m212/load_setup; 6 blocks south of the filler rig, so the two camera
# positions never see each other's rig). Layout along +X (east), mirroring the quarry_cycle
# gametest:
#   engine_stone[facing=east] at the origin -> kinesis pipe one east -> quarry two east,
#   the mining area spanning three east .. ten east on the rig plane (x3..x10, z0..z1;
#   8x2 = 16 stone, a deliberately oversized area so the mining cycle - 16 x 8000 uMJ at the
#   slice engine's 100 uMJ/t - keeps running for minutes and the screenshot session always
#   catches a mid-work state with the BER frame and the amber drill-target marker visible).
#
# Idempotency sweep: every world load re-places the same rig, so the previous rig's block
# entities must go first; clears only this rig's footprint box, never below the rig plane.
fill ~ ~ ~ ~10 ~4 ~4 air
# The engine faces east so its output (FACING) points into the pipe chain.
setblock ~ ~ ~ buildcraftcore:engine_stone[facing=east]
setblock ~1 ~ ~ buildcraftcore:pipe_kinesis_wood
setblock ~2 ~ ~ buildcraftbuilders:quarry
# The pre-mined stone wall (legacy: whatever the markers enclosed; slice: a fixed rig plane).
setblock ~3 ~ ~ minecraft:stone
setblock ~4 ~ ~ minecraft:stone
setblock ~5 ~ ~ minecraft:stone
setblock ~6 ~ ~ minecraft:stone
setblock ~7 ~ ~ minecraft:stone
setblock ~8 ~ ~ minecraft:stone
setblock ~9 ~ ~ minecraft:stone
setblock ~10 ~ ~ minecraft:stone
setblock ~3 ~ ~1 minecraft:stone
setblock ~4 ~ ~1 minecraft:stone
setblock ~5 ~ ~1 minecraft:stone
setblock ~6 ~ ~1 minecraft:stone
setblock ~7 ~ ~1 minecraft:stone
setblock ~8 ~ ~1 minecraft:stone
setblock ~9 ~ ~1 minecraft:stone
setblock ~10 ~ ~1 minecraft:stone
# Ignite the engine without a player (the M2.7b /data merge path).
data merge block ~ ~ ~ {bc_pending_fuel: 5, bc_pending_burn_ticks: 1600, bc_energy_stored: 0L}
# Configure the quarry the same way the quarry_cycle gametest does, via the persisted BE fields
# (area corners in absolute world coordinates: the rig origin is spawn + (0, +1, +56), and the
# frozen scratch save spawns at (-5074346, -59, 10493953), so the origin is (-5074346, -58, 10494009)
# and the area spans x -5074343..-5074336, y -58, z 10494009..10494010). The output buffer is
# emptied too (bc_output: []): the buffer persists in the save, and once it holds its 8 stacks
# the quarry refuses power and stops picking targets (hasWork) - exactly what a previous
# evidence session's leftovers would otherwise do to this one.
data merge block ~2 ~ ~ {bc_energy_stored: 0L, bc_total_received: 0L, bc_finished: 0b, bc_task_power: 0L, bc_output: [], bc_min_x: -5074343, bc_min_y: -58, bc_min_z: 10494009, bc_max_x: -5074336, bc_max_y: -58, bc_max_z: 10494010}
