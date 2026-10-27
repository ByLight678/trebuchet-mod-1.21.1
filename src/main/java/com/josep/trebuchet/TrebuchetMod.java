package com.josep.trebuchet;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
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
            BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.WOOD).noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> TREBUCHET_ITEM = ITEMS.registerSimpleBlockItem(TREBUCHET);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TREBUCHET_TAB = CREATIVE_TABS.register("trebuchet", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trebuchet"))
                    .icon(() -> TREBUCHET_ITEM.get().getDefaultInstance())
                    .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                    .displayItems((parameters, output) -> output.accept(TREBUCHET_ITEM.get()))
                    .build());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrebuchetBlockEntity>> TREBUCHET_ENTITY =
            BLOCK_ENTITIES.register("trebuchet", () -> BlockEntityType.Builder.of(TrebuchetBlockEntity::new, TREBUCHET.get()).build(null));

    public TrebuchetMod(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, TrebuchetConfig.SPEC);
        modEventBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("2");
            registrar.playToClient(LaunchVelocityPayload.TYPE, LaunchVelocityPayload.CODEC, LaunchVelocityPayload::handle);
            registrar.playToClient(FlightControlPayload.TYPE, FlightControlPayload.CODEC,
                    (payload, context) -> TrebuchetFlightControl.begin(context.player(), payload.ticks()));
        });
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Trebuchet mod initialized");
    }
}
