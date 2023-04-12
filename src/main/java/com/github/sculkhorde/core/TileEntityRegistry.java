package com.github.sculkhorde.core;

import com.github.sculkhorde.common.tileentity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class TileEntityRegistry {

    public static DeferredRegister<BlockEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, SculkHorde.MOD_ID);

    public static RegistryObject<BlockEntityType<SculkMassTile>> SCULK_MASS_TILE =
            TILE_ENTITIES.register("sculk_mass_tile", () -> BlockEntityType.Builder.of(
                    SculkMassTile::new, BlockRegistry.SCULK_MASS.get()).build(null));

    public static RegistryObject<BlockEntityType<SculkNodeTile>> SCULK_BRAIN_TILE =
            TILE_ENTITIES.register("sculk_brain_tile", () -> BlockEntityType.Builder.of(
                    SculkNodeTile::new, BlockRegistry.SCULK_NODE_BLOCK.get()).build(null));

    public static RegistryObject<BlockEntityType<SculkBeeNestTile>> SCULK_BEE_NEST_TILE =
            TILE_ENTITIES.register("sculk_bee_nest_tile", () -> BlockEntityType.Builder.of(
                    SculkBeeNestTile::new, BlockRegistry.SCULK_BEE_NEST_BLOCK.get()).build(null));

    public static RegistryObject<BlockEntityType<SculkBeeNestCellTile>> SCULK_BEE_NEST_CELL_TILE =
            TILE_ENTITIES.register("sculk_bee_nest_cell_tile", () -> BlockEntityType.Builder.of(
                    SculkBeeNestCellTile::new, BlockRegistry.SCULK_BEE_NEST_CELL_BLOCK.get()).build(null));

    public static RegistryObject<BlockEntityType<SculkSummonerTile>> SCULK_SUMMONER_TILE =
            TILE_ENTITIES.register("sculk_summoner_tile", () -> BlockEntityType.Builder.of(
                    SculkSummonerTile::new, BlockRegistry.SCULK_SUMMONER_BLOCK.get()).build(null));

    public static RegistryObject<BlockEntityType<SculkLivingRockRootTile>> SCULK_LIVING_ROCK_ROOT_TILE =
            TILE_ENTITIES.register("sculk_living_rock_root_tile", () -> BlockEntityType.Builder.of(
                    SculkLivingRockRootTile::new, BlockRegistry.SCULK_LIVING_ROCK_ROOT_BLOCK.get()).build(null));

    public static RegistryObject<BlockEntityType<DevStructureTesterTile>> DEV_STRUCTURE_TESTER_TILE =
            TILE_ENTITIES.register("dev_structure_tester_tile", () -> BlockEntityType.Builder.of(
                    DevStructureTesterTile::new, BlockRegistry.DEV_STRUCTURE_TESTER_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        TILE_ENTITIES.register(eventBus);
    }
}
