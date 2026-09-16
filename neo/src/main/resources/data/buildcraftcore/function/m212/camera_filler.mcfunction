# M2.12 screenshot camera (filler rig): parks every player in spectator mode at the filler rig's
# viewpoint so the headless quickPlay client photographs the rig from a deterministic angle. Spawn-
# relative like the rig: the rig stands at (spawn.x, surface+1, spawn.z+50); the filler block sits
# two east of the rig origin, the work area frame spans three..ten east. The camera hovers well
# south-east of the rig, above its plane, looking back north-west down the wall's face so the
# filler machine, the work-area frame and the amber active-cell cube are all in frame (the first
# session's close-in south shot had the fresh dirt wall filling the whole viewport).
#
# Evidence tooling, not gameplay: it only runs in dev-run worlds that load this mod's built-in
# datapack. Deliberately NOT wired into the minecraft:tick function tag (that tag stays owned by
# the M2.7b camera): for an m212 evidence session, add "buildcraftcore:m212/camera_filler" to
# data/minecraft/tags/function/tick.json (after the m27b entry), run the client, shoot, revert.
gamemode spectator @a
execute positioned ~ ~1 ~50 run tp @a ~13.5 ~3.6 ~7.5 133 22
