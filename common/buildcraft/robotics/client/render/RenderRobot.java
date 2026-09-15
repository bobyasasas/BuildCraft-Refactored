/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.robotics.client.render;

import buildcraft.api.robots.IRobotOverlayItem;
import buildcraft.core.client.BuildCraftLaserManager;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.RenderUtil;
import buildcraft.lib.misc.VecUtil;
import buildcraft.robotics.BCRoboticsItems;
import buildcraft.robotics.entity.EntityRobot;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** All of this is getting a mega-rewrite for Neptune */
// public class RenderRobot extends Render<EntityRobot>
public class RenderRobot extends EntityRenderer<EntityRobot> {
    private static final ResourceLocation overlay_red = new ResourceLocation("buildcraftrobotics:textures/entities" + "/overlay_side.png");
    private static final ResourceLocation overlay_cyan = new ResourceLocation("buildcraftrobotics:textures/entities" + "/overlay_bottom.png");


    private ModelPart skullOverlayBox;
    private ModelPart box;
    private ModelPart helmetBox;
    private final Map<SkullBlock.Type, SkullModelBase> skullModels;
    private static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT_NO_CULL_NO_DEPTH = Util.memoize((p_173233_) -> {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder().setShaderState(RenderType.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER).setTextureState(new RenderStateShard.TextureStateShard(p_173233_, false, false)).setDepthTestState(RenderStateShard.NO_DEPTH_TEST).setTransparencyState(RenderStateShard.NO_TRANSPARENCY).setCullState(RenderStateShard.NO_CULL).setLightmapState(RenderStateShard.LIGHTMAP).setOverlayState(RenderStateShard.OVERLAY).createCompositeState(true);
        return RenderType.create("entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, rendertype$compositestate);
    });

    public RenderRobot(EntityRendererProvider.Context context) {
        super(context);
        this.skullModels = SkullBlockRenderer.createSkullRenderers(context.getModelSet());
        //      @Override

        //    @Override

        MeshDefinition boxMeshDefinition = new MeshDefinition();
        boxMeshDefinition.getRoot().addOrReplaceChild(
                "box",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4F, -4F, -4F, 8, 8, 8, CubeDeformation.NONE),
                PartPose.rotation(0.0F, 0.0F, 0.0F)
        );
        box = boxMeshDefinition.getRoot().bake(32, 32);

        MeshDefinition helmetBoxMeshDefinition = new MeshDefinition();
        helmetBoxMeshDefinition.getRoot().addOrReplaceChild(
                "helmet_box",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4F, -8F, -4F, 8, 8, 8, LayerDefinitions.OUTER_ARMOR_DEFORMATION),
                PartPose.rotation(0.0F, 0.0F, 0.0F)
        );
        helmetBox = helmetBoxMeshDefinition.getRoot().bake(64, 32);

        MeshDefinition skullOverlayBoxMeshDefinition = new MeshDefinition();
        skullOverlayBoxMeshDefinition.getRoot().addOrReplaceChild(
                "skull_overlay_box",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, CubeDeformation.NONE, 0.5F, 0.5F),
                PartPose.rotation(0.0F, 0.0F, 0.0F)
        );
        skullOverlayBox = skullOverlayBoxMeshDefinition.getRoot().bake(64, 32);
    }

    @Override
    public void render(EntityRobot robot, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        packedLight = RenderUtil.getCombinedLight(robot.level(), robot.blockPosition());

        poseStack.pushPose();

        float robotYaw = this.interpolateRotation(robot.yBodyRotO, robot.yBodyRot, partialTicks);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-robotYaw), 0.0f, 1.0f, 0.0f);

        boolean glasses = isWearingGlasses();
        if (glasses) {
        } else {
            IItemHandler itemHandler = robot.getCapability(CapUtil.CAP_ITEMS).orElse(null);
            if (!itemHandler.getStackInSlot(0).isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(-0.125F, 0, -0.125F);
                doRenderItem(itemHandler.getStackInSlot(0), poseStack, bufferSource, packedLight, robot);
                RenderUtil.color(1, 1, 1);
                poseStack.popPose();
            }

            if (!itemHandler.getStackInSlot(1).isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(+0.125F, 0, -0.125F);
                doRenderItem(itemHandler.getStackInSlot(1), poseStack, bufferSource, packedLight, robot);
                RenderUtil.color(1, 1, 1);
                poseStack.popPose();
            }

            if (!itemHandler.getStackInSlot(2).isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(+0.125F, 0, +0.125F);
                doRenderItem(itemHandler.getStackInSlot(2), poseStack, bufferSource, packedLight, robot);
                RenderUtil.color(1, 1, 1);
                poseStack.popPose();
            }

            if (!itemHandler.getStackInSlot(3).isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(-0.125F, 0, +0.125F);
                doRenderItem(itemHandler.getStackInSlot(3), poseStack, bufferSource, packedLight, robot);
                RenderUtil.color(1, 1, 1);
                poseStack.popPose();
            }

            if (!robot.itemInUse.isEmpty()) {
                poseStack.pushPose();

                poseStack.rotateAround(Axis.ZP.rotationDegrees(robot.itemAimPitch), 0, 0, 1);

                if (robot.itemActive) {
                    long newDate = new Date().getTime();
                    robot.itemActiveStage = (robot.itemActiveStage + (newDate - robot.lastUpdateTime) / 10) % 45;
                    poseStack.rotateAround(Axis.ZP.rotationDegrees(robot.itemActiveStage), 0, 0, 1);
                    robot.lastUpdateTime = newDate;
                }

                poseStack.translate(-0.4F, 0, 0);
                poseStack.rotateAround(Axis.YP.rotationDegrees(-45F + 180F), 0, 1, 0);
                poseStack.scale(0.8F, 0.8F, 0.8F);

                ItemStack itemstack1 = robot.itemInUse;

                Minecraft.getInstance().getItemRenderer().renderStatic(robot, itemstack1, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, poseStack, bufferSource, robot.level(), packedLight, OverlayTexture.NO_OVERLAY, robot.getId());

                RenderUtil.color(1, 1, 1);
                poseStack.popPose();
            }
        }
        if (robot.isLaserVisible) {
            LaserData_BC8 laser = new LaserData_BC8(BuildCraftLaserManager.POWER_MED, VecUtil.getVec(robot), new Vec3(robot.laserEndX, robot.laserEndY, robot.laserEndZ), 0.5 / 16);

            LaserRenderer_BC8.renderLaserDynamic(laser, poseStack.last(), bufferSource.getBuffer(RenderType.cutout()));
        }

        if (robot.getTexture() != null) {
            float storagePercent = (float) robot.getBattery().getStored() / (float) robot.getBattery().getCapacity();
            if (robot.hurtTime > 0) {
                RenderUtil.color(1.0f, 0.6f, 0.6f);
                poseStack.rotateAround(Axis.ZP.rotationDegrees(robot.hurtTime * 0.01f), 0, 0, 1);
            }
            doRenderRobot(1, robot.getTexture(), storagePercent, robot.isActive(), poseStack, bufferSource, packedLight);
        }

        if (glasses) {
        } else {
            for (ItemStack s : robot.getWearables()) {
                doRenderWearable(robot, s, poseStack, bufferSource, packedLight);
            }
        }

        poseStack.popPose();
    }

    private boolean isWearingGlasses() {
        Player player = Minecraft.getInstance().player;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || helmet.getItem() != BCRoboticsItems.robotGoggles.get()) {
            return false;
        }
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRobot entity) {
        return entity.getTexture();
    }

    // @Override

    private void doRenderItem(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, EntityRobot robot) {
        float renderScale = 0.5f;
        poseStack.pushPose();
        poseStack.translate(0, 0.28F, 0);
        poseStack.scale(renderScale, renderScale, renderScale);
        Minecraft.getInstance().getItemRenderer().renderStatic(robot, stack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, robot.level(), packedLight, OverlayTexture.NO_OVERLAY, robot.getId());

        poseStack.popPose();
    }

    private void doRenderWearable(EntityRobot entity, ItemStack wearable, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (wearable.getItem() instanceof IRobotOverlayItem) {
            ((IRobotOverlayItem) wearable.getItem()).renderRobotOverlay(wearable);
        } else if (wearable.getItem() instanceof ArmorItem) {
            poseStack.pushPose();
            poseStack.scale(1.0125F, 1.0125F, 1.0125F);
            poseStack.translate(0.0f, -0.25f, 0.0f);
            poseStack.rotateAround(Axis.ZP.rotationDegrees(180F), 0, 0, 1);

            int color;
            if (wearable.getItem() instanceof DyeableLeatherItem) {
                color = ((DyeableLeatherItem) wearable.getItem()).getColor(wearable);
            } else {
                color = 16777215;
            }

            ResourceLocation armorTexture = new ResourceLocation(ForgeHooksClient.getArmorTexture(entity, wearable, "", EquipmentSlot.HEAD, ""));
            Model armorModel = ForgeHooksClient.getArmorModel(entity, wearable, EquipmentSlot.HEAD, null);
            poseStack.pushPose();
            poseStack.rotateAround(Axis.YP.rotationDegrees(-90.0f), 0, 1, 0);
            boolean foil = wearable.hasFoil();
            if (armorModel != null) {


                if (color != 16777215) {
                    float f = (float) (color >> 16 & 255) / 255.0F;
                    float f1 = (float) (color >> 8 & 255) / 255.0F;
                    float f2 = (float) (color & 255) / 255.0F;
                    this.renderModel(poseStack, bufferSource, packedLight, foil, armorModel, f, f1, f2, this.getArmorResource(entity, wearable, EquipmentSlot.HEAD, null));
                    this.renderModel(poseStack, bufferSource, packedLight, foil, armorModel, 1.0F, 1.0F, 1.0F, this.getArmorResource(entity, wearable, EquipmentSlot.HEAD, "overlay"));
                } else {
                    this.renderModel(poseStack, bufferSource, packedLight, foil, armorModel, 1.0F, 1.0F, 1.0F, this.getArmorResource(entity, wearable, EquipmentSlot.HEAD, null));
                }
            } else {


                if (color != 16777215) {
                    float f = (float) (color >> 16 & 255) / 255.0F;
                    float f1 = (float) (color >> 8 & 255) / 255.0F;
                    float f2 = (float) (color & 255) / 255.0F;
                    this.renderModel(poseStack, bufferSource, packedLight, foil, helmetBox, f, f1, f2, this.getArmorResource(entity, wearable, EquipmentSlot.HEAD, null));
                    this.renderModel(poseStack, bufferSource, packedLight, foil, helmetBox, 1.0F, 1.0F, 1.0F, this.getArmorResource(entity, wearable, EquipmentSlot.HEAD, "overlay"));
                } else {
                    this.renderModel(poseStack, bufferSource, packedLight, foil, helmetBox, 1.0F, 1.0F, 1.0F, this.getArmorResource(entity, wearable, EquipmentSlot.HEAD, null));
                }
            }
            poseStack.popPose();

            poseStack.popPose();
        }
        else if (wearable.getItem() instanceof BlockItem && ((BlockItem) wearable.getItem()).getBlock() instanceof AbstractSkullBlock) {
            doRenderSkull(wearable, poseStack, bufferSource, packedLight);
        }
    }

    private void doRenderSkull(ItemStack wearable, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(1.0125F, 1.0125F, 1.0125F);
        GameProfile gameProfile = null;
        if (wearable.hasTag()) {
            CompoundTag nbt = wearable.getTag();
            if (nbt.contains("Name")) {// FIXME: Come back to this!
            } else if (nbt.contains("SkullOwner", Tag.TAG_COMPOUND)) {
                gameProfile = NbtUtils.readGameProfile(nbt.getCompound("SkullOwner"));
            }
        }

        poseStack.translate(-0.5D, -0.25D, -0.5D);
        SkullBlock.Type skullblock$type = ((AbstractSkullBlock) ((BlockItem) wearable.getItem()).getBlock()).getType();
        SkullModelBase skullmodelbase = this.skullModels.get(skullblock$type);
        RenderType rendertype = SkullBlockRenderer.getRenderType(skullblock$type, gameProfile);
        SkullBlockRenderer.renderSkull(null, 180.0F, 0, poseStack, bufferSource, packedLight, skullmodelbase, rendertype);
        poseStack.popPose();
    }

    private void doRenderRobot(float factor, ResourceLocation texture, float storagePercent, boolean isAsleep, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        boolean glasses = isWearingGlasses();
        poseStack.pushPose();
        if (glasses) {
            box.render(poseStack, bufferSource.getBuffer(ENTITY_CUTOUT_NO_CULL_NO_DEPTH.apply(texture)), packedLight, OverlayTexture.NO_OVERLAY, 1 - storagePercent, storagePercent, 0, 1.0F);
        } else {
            box.render(poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        poseStack.popPose();

        if (!isAsleep && !glasses) {

            poseStack.pushPose();

            box.render(poseStack, bufferSource.getBuffer(RenderType.entityTranslucent(overlay_red)), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, storagePercent);


            box.render(poseStack, bufferSource.getBuffer(RenderType.entityTranslucent(overlay_cyan)), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            poseStack.popPose();

        }

    }

    private float interpolateRotation(float prevRot, float rot, float partialTicks) {
        float angle;

        for (angle = rot - prevRot; angle < -180.0F; angle += 360.0F) {
        }

        while (angle >= 180.0F) {
            angle -= 360.0F;
        }

        return prevRot + partialTicks * angle;
    }

    private void renderModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, boolean foil, Model model, float r, float g, float b, ResourceLocation armorResource) {
        VertexConsumer vertexconsumer = ItemRenderer.getArmorFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(armorResource), false, foil);
        model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
    }

    private void renderModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, boolean foil, ModelPart model, float r, float g, float b, ResourceLocation armorResource) {
        VertexConsumer vertexconsumer = ItemRenderer.getArmorFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(armorResource), false, foil);
        model.render(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
    }

    private static final Map<String, ResourceLocation> ARMOR_LOCATION_CACHE = Maps.newHashMap();

    public ResourceLocation getArmorResource(Entity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
        ArmorItem item = (ArmorItem) stack.getItem();
        String texture = item.getMaterial().getName();
        String domain = "minecraft";
        int idx = texture.indexOf(':');
        if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
        }
        String s1 = String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d%s.png", domain, texture, 1, type == null ? "" : String.format(Locale.ROOT, "_%s", type));

        s1 = ForgeHooksClient.getArmorTexture(entity, stack, s1, slot, type);
        ResourceLocation resourcelocation = ARMOR_LOCATION_CACHE.get(s1);

        if (resourcelocation == null) {
            resourcelocation = new ResourceLocation(s1);
            ARMOR_LOCATION_CACHE.put(s1, resourcelocation);
        }

        return resourcelocation;
    }
}
