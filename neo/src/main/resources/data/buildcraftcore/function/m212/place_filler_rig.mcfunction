# The M2.12 filler evidence rig, executed 50 blocks south of and one block above the world spawn's
# ground surface (see m212/load_setup; the M2.7b rig occupies the 30..34 strip, the m211 gate rig
# the 40..42 strip, so this sits beyond them). Layout along +X (east), mirroring the filler_cycle
# gametest:
#   engine_stone[facing=east] at the origin -> kinesis pipe one east -> filler two east,
#   the buildcraft:fill work area spanning three east .. ten east (x3..x10, y0..y2, z0..z2;
#   8x3x3 = 72 cells against 64 inserted dirt: the cycle fills 64 cells (~2.5 min at 4800 uMJ
#   per placement on the slice engine's 100 uMJ/t) and then parks FOREVER in the legacy
#   "missing resources" stall with the amber active-cell marker sitting on the south-top row -
#   a deterministic mid-work tableau, so the screenshot session has no timing race to win).
#
# Idempotency sweep: every world load re-places the same rig, so the previous rig's block
# entities must go first; clears only this rig's footprint box, never below the rig plane.
fill ~ ~ ~ ~10 ~4 ~4 air
# The engine faces east so its output (FACING) points into the pipe chain.
setblock ~ ~ ~ buildcraftcore:engine_stone[facing=east]
setblock ~1 ~ ~ buildcraftcore:pipe_kinesis_wood
setblock ~2 ~ ~ buildcraftbuilders:filler
# Ignite the engine without a player (the M2.7b /data merge path: loadWithComponents +
# setChanged + sendBlockUpdated; five items burn ~6.7 minutes so the engine keeps feeding the
# chain whenever the screenshot is taken).
data merge block ~ ~ ~ {bc_pending_fuel: 5, bc_pending_burn_ticks: 1600, bc_energy_stored: 0L}
# Configure the filler the same way the filler_cycle gametest does, via the persisted BE fields
# (box corners in absolute world coordinates: the rig origin is spawn + (0, +1, +50), and the
# frozen scratch save spawns at (-5074346, -59, 10493953), so the origin is (-5074346, -58, 10494003)
# and the box spans x -5074343..-5074336, y -58..-56, z 10494003..10494005):
# pattern = legacy PatternFill unique tag, 64 dirt in the resource buffer (8 cells short of the
# 72-cell box, on purpose - see the stall note above), battery empty.
data merge block ~2 ~ ~ {bc_energy_stored: 0L, bc_total_received: 0L, bc_finished: 0b, bc_pattern: "buildcraft:fill", bc_min_x: -5074343, bc_min_y: -58, bc_min_z: 10494003, bc_max_x: -5074336, bc_max_y: -56, bc_max_z: 10494005, bc_resource: {id: "minecraft:dirt", count: 64}}
