# The M2.11 gate evidence rig, executed 40 blocks south of and one block above the world
# spawn's ground surface (see m211/load_setup; the M2.7b rig occupies the 30..34 strip, so this
# rig sits beyond it). Layout along +Z (south), mirroring the gate_logic gametest with one
# cosmetic deviation: the lamp sits two east of the pipe, fed by a redstone dust trail on the
# gate face, so the east camera angle sees the gate plate itself instead of a lamp wall.
#   engine_stone[facing=south] at the origin -> kinesis pipe one south (the gate host),
#   dust one east (powered by the gate face), redstone lamp two east, redstone block on top.
#
# Idempotency sweep: every world load re-places the same rig, so the previous rig's block
# entities must go first; clears only this rig's footprint box, never below the rig plane.
fill ~ ~ ~ ~2 ~4 ~4 air
# The engine faces south so its output (FACING) points into the gate pipe.
setblock ~ ~ ~ buildcraftcore:engine_stone[facing=south]
setblock ~ ~ ~1 buildcraftcore:pipe_kinesis_wood
setblock ~1 ~ ~1 minecraft:redstone_wire
setblock ~2 ~ ~1 minecraft:redstone_lamp
setblock ~ ~1 ~1 minecraft:redstone_block
# Ignite the engine without a player (the M2.7b /data merge path: loadWithComponents +
# setChanged + sendBlockUpdated; five items burn ~6.7 minutes so the engine is burning and the
# gate's engine trigger stays satisfied whenever the screenshot is taken).
data merge block ~ ~ ~ {bc_pending_fuel: 5, bc_pending_burn_ticks: 1600, bc_energy_stored: 0L}
# Attach the gate (buildcraftsilicon:plug_gate_iron_and_no_modifier's variant: AND x IRON x
# no_modifier = 2 slots) on the pipe's EAST face and configure both trigger/action pairs, via
# the same legacy-shaped gate_data compound the M2.6 gate_config codec persists (BcGateNbt):
# slot 0: buildcraft:engine.stage.blue -> buildcraft:redstone.output (legacy latches the output,
#         the lamp on the gate face lights through the pipe block's signal overrides),
# slot 1: buildcraft:redstone.input.active (the redstone block above) ->
#         buildcraft:pipe.wire.output.red (the pipe hub tints red in the BER).
# side:6b is EnumGatePart CENTER, the legacy default for statements without a side.
data merge block ~ ~ ~1 {bc_gate_side:5, bc_gate:{variant:{logic:0b, material:1b, modifier:0b}, connections:0s, 'trigger[0]':{s:{kind:"buildcraft:engine.stage.blue", side:6b}}, 'action[0]':{s:{kind:"buildcraft:redstone.output", side:6b}}, 'trigger[1]':{s:{kind:"buildcraft:redstone.input.active", side:6b}}, 'action[1]':{s:{kind:"buildcraft:pipe.wire.output.red", side:6b}}, triggerOn:0s, actionOn:0s}}
