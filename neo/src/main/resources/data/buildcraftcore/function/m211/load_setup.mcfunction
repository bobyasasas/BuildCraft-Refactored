# The M2.11 gate evidence rig loader: runs on every world load through the minecraft:load
# function tag, and re-places the rig 40 blocks south of spawn (the load-tag sender stands at
# the world spawn point, so everything below is spawn-relative; the M2.7b rig occupies the
# 30..34 strip, this one starts at 40). Time-of-day is pinned by the M2.7b load_setup, which
# runs first in the load tag.
execute positioned ~ ~1 ~40 run function buildcraftcore:m211/place_gate_rig
