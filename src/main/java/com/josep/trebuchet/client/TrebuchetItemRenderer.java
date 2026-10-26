package com.josep.trebuchet.client;

import com.josep.trebuchet.TrebuchetMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Inventory/hand rendering only; world geometry always retains its original scale. */
public final class TrebuchetItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TrebuchetMod.MOD_ID, "textures/block/trebuchet.png");

    public TrebuchetItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.08, 0.5);
        pose.scale(0.22F, -0.22F, 0.22F);
        // Bake against the current layer definitions so resource reloads also update the item.
        Minecraft.getInstance().getEntityModels().bakeLayer(TrebuchetModel.LAYER)
                .render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }
}
