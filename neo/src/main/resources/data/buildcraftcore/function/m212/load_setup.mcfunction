# The M2.12 filler/quarry evidence rig loader: runs on every world load through the minecraft:load
# function tag, and re-places the two rigs 50/56 blocks south of spawn (the load-tag sender stands
# at the world spawn point, so everything below is spawn-relative; the M2.7b rig occupies the
# 30..34 strip, the M2.11 gate rig the 40..42 strip, so these start at 50). Time-of-day is pinned
# by the M2.7b load_setup, which runs first in the load tag.
execute positioned ~ ~1 ~50 run function buildcraftcore:m212/place_filler_rig
execute positioned ~ ~1 ~56 run function buildcraftcore:m212/place_quarry_rig
