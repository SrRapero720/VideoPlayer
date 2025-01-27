package com.github.NGoedix.watchvideo.block.entity;

import com.github.NGoedix.watchvideo.Reference;
import com.github.NGoedix.watchvideo.block.ModBlocks;
import com.github.NGoedix.watchvideo.block.entity.custom.HandRadioBlockEntity;
import com.github.NGoedix.watchvideo.block.entity.custom.RadioBlockEntity;
import com.github.NGoedix.watchvideo.block.entity.custom.TVBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, Reference.MOD_ID);

    public static final RegistryObject<BlockEntityType<TVBlockEntity>> TV_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("tv_block_entity", () ->
                    BlockEntityType.Builder.of(TVBlockEntity::new, ModBlocks.TV_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<RadioBlockEntity>> RADIO_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("radio_block_entity", () ->
                    BlockEntityType.Builder.of(RadioBlockEntity::new, ModBlocks.RADIO_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<HandRadioBlockEntity>> HAND_RADIO_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("hand_radio_block_entity", () ->
                    BlockEntityType.Builder.of(HandRadioBlockEntity::new, ModBlocks.HAND_RADIO_BLOCK.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
