/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.lib.BcLangKeys;

/**
 * Central item registration for buildcraftrobotics (task M2.4c registry parity). All 40 item ids the 1.20.1 registry
 * baseline attributes to {@code buildcraftrobotics} register here: the two block items, the 18 redstone boards
 * (legacy {@code ItemRedstoneBoard}, one per {@code board_robot_*} id incl. the empty board), the 17 robot items
 * (legacy {@code ItemRobot}, one per {@code robot_*} id incl. {@code robot_base}) and the goggles/station items. All
 * placeholder, behaviour classes migrate in M2.5+.
 */
public final class BcRoboticsItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftRobotics.MOD_ID);

    // boards: legacy ItemRedstoneBoard, one per RedstoneBoardRegistry entry (17 robot boards + the empty board)

    /** Placeholder for {@code buildcraftrobotics:board_robot_bomber} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_BOMBER = BcLangKeys.item(ITEMS, "board_robot_bomber");

    /** Placeholder for {@code buildcraftrobotics:board_robot_builder} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_BUILDER = BcLangKeys.item(ITEMS, "board_robot_builder");

    /** Placeholder for {@code buildcraftrobotics:board_robot_butcher} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_BUTCHER = BcLangKeys.item(ITEMS, "board_robot_butcher");

    /** Placeholder for {@code buildcraftrobotics:board_robot_carrier} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_CARRIER = BcLangKeys.item(ITEMS, "board_robot_carrier");

    /** Placeholder for {@code buildcraftrobotics:board_robot_delivery} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_DELIVERY = BcLangKeys.item(ITEMS, "board_robot_delivery");

    /** Placeholder for {@code buildcraftrobotics:board_robot_empty} (legacy {@code ItemRedstoneBoard} of the empty board); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_EMPTY = BcLangKeys.item(ITEMS, "board_robot_empty");

    /** Placeholder for {@code buildcraftrobotics:board_robot_farmer} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_FARMER = BcLangKeys.item(ITEMS, "board_robot_farmer");

    /** Placeholder for {@code buildcraftrobotics:board_robot_fluid_carrier} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_FLUID_CARRIER = BcLangKeys.item(ITEMS, "board_robot_fluid_carrier");

    /** Placeholder for {@code buildcraftrobotics:board_robot_harvester} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_HARVESTER = BcLangKeys.item(ITEMS, "board_robot_harvester");

    /** Placeholder for {@code buildcraftrobotics:board_robot_knight} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_KNIGHT = BcLangKeys.item(ITEMS, "board_robot_knight");

    /** Placeholder for {@code buildcraftrobotics:board_robot_leave_cutter} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_LEAVE_CUTTER = BcLangKeys.item(ITEMS, "board_robot_leave_cutter");

    /** Placeholder for {@code buildcraftrobotics:board_robot_lumberjack} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_LUMBERJACK = BcLangKeys.item(ITEMS, "board_robot_lumberjack");

    /** Placeholder for {@code buildcraftrobotics:board_robot_miner} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_MINER = BcLangKeys.item(ITEMS, "board_robot_miner");

    /** Placeholder for {@code buildcraftrobotics:board_robot_picker} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_PICKER = BcLangKeys.item(ITEMS, "board_robot_picker");

    /** Placeholder for {@code buildcraftrobotics:board_robot_planter} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_PLANTER = BcLangKeys.item(ITEMS, "board_robot_planter");

    /** Placeholder for {@code buildcraftrobotics:board_robot_pump} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_PUMP = BcLangKeys.item(ITEMS, "board_robot_pump");

    /** Placeholder for {@code buildcraftrobotics:board_robot_shovelman} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_SHOVELMAN = BcLangKeys.item(ITEMS, "board_robot_shovelman");

    /** Placeholder for {@code buildcraftrobotics:board_robot_stripes} (legacy {@code ItemRedstoneBoard}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> BOARD_ROBOT_STRIPES = BcLangKeys.item(ITEMS, "board_robot_stripes");

    // blocks and machines

    /** Item form of the {@code buildcraftrobotics:requester} placeholder block (legacy {@code BlockItem} of {@code BlockRequester}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> REQUESTER = BcLangKeys.blockItem(ITEMS, BcRoboticsBlocks.REQUESTER);

    /** Item form of the {@code buildcraftrobotics:zone_planner} placeholder block (legacy {@code BlockItem} of {@code BlockZonePlanner}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> ZONE_PLANNER = BcLangKeys.blockItem(ITEMS, BcRoboticsBlocks.ZONE_PLANNER);

    // robots: legacy ItemRobot, one per RedstoneBoardRobotNBT (robot_base is the empty-board robot id)

    /** Placeholder for {@code buildcraftrobotics:robot_base} (legacy {@code ItemRobot} of the empty-board robot); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_BASE = BcLangKeys.item(ITEMS, "robot_base");

    /** Placeholder for {@code buildcraftrobotics:robot_bomber} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_BOMBER = BcLangKeys.item(ITEMS, "robot_bomber");

    /** Placeholder for {@code buildcraftrobotics:robot_builder} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_BUILDER = BcLangKeys.item(ITEMS, "robot_builder");

    /** Placeholder for {@code buildcraftrobotics:robot_butcher} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_BUTCHER = BcLangKeys.item(ITEMS, "robot_butcher");

    /** Placeholder for {@code buildcraftrobotics:robot_carrier} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_CARRIER = BcLangKeys.item(ITEMS, "robot_carrier");

    /** Placeholder for {@code buildcraftrobotics:robot_delivery} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_DELIVERY = BcLangKeys.item(ITEMS, "robot_delivery");

    /** Placeholder for {@code buildcraftrobotics:robot_farmer} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_FARMER = BcLangKeys.item(ITEMS, "robot_farmer");

    /** Placeholder for {@code buildcraftrobotics:robot_fluid_carrier} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_FLUID_CARRIER = BcLangKeys.item(ITEMS, "robot_fluid_carrier");

    /** Placeholder for {@code buildcraftrobotics:robot_googles} (legacy {@code ItemRobotGoggles}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_GOOGLES = BcLangKeys.item(ITEMS, "robot_googles");

    /** Placeholder for {@code buildcraftrobotics:robot_harvester} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_HARVESTER = BcLangKeys.item(ITEMS, "robot_harvester");

    /** Placeholder for {@code buildcraftrobotics:robot_knight} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_KNIGHT = BcLangKeys.item(ITEMS, "robot_knight");

    /** Placeholder for {@code buildcraftrobotics:robot_leave_cutter} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_LEAVE_CUTTER = BcLangKeys.item(ITEMS, "robot_leave_cutter");

    /** Placeholder for {@code buildcraftrobotics:robot_lumberjack} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_LUMBERJACK = BcLangKeys.item(ITEMS, "robot_lumberjack");

    /** Placeholder for {@code buildcraftrobotics:robot_miner} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_MINER = BcLangKeys.item(ITEMS, "robot_miner");

    /** Placeholder for {@code buildcraftrobotics:robot_picker} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_PICKER = BcLangKeys.item(ITEMS, "robot_picker");

    /** Placeholder for {@code buildcraftrobotics:robot_planter} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_PLANTER = BcLangKeys.item(ITEMS, "robot_planter");

    /** Placeholder for {@code buildcraftrobotics:robot_pump} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_PUMP = BcLangKeys.item(ITEMS, "robot_pump");

    /** Placeholder for {@code buildcraftrobotics:robot_shovelman} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_SHOVELMAN = BcLangKeys.item(ITEMS, "robot_shovelman");

    /** Placeholder for {@code buildcraftrobotics:robot_station} (legacy {@code ItemRobotStation}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_STATION = BcLangKeys.item(ITEMS, "robot_station");

    /** Placeholder for {@code buildcraftrobotics:robot_stripes} (legacy {@code ItemRobot}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> ROBOT_STRIPES = BcLangKeys.item(ITEMS, "robot_stripes");

    private BcRoboticsItems() {
    }
}
