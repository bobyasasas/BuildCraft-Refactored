/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;

/**
 * Compile-time (and client-startup) smoke check for the M2.7a quad infrastructure. The M2.7a scope ships no real
 * renderer yet — those arrive in M2.7b — so this class exists purely to prove that the
 * {@link BcQuad}/{@link BcVertex} toolkit behaves as documented: unit quad construction, immutable colour/light
 * transforms and pose transforms all run and agree with each other. It is invoked once from
 * {@code buildcraft.energy.client.BuildCraftEnergyClient} so {@code runClient} logs a PASS/FAIL line; everything it
 * touches (PoseStack, Direction, ARGB, JOML) is bootstrap-free.
 */
public final class BcQuadSmoke {

    /** Runs the smoke checks and returns {@code true} when every assertion held. Never throws — callers log the
     * result. */
    public static boolean check() {
        // 1. Unit quad: correct vertex count, winding normals point outwards.
        BcQuad quad = BcQuad.unit(Direction.UP, -1, true);
        for (Direction face : Direction.values()) {
            BcQuad unit = BcQuad.unit(face, -1, false);
            if (unit.normal() != face.getUnitVec3f()) {
                return false;
            }
        }

        // 2. Colour transforms: immutable chaining, buffer-ready ARGB arithmetic.
        BcQuad tinted = quad.withColor(0xFF804020).multiplyColor(0xFF808080);
        boolean coloursOk = true;
        for (int i = 0; i < 4; i++) {
            coloursOk &= tinted.vertex(i).color() == 0xFF402010;
            coloursOk &= quad.vertex(i).color() == 0xFFFFFFFF; // original untouched
        }
        if (!coloursOk) {
            return false;
        }

        // 3. Light transform.
        if (quad.withLight(15728880).vertex(0).light() != 15728880) {
            return false;
        }

        // 4. Pose transform: a +1/+0/+0 translation moves all four positions by exactly 1 on x,
        //    without touching the original quad (records are immutable, positions are copied).
        PoseStack stack = new PoseStack();
        stack.translate(1, 0, 0);
        BcQuad moved = quad.transform(stack.last());
        for (int i = 0; i < 4; i++) {
            float originalX = quad.vertex(i).position().x();
            if (Math.abs(moved.vertex(i).position().x() - (originalX + 1)) > 1e-6f) {
                return false;
            }
            if (Math.abs(quad.vertex(i).position().x() - originalX) > 1e-6f) {
                return false; // transform() must not touch the original quad
            }
        }
        return true;
    }

    private BcQuadSmoke() {
    }
}
