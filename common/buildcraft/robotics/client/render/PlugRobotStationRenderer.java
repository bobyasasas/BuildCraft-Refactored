package buildcraft.robotics.client.render;

import buildcraft.api.transport.pluggable.IPlugDynamicRenderer;
import buildcraft.lib.client.model.AdvModelCache;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.robotics.BCRoboticsModels;
import buildcraft.robotics.plug.PluggableRobotStation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum PlugRobotStationRenderer implements IPlugDynamicRenderer<PluggableRobotStation> {
    INSTANCE;



    private static final AdvModelCache cache = new AdvModelCache(BCRoboticsModels.ROBOT_STATION_DYNAMIC, PluggableRobotStation.MODEL_VAR_INFO);

    public static void onModelBake() {
        cache.reset();
    }

    @Override
    public void render(PluggableRobotStation robotStation, float partialTicks, PoseStack poseStack, VertexConsumer bb, int combinedLight, int combinedOverlay) {
        robotStation.setClientModelVariables();
        if (robotStation.clientModelData.hasNoNodes()) {
            robotStation.clientModelData.setNodes(BCRoboticsModels.ROBOT_STATION_DYNAMIC.createTickableNodes());
        }
        robotStation.clientModelData.refresh();

        MutableQuad copy = new MutableQuad();
        for (MutableQuad q : cache.getCutoutQuads()) {
            copy.copyFrom(q);
            copy.multShade();

            copy.lighti(combinedLight);
            copy.overlay(combinedOverlay);

            q.render(poseStack.last(), bb);
        }
    }
}
