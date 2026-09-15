/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render;

import buildcraft.api.core.EnumPipePart;
import buildcraft.lib.BCLibConfig;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.misc.ItemUtil;
import buildcraft.lib.misc.RenderUtil;
import buildcraft.lib.misc.SpriteUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class ItemRenderUtil {


    private static final Random modelOffsetRandom = new Random(0);


    //    private static final RenderEntityItem customItemRenderer =
//    private static final ItemEntityRenderer customItemRenderer =
//                @Override
//                @Override

    static {
//        glListCache = CacheBuilder.newBuilder()//
//                .expireAfterAccess(40, TimeUnit.SECONDS)//
//                .removalListener(ItemRenderUtil::onStackRemove)//
    }



//        // This is broken - some stacks render too big but some render way too small.
//        // Also not all stacks are centered :/
//            // Items are rotated by 45 degrees

    // Batch item rendering

    private static boolean inBatch = false;

    /** Used to render a lot of items in sequential order. Assumes that you don't change the glstate inbetween calls. */
    public static void renderItemStack(ItemStack stack, int lightc, Direction dir, PoseStack poseStack, VertexConsumer bb) {
        renderItemStack(stack, stack.getCount(), lightc, dir, poseStack, bb);
    }

    /** Used to render a lot of items in sequential order. Assumes that you don't change the glstate inbetween calls. */
    public static void renderItemStack(ItemStack stack, int stackCount, int lightc, Direction dir, PoseStack poseStack, VertexConsumer bb) {
        if (stack.isEmpty()) {
            return;
        }
        try {
            renderItemStackInternal(stack, stackCount, lightc, dir, poseStack, bb);
        } catch (Throwable exception) {
            CrashReport report = CrashReport.forThrowable(exception, "Rendering Item Stack");
            CrashReportCategory category = report.addCategory("Item being rendered");
            category.setDetail("Stack Count", stackCount);
            category.setDetail("Item Class", () -> "" + stack.getItem().getClass());
            category.setDetail("Item ID", () -> "" + ForgeRegistries.ITEMS.getKey(stack.getItem()));
            category.setDetail("Item NBT", () -> "" + stack.getTag());
            throw new ReportedException(report);
        }
    }

    private static void renderItemStackInternal(ItemStack stack, int stackCount, int lightc, Direction dir, PoseStack poseStack, VertexConsumer bb) {
        if (dir == null) {
            dir = Direction.EAST;
        }
        dir = BCLibConfig.rotateTravelingItems.changeFacing(dir);

        Minecraft mc = Minecraft.getInstance();

        BakedModel model = mc.getItemRenderer().getModel(stack, null, null, /*seed*/ 0);
        model = model.getOverrides().resolve(model, stack, null, null, /*seed*/ 0);
        boolean requireGl = stack.isEnchanted() || model.isCustomRenderer();

        if (bb != null && !requireGl) {

            final int itemModelCount = getStackModelCount(stackCount);

            if (itemModelCount > 1) {
                setupModelOffsetRandom(stack);
            }

            poseStack.pushPose();
            for (int i = 0; i < itemModelCount; i++) {
                if (i == 0) {
                } else {
                    float dx = (modelOffsetRandom.nextFloat() * 2.0F - 1.0F) * 0.08F;
                    float dy = (modelOffsetRandom.nextFloat() * 2.0F - 1.0F) * 0.08F;
                    float dz = (modelOffsetRandom.nextFloat() * 2.0F - 1.0F) * 0.08F;
                    poseStack.translate(+dx, +dy, +dz);

                }

                float scale = 0.30f;

                MutableQuad q = new MutableQuad(-1, null);
                for (EnumPipePart part : EnumPipePart.VALUES) {
                    for (BakedQuad quad : model.getQuads(null, part.face, RandomSource.create(0))) {
                        q.fromBakedItem(quad);
                        q.translated(-0.5, -0.5, -0.5);
                        q.scaled(scale);
                        q.rotate(Direction.SOUTH, dir, 0, 0, 0);
                        if (quad.isTinted()) {
                            int colour = mc.getItemColors().getColor(stack, quad.getTintIndex());
                            q.multColouri(colour, colour >> 8, colour >> 16, 0xFF);
                        }
                        q.lighti(lightc);
                        q.normalf(1, 1, 1);
                        q.multShade();
                        q.render(poseStack.last(), bb);
                    }
                }
            }

            poseStack.popPose();
            return;
        }
        else {
            mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, lightc, OverlayTexture.NO_OVERLAY, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), mc.level, 0);
        }

        if (!inBatch) {
            inBatch = true;
            SpriteUtil.bindTexture(TextureAtlas.LOCATION_BLOCKS);
            poseStack.pushPose();
            poseStack.scale(0.3F, 0.3F, 0.3F);
            RenderUtil.disableStandardItemLighting();
        }
        mc.getItemRenderer().renderModelLists(model, stack, lightc, OverlayTexture.NO_OVERLAY, poseStack, bb);
    }

    private static void setupModelOffsetRandom(ItemStack stack) {
        final long seed;
        if (stack.isEmpty()) {
            seed = 137;
        } else {
            ResourceLocation regName = ItemUtil.getRegistryName(stack.getItem());
            if (regName == null) {
                seed = 127;
            } else {
                int regNameSeed = regName.getNamespace().hashCode() ^ regName.getPath().hashCode();
                seed = (regNameSeed & 0x7F_FF_FF_FF);
            }
        }
        modelOffsetRandom.setSeed(seed);
    }

    private static int getStackModelCount(int stackCount) {
        if (stackCount > 1) {
            if (stackCount > 16) {
                if (stackCount > 32) {
                    if (stackCount > 48) {
                        return 5;
                    } else {
                        return 4;
                    }
                } else {
                    return 3;
                }
            } else {
                return 2;
            }
        } else {
            return 1;
        }
    }

    public static void endItemBatch(PoseStack poseStack) {
        if (inBatch) {
            inBatch = false;
            poseStack.popPose();
        }
    }
}
