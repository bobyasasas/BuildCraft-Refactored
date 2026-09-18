/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

import net.minecraft.core.Direction;
import buildcraft.lib.expression.api.NodeType;
import buildcraft.lib.expression.api.NodeTypes;

/**
 * The Minecraft-specific expression types the jsonbc parser needs (M4.4) &mdash; the subset of the legacy
 * {@code buildcraft.lib.misc.ExpressionCompat} used by the model files: {@code Facing.*} constants so expressions like
 * {@code direction != Facing.UP} (jsonbc rules) compile and evaluate.
 */
public final class JsonModelExpressionTypes {

    /** The {@code Facing} expression type; also registered globally as {@code NodeTypes.addType("Facing", ...)} so
     * the {@code Facing.<constant>} syntax resolves, exactly like the legacy {@code ExpressionCompat.ENUM_FACING}. */
    public static final NodeType<Direction> ENUM_FACING;

    static {
        ENUM_FACING = new NodeType<>("Facing", Direction.UP);
        NodeTypes.addType("Facing", ENUM_FACING);
        ENUM_FACING.put_t_t("getOpposite", Direction::getOpposite);
        ENUM_FACING.put_t_o("(string)", String.class, Direction::getName);
        for (Direction f : Direction.values()) {
            ENUM_FACING.putConstant("" + f, f);
        }
    }

    private JsonModelExpressionTypes() {
    }
}
