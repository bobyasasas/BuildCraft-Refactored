# The M2.13 robot evidence rig, executed 62 blocks south of and one block above the world spawn's
# ground surface (see m213/load_setup). Layout, mirroring the robot_task gametest:
#   a robot_miner summoned at the rig origin (it floats: noPhysics + noGravity, the legacy flight
#   stance) with its work zone carrying a 6x3x2 wall of iron ore three..eight east (x3..x8,
#   y0..y2, z0..z1; 36 cells). The robot searches the zone, flies to each ore cell (0.1
#   blocks/tick straight leg), breaks it with the crack overlay (legacy AIRobotBreak damage
#   formula at diamond-pickaxe speed) and carries the drops - roughly 40 s of continuous work
#   (1.1 s per ore cell), so the screenshot session has a comfortable window on the amber working
#   tint. After the last ore it latches DONE and parks (grey tint).
#
# Idempotency: every world load re-places the same rig, so the previous session's robots are
# killed first (/kill removes plain entities directly - hurtServer's invulnerable stance only
# blocks damage, not RemovalReason.KILLED) and the footprint box is cleared. Robots never stray
# beyond the ~24 block radius, so the neighbouring M2.12 rigs are untouched.
kill @e[type=buildcraftrobotics:robot_miner,distance=..24]
fill ~ ~ ~ ~10 ~5 ~4 air
# The ore wall (the rig's work zone, 6x3x2: x3..x8, y0..y2, z0..z1).
setblock ~3 ~ ~ minecraft:iron_ore
setblock ~4 ~ ~ minecraft:iron_ore
setblock ~5 ~ ~ minecraft:iron_ore
setblock ~6 ~ ~ minecraft:iron_ore
setblock ~7 ~ ~ minecraft:iron_ore
setblock ~8 ~ ~ minecraft:iron_ore
setblock ~3 ~1 ~ minecraft:iron_ore
setblock ~4 ~1 ~ minecraft:iron_ore
setblock ~5 ~1 ~ minecraft:iron_ore
setblock ~6 ~1 ~ minecraft:iron_ore
setblock ~7 ~1 ~ minecraft:iron_ore
setblock ~8 ~1 ~ minecraft:iron_ore
setblock ~3 ~2 ~ minecraft:iron_ore
setblock ~4 ~2 ~ minecraft:iron_ore
setblock ~5 ~2 ~ minecraft:iron_ore
setblock ~6 ~2 ~ minecraft:iron_ore
setblock ~7 ~2 ~ minecraft:iron_ore
setblock ~8 ~2 ~ minecraft:iron_ore
setblock ~3 ~ ~1 minecraft:iron_ore
setblock ~4 ~ ~1 minecraft:iron_ore
setblock ~5 ~ ~1 minecraft:iron_ore
setblock ~6 ~ ~1 minecraft:iron_ore
setblock ~7 ~ ~1 minecraft:iron_ore
setblock ~8 ~ ~1 minecraft:iron_ore
setblock ~3 ~1 ~1 minecraft:iron_ore
setblock ~4 ~1 ~1 minecraft:iron_ore
setblock ~5 ~1 ~1 minecraft:iron_ore
setblock ~6 ~1 ~1 minecraft:iron_ore
setblock ~7 ~1 ~1 minecraft:iron_ore
setblock ~8 ~1 ~1 minecraft:iron_ore
setblock ~3 ~2 ~1 minecraft:iron_ore
setblock ~4 ~2 ~1 minecraft:iron_ore
setblock ~5 ~2 ~1 minecraft:iron_ore
setblock ~6 ~2 ~1 minecraft:iron_ore
setblock ~7 ~2 ~1 minecraft:iron_ore
setblock ~8 ~2 ~1 minecraft:iron_ore
# The robot, configured in one summon through its persisted bc_* entity NBT (the same keys the
# gametest sets programmatically): full battery (500000 uMJ = legacy 5,000 MJ x10^-4) and the
# work zone in absolute world coordinates. The rig origin is spawn + (0, +1, +62) and the frozen
# scratch save spawns at (-5074346, -59, 10493953), so the origin is (-5074346, -58, 10494015),
# the wall spans x -5074343..-5074338, y -58..-56, z 10494015..10494016. No bc_task_state: the
# task state machine wakes an idle robot with a zone into SEARCH on its first tick.
summon buildcraftrobotics:robot_miner ~ ~ ~ {bc_energy_stored: 500000L, bc_min_x: -5074343, bc_min_y: -58, bc_min_z: 10494015, bc_max_x: -5074338, bc_max_y: -56, bc_max_z: 10494016}
