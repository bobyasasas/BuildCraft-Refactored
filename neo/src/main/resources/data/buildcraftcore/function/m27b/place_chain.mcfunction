# The M2.7b evidence rig itself; executed 30 blocks south of and one block above the world
# spawn's ground surface (see load_setup). One row along +Z (south).
#
# Idempotency sweep first: every world load re-resolves the same spawn-relative origin and
# re-places the same rig, so the previous rig's block entities (and their burn/energy state)
# must go before the setblocks. Clears only this rig's footprint column and never below the
# rig plane, so the ground stays untouched and the layout is pixel-stable across sessions.
fill ~ ~ ~ ~ ~4 ~4 air
# Engine faces south so its output (FACING) points into the first pipe; the meter is the sink.
setblock ~ ~ ~ buildcraftcore:engine_stone[facing=south]
setblock ~ ~ ~1 buildcraftcore:pipe_kinesis_wood
setblock ~ ~ ~2 buildcraftcore:pipe_kinesis_wood
setblock ~ ~ ~3 buildcraftcore:pipe_kinesis_wood
setblock ~ ~ ~4 buildcraftcore:energy_meter
# Ignite the engine without a player: queue five coal-length fuel items directly in the block
# entity (the vanilla /data merge path: loadWithComponents + setChanged + sendBlockUpdated).
# The BE tick ignites the first item on its next tick; five items burn ~6.7 minutes, so the
# piston is still extended (burn progress 0.25..0.75) whenever the screenshot is taken.
data merge block ~ ~ ~ {bc_pending_fuel: 5, bc_pending_burn_ticks: 1600, bc_energy_stored: 0L}
