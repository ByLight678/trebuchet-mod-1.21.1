package com.josep.trebuchet;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TrebuchetMod.MOD_ID)
public final class TrebuchetMod {
    public static final String MOD_ID = "trebuchet";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredBlock<TrebuchetBlock> TREBUCHET = BLOCKS.registerBlock("trebuchet", TrebuchetBlock::new,
            BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> TREBUCHET_ITEM = ITEMS.registerSimpleBlockItem(TREBUCHET);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrebuchetBlockEntity>> TREBUCHET_ENTITY =
            BLOCK_ENTITIES.register("trebuchet", () -> BlockEntityType.Builder.of(TrebuchetBlockEntity::new, TREBUCHET.get()).build(null));

    public TrebuchetMod(IEventBus modEventBus) {
        modEventBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
                event.registrar("1").playToClient(LaunchVelocityPayload.TYPE, LaunchVelocityPayload.CODEC, LaunchVelocityPayload::handle));
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(this::addCreativeItems);
        modEventBus.addListener(this::commonSetup);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TREBUCHET_ITEM);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Trebuchet mod initialized");
    }
}
