# M2.12 screenshot camera (quarry rig): parks every player in spectator mode at the quarry rig's
# viewpoint so the headless quickPlay client photographs the rig from a deterministic angle. Spawn-
# relative like the rig: the rig stands at (spawn.x, surface+1, spawn.z+56); the quarry block sits
# two east of the rig origin, the mining area frame spans three..ten east. The camera hovers
# north-east of the rig, above its plane, looking back south-west down the strip: the scan mines
# the NORTH row first (x then z), so the camera must stand on the north side for the mined gaps
# and the amber drill-target cage to be on the near side of the strip (the first session's
# south-side shot had both hidden behind the untouched south row).
#
# Evidence tooling, not gameplay: it only runs in dev-run worlds that load this mod's built-in
# datapack. Deliberately NOT wired into the minecraft:tick function tag (that tag stays owned by
# the M2.7b camera): for an m212 evidence session, add "buildcraftcore:m212/camera_quarry" to
# data/minecraft/tags/function/tick.json (after the m27b entry), run the client, shoot, revert.
gamemode spectator @a
execute positioned ~ ~1 ~56 run tp @a ~9 ~2.4 ~-4 30 30
