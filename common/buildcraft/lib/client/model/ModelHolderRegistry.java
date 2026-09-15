/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.model;

import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModLoadingStage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class ModelHolderRegistry {
    public static final boolean DEBUG = BCDebugging.shouldDebugLog("lib.model.holder");

    static final List<ModelHolder> HOLDERS = new CopyOnWriteArrayList<>();

    public static void onTextureStitchPre() {
        CopyOnWriteArraySet<ResourceLocation> toStitch = new CopyOnWriteArraySet<>();
        for (ModelHolder holder : HOLDERS) {
            holder.onTextureStitchPre(toStitch);
        }

    }

    public static void onDatagenTextureRegister(Consumer<ResourceLocation> consumer, ExistingFileHelper fileHelper) {
        CopyOnWriteArraySet<ResourceLocation> toStitch = new CopyOnWriteArraySet<>();
        for (ModelHolder holder : HOLDERS) {
            holder.onDatagenTextureRegister(toStitch, fileHelper);
        }

        // Sort by location so datagen atlas output is deterministic
        List<ResourceLocation> sorted = new ArrayList<>(toStitch);
        sorted.sort(Comparator.naturalOrder());
        for (ResourceLocation res : sorted) {
            consumer.accept(res);
        }
    }

    public static void onModelBake() {
        for (ModelHolder holder : HOLDERS) {
            holder.onModelBake();
        }
        if (DEBUG && ModLoadingContext.get().getActiveContainer().getCurrentState() == ModLoadingStage.COMPLETE) {
            BCLog.logger.info("[lib.model.holder] List of registered Models:");
            List<ModelHolder> holders = new ArrayList<>();
            holders.addAll(HOLDERS);
            holders.sort(Comparator.comparing(a -> a.modelLocation.toString()));

            for (ModelHolder holder : holders) {
                String status = "  ";
                if (holder.failReason != null) {
                    status += "(" + holder.failReason + ")";
                } else if (!holder.hasBakedQuads()) {
                    status += "(Model was registered too late)";
                }

                BCLog.logger.info("  - " + holder.modelLocation + status);
            }
            BCLog.logger.info("[lib.model.holder] Total of " + HOLDERS.size() + " models");
        }
    }
}
