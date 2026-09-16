# The M2.13 robot evidence rig loader: runs on every world load through the minecraft:load
# function tag, and re-places the rig 62 blocks south of spawn (the load-tag sender stands at the
# world spawn point, so everything below is spawn-relative; the M2.7b rig occupies the 30..34
# strip, the M2.11 gate rig the 40..42 strip, the M2.12 filler/quarry rigs the 50/56 strips, so
# this starts at 62). Time-of-day is pinned by the M2.7b load_setup, which runs first in the load
# tag.
execute positioned ~ ~1 ~62 run function buildcraftcore:m213/place_robot_rig
