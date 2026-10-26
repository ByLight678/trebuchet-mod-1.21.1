package com.josep.trebuchet;

import javax.annotation.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Twelve occupied base cells; the upper model deliberately has no collision. */
public final class TrebuchetBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 11);
    public static final int ANCHOR = 1; // Middle cell of the rear row.

    public TrebuchetBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, ANCHOR));
    }

    public static BlockPos partPosition(BlockPos anchor, Direction facing, int part) {
        return anchor.relative(facing, part / 3).relative(facing.getCounterClockWise(), part % 3 - 1);
    }

    public static BlockPos anchorPosition(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        int part = state.getValue(PART);
        return pos.relative(facing, -(part / 3)).relative(facing.getCounterClockWise(), 1 - part % 3);
    }

    private void launch(BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide && level.getBlockEntity(anchorPosition(pos, state)) instanceof TrebuchetBlockEntity trebuchet) {
            trebuchet.startLaunch();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        launch(state, level, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                            Player player, InteractionHand hand, BlockHitResult hit) {
        launch(state, level, pos);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        Level level = context.getLevel();
        for (int part = 0; part < 12; part++) {
            BlockPos cell = partPosition(context.getClickedPos(), facing, part);
            if (!level.hasChunkAt(cell) || level.isOutsideBuildHeight(cell)
                    || !level.getWorldBorder().isWithinBounds(cell)
                    || !level.getBlockState(cell).canBeReplaced(context)
                    || !level.getBlockState(cell.below()).isFaceSturdy(level, cell.below(), Direction.UP)
                    || !level.isUnobstructed(state, cell, CollisionContext.empty())) {
                return null;
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            for (int part = 0; part < 12; part++) {
                if (part != ANCHOR) {
                    level.setBlock(partPosition(pos, state.getValue(FACING), part), state.setValue(PART, part), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos anchor = anchorPosition(pos, state);
            Direction facing = state.getValue(FACING);
            for (int part = 0; part < 12; part++) {
                BlockPos cell = partPosition(anchor, facing, part);
                if (cell.equals(pos)) continue;
                BlockState other = level.getBlockState(cell);
                if (other.is(this) && other.getValue(FACING) == facing && other.getValue(PART) == part) {
                    // Removing siblings without loot leaves exactly the originally broken cell's drop.
                    level.removeBlock(cell, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == ANCHOR ? new TrebuchetBlockEntity(pos, state) : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}
