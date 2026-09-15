/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.robotics.ai;

import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.EntityRobotBase;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class AIRobotShutdown extends AIRobot {
    private int skip;
    private double motionX;
    private double motionZ;

    public AIRobotShutdown(EntityRobotBase iRobot) {
        super(iRobot);
        skip = 0;
        motionX = robot.getDeltaMovement().x();
        motionZ = robot.getDeltaMovement().z();
    }

    @Override
    public void start() {
        robot.undock();
        robot.setDeltaMovement(motionX, -0.075f, motionZ);
    }

    private boolean isBlocked(float yOffset) {
        return !robot.level().noCollision(robot, robot.getBoundingBox().move(robot.getDeltaMovement().x(), yOffset, robot.getDeltaMovement().z()));
    }

    @Override
    public void update() {
        if (skip == 0) {
            if (!isBlocked(-0.075f)) {
                robot.setDeltaMovement(robot.getDeltaMovement().x(), -0.075f, robot.getDeltaMovement().z());
            } else {
                while (isBlocked(0)) {
                    robot.move(MoverType.SELF, new Vec3(0, 0.075f, 0));
                }
                robot.setDeltaMovement(robot.getDeltaMovement().x(), 0f, robot.getDeltaMovement().z());
                if (robot.getDeltaMovement().x() != 0 || robot.getDeltaMovement().z() != 0) {
                    robot.setDeltaMovement(0f, robot.getDeltaMovement().y(), 0f);
                    skip = 0;
                } else {
                    skip = 20;
                }
            }
        } else {
            skip--;
        }

    }

    @Override
    public long getPowerCost() {
        return 0;
    }
}
