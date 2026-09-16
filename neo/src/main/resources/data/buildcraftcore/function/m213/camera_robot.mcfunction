# M2.13 screenshot camera (robot rig): parks every player in spectator mode at the robot rig's
# viewpoint so the headless quickPlay client photographs the rig from a deterministic angle.
# Spawn-relative like the rig: the rig stands at (spawn.x, surface+1, spawn.z+62); the robot
# floats at the rig origin and the ore wall spans three..eight east, two cells deep. The camera
# hovers north of the wall at its mid-height, looking back south down the wall's face so the
# robot (amber while working) and the remaining ore wall are both in frame, with the crack
# overlay on whichever cell is being broken.
#
# Evidence tooling, not gameplay: it only runs in dev-run worlds that load this mod's built-in
# datapack. Deliberately NOT wired into the minecraft:tick function tag (that tag stays owned by
# the M2.7b camera): for an m213 evidence session, add "buildcraftcore:m213/camera_robot" to
# data/minecraft/tags/function/tick.json (after the m27b entry), run the client, shoot, revert.
gamemode spectator @a
execute positioned ~ ~1 ~62 run tp @a ~5.5 ~1.4 ~-3.5 10 16
