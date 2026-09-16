# M2.7b visual evidence rig (runs on every world load through the minecraft:load function tag).
# The load-tag sender stands at the world spawn point, so everything below is spawn-relative.
# The rig stands 30 blocks south of spawn at a fixed vertical offset of +1 (surface air of the
# M2.7b scratch save "gametestworld", whose spawn sits one block below the ground surface at
# (-5074346, -59, 10493953)). Two deliberate choices here:
#   - fixed offsets instead of a heightmap projection: the rig's engine block itself is solid,
#     so a heightmap-based origin crept one block up on every world load and stacked a second
#     rig layer (observed in the first evidence session; see place_chain's sweep);
#   - 30 blocks south instead of at the spawn column: the scratch save doubles as the vanilla
#     game test world, and the test_instance_blocks the runGameTestServer framework leaves at
#     ~7-block intervals along +X two blocks south of spawn render permanent status beacons
#     and structure bounds there - the rig must be clear of that strip for a clean screenshot.
#
# Rig: stone engine -> three wooden kinesis pipes -> energy meter, one row along +Z (south).
# The M2.7b render chain this evidences:
#   1. the datapack merge below queues fuel in the StoneEngineBlockEntity,
#   2. its server tick ignites it and syncs burn state to the client (update tag packet),
#   3. StoneEngineBlockRenderer extends the piston and KinesisPipeBlockRenderer draws the
#      connected pipe geometry (engine output FACING and meter both count as connections).
# Pin the world to a bright morning on every load so the screenshot lighting is deterministic
# (the scratch save's clock had drifted to night by the second evidence session, rendering the
# rig nearly invisible; even a few minutes of daylight advance still leaves the rig in full
# sun). Note: the legacy doDaylightCycle gamerule no longer exists on 26.1.2 — its day-night
# cycle moved to the world clock system — and one bad line fails the whole function's parse,
# so nothing here may reference removed commands.
time set 1000
execute positioned ~ ~1 ~30 run function buildcraftcore:m27b/place_chain
