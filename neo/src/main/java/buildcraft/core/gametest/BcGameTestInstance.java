/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.gametest;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Code-defined game test instance: runs a {@link Consumer} directly instead of resolving a function key through the
 * vanilla {@code minecraft:test_function} registry (task M2.2a).
 *
 * <p>The vanilla test function registry bootstraps and freezes before mod constructors run in the headless gametest
 * server, so a {@link net.minecraft.gametest.framework.FunctionGameTestInstance} can never resolve mod-provided
 * functions. Instances of this class are created in code and registered through NeoForge's
 * {@link net.neoforged.neoforge.event.RegisterGameTestsEvent}, which is the supported mod path into
 * {@code minecraft:test_instance}; they are never data-(de)serialized, so {@link #codec()} is a constant placeholder.
 */
public final class BcGameTestInstance extends GameTestInstance {

    public static final MapCodec<BcGameTestInstance> CODEC = MapCodec.unit((Supplier<BcGameTestInstance>) () -> {
        throw new IllegalStateException("BcGameTestInstance is code-registered and cannot be deserialized");
    });

    private final Consumer<GameTestHelper> function;

    public BcGameTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> info, Consumer<GameTestHelper> function) {
        super(info);
        this.function = function;
    }

    @Override
    public void run(GameTestHelper helper) {
        this.function.accept(helper);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("buildcraft_function");
    }
}
