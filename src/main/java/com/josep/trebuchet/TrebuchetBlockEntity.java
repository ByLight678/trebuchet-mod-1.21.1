package com.josep.trebuchet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TrebuchetBlockEntity extends BlockEntity {
    public static final int LAUNCH_TICKS = 64;
    private long launchStart = -1;

    public TrebuchetBlockEntity(BlockPos pos, BlockState state) {
        super(TrebuchetMod.TREBUCHET_ENTITY.get(), pos, state);
    }

    public boolean startLaunch() {
        if (level == null || level.isClientSide || isLaunching()) return false;
        launchStart = level.getGameTime();
        launchEntities();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    private void launchEntities() {
        Direction facing = getBlockState().getValue(TrebuchetBlock.FACING);
        Vec3 velocity = TrebuchetConfig.launchVelocity(facing);
        BlockPos launchPad = TrebuchetBlock.partPosition(worldPosition, facing, 10);
        double top = launchPad.getY() + 1.0;
        // Launch from the opposite end's middle cell (part 10), not the renderer anchor.
        // Select by feet and horizontal centre,
        // not by body overlap, so adjacent tiles and entities overhead are unaffected.
        AABB surface = new AABB(launchPad.getX(), top - 0.15, launchPad.getZ(),
                launchPad.getX() + 1, top + 0.25, launchPad.getZ() + 1);
        for (Entity entity : level.getEntities((Entity) null, surface, candidate -> candidate.isAlive()
                && candidate.getX() >= surface.minX && candidate.getX() < surface.maxX
                && candidate.getZ() >= surface.minZ && candidate.getZ() < surface.maxZ
                && candidate.getY() >= surface.minY && candidate.getY() <= surface.maxY)) {
            LaunchMotionSync.launch(entity, velocity);
        }
    }

    public boolean isLaunching() {
        return level != null && launchStart >= 0 && level.getGameTime() >= launchStart
                && level.getGameTime() - launchStart < LAUNCH_TICKS;
    }

    public float launchSeconds(float partialTick) {
        if (!isLaunching()) return -1;
        return (level.getGameTime() - launchStart + partialTick) / 20F;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("LaunchStart", launchStart);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        launchStart = tag.contains("LaunchStart") ? tag.getLong("LaunchStart") : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
