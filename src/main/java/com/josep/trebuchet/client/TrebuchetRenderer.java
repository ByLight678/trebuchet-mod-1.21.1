package com.josep.trebuchet.client;

import com.josep.trebuchet.TrebuchetBlock;
import com.josep.trebuchet.TrebuchetBlockEntity;
import com.josep.trebuchet.TrebuchetMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public final class TrebuchetRenderer implements BlockEntityRenderer<TrebuchetBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TrebuchetMod.MOD_ID, "textures/block/trebuchet.png");
    private final ModelPart model;

    public TrebuchetRenderer(BlockEntityRendererProvider.Context context) {
        model = context.bakeLayer(TrebuchetModel.LAYER);
    }

    @Override
    public void render(TrebuchetBlockEntity blockEntity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Direction facing = blockEntity.getBlockState().getValue(TrebuchetBlock.FACING);
        // Renderers are shared: reset before every instance so an idle neighbour never inherits a pose.
        model.getAllParts().forEach(ModelPart::resetPose);
        TrebuchetLaunchAnimation.apply(model, blockEntity.launchSeconds(partialTick));
        pose.pushPose();
        pose.translate(0.5 + facing.getStepX() * 1.5, 0, 0.5 + facing.getStepZ() * 1.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        // ModelPart uses Y-down coordinates and divides model units by 16 itself.
        // Undo only that coordinate convention; retain every original model dimension.
        pose.scale(1, -1, 1);
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TrebuchetBlockEntity blockEntity) {
        // Enclose all four facings and the geometry above the one-block collision base.
        return new AABB(blockEntity.getBlockPos()).inflate(4, 0, 4).expandTowards(0, 4, 0);
    }
}
