package com.josep.trebuchet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TrebuchetMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrebuchetGameTests {
    @GameTest(template = "empty")
    public static void oppositeEndLaunchesEntitiesInReverseDirectionForEveryFacing(GameTestHelper helper) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            placeAndCheck(helper, facing);
            BlockPos anchor = helper.absolutePos(ANCHOR);
            BlockPos pad = TrebuchetBlock.partPosition(ANCHOR, facing, 10);
            Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(pad).above());
            var player = helper.makeMockServerPlayerInLevel();
            // GameTest's embedded connection has no mod handshake. Capture outgoing packets
            // at the listener boundary instead of passing them into the unnegotiated codec.
            var sent = new java.util.ArrayList<net.minecraft.network.protocol.Packet<?>>();
            var mockConnection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(mockConnection);
            player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(
                    helper.getLevel().getServer(), mockConnection,
                    player, net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override
                public void send(net.minecraft.network.protocol.Packet<?> packet) { sent.add(packet); }
            };
            player.setPos(feet);
            var pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, pad.above());
            var tnt = helper.spawn(net.minecraft.world.entity.EntityType.TNT, pad.above());
            tnt.setFuse(75);
            var item = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), feet.x, feet.y, feet.z,
                    new ItemStack(Blocks.STONE));
            helper.getLevel().addFreshEntity(item);
            var adjacent = helper.spawn(net.minecraft.world.entity.EntityType.ARMOR_STAND,
                    pad.above().relative(facing.getClockWise()));
            var overhead = helper.spawn(net.minecraft.world.entity.EntityType.ARMOR_STAND, pad.above(3));
            var oldPad = helper.spawn(net.minecraft.world.entity.EntityType.ARMOR_STAND, ANCHOR.above());
            oldPad.setDeltaMovement(Vec3.ZERO);
            var targets = java.util.List.of(player, pig, tnt, item);
            targets.forEach(entity -> { entity.setDeltaMovement(1, -1, 2); entity.setOnGround(true); });
            adjacent.setDeltaMovement(Vec3.ZERO);
            overhead.setDeltaMovement(Vec3.ZERO);
            // Clicking a different cell must still use the opposite end's middle pad.
            BlockPos clicked = TrebuchetBlock.partPosition(anchor, facing, 11);
            helper.getLevel().getBlockState(clicked).useWithoutItem(helper.getLevel(), player,
                    new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false));
            Vec3 expected = new Vec3(-facing.getStepX() * 13, 7, -facing.getStepZ() * 13);
            helper.assertTrue(sent.stream().anyMatch(packet -> packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom
                    && custom.payload() instanceof LaunchVelocityPayload payload && payload.entityId() == player.getId()
                    && payload.velocity().equals(expected)), "Player did not receive full-speed launch packet");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                LaunchVelocityPayload.CODEC.encode(buffer, new LaunchVelocityPayload(player.getId(), expected));
                helper.assertTrue(LaunchVelocityPayload.CODEC.decode(buffer).velocity().equals(expected), "Network codec clamped launch speed");
            } finally { buffer.release(); }
            for (var entity : targets) {
                helper.assertTrue(entity.getDeltaMovement().equals(expected), "Incorrect 140 m/s up, 260 m/s horizontal launch for " + entity.getType() + " facing " + facing);
                helper.assertTrue(!entity.onGround(), "Launched entity remained grounded");
            }
            helper.assertTrue(adjacent.getDeltaMovement().equals(Vec3.ZERO), "Adjacent entity was launched");
            helper.assertTrue(overhead.getDeltaMovement().equals(Vec3.ZERO), "Overhead entity was launched");
            helper.assertTrue(oldPad.getDeltaMovement().equals(Vec3.ZERO), "Old counterweight-side pad still launches entities");
            helper.assertTrue(tnt.getFuse() == 75, "Launch changed the TNT fuse");
            pig.setDeltaMovement(Vec3.ZERO);
            helper.getLevel().getBlockState(clicked).useWithoutItem(helper.getLevel(), player,
                    new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false));
            helper.assertTrue(pig.getDeltaMovement().equals(Vec3.ZERO), "Repeated click during animation relaunched payload");
            targets.forEach(net.minecraft.world.entity.Entity::discard);
            adjacent.discard();
            overhead.discard();
            oldPad.discard();
            helper.getLevel().destroyBlock(anchor, false);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void everyCellStartsLaunchAndAnimationCanReplay(GameTestHelper helper) {
        placeAndCheck(helper, Direction.SOUTH);
        BlockPos anchor = helper.absolutePos(ANCHOR);
        TrebuchetBlockEntity entity = (TrebuchetBlockEntity) helper.getLevel().getBlockEntity(anchor);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int part = 0; part < 12; part++) {
            // Reset between clicks to test each cell independently without waiting twelve cycles.
            entity.loadAdditional(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess());
            BlockPos cell = TrebuchetBlock.partPosition(anchor, Direction.SOUTH, part);
            BlockState state = helper.getLevel().getBlockState(cell);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(cell), Direction.UP, cell, false);
            state.useWithoutItem(helper.getLevel(), player, hit);
            helper.assertTrue(entity.isLaunching(), "Empty-hand click failed on cell " + part);
            helper.assertTrue(!entity.startLaunch(), "Active animation must not restart");
            entity.loadAdditional(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE));
            state.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(entity.isLaunching(), "Held-item click failed on cell " + part);
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "Animation consumed held item");
        }
        TrebuchetBlockEntity synced = new TrebuchetBlockEntity(anchor, entity.getBlockState());
        synced.setLevel(helper.getLevel());
        synced.handleUpdateTag(entity.getUpdateTag(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(synced.isLaunching(), "Update tag did not synchronize launch");
        helper.runAfterDelay(32, () -> helper.assertTrue(entity.isLaunching(), "Animation stopped before completion"));
        helper.runAfterDelay(64, () -> {
            helper.assertTrue(!entity.isLaunching(), "Animation did not finish after 64 ticks");
            helper.assertTrue(entity.launchSeconds(0) == -1, "Completed animation did not return to idle");
            helper.assertTrue(entity.startLaunch(), "Completed animation cannot replay");
            helper.succeed();
        });
    }

    private static final BlockPos ANCHOR = new BlockPos(4, 1, 4);

    private static BlockPlaceContext placement(GameTestHelper helper, Direction facing) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(ANCHOR);
        player.setYRot(facing.toYRot());
        player.setPos(Vec3.atCenterOf(pos.relative(facing.getOpposite(), 3)));
        ItemStack stack = new ItemStack(TrebuchetMod.TREBUCHET_ITEM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (int part = 0; part < 12; part++) {
            BlockPos cell = TrebuchetBlock.partPosition(pos, facing, part);
            helper.getLevel().setBlockAndUpdate(cell.below(), Blocks.STONE.defaultBlockState());
        }
        return new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false));
    }

    private static void placeAndCheck(GameTestHelper helper, Direction facing) {
        BlockPlaceContext context = placement(helper, facing);
        helper.assertTrue(TrebuchetMod.TREBUCHET_ITEM.get().place(context).consumesAction(), "Placement failed for " + facing);
        helper.assertTrue(context.getItemInHand().getCount() == 1, "Placement must consume one item");
        BlockPos anchor = helper.absolutePos(ANCHOR);
        int entities = 0;
        for (int part = 0; part < 12; part++) {
            BlockPos cell = TrebuchetBlock.partPosition(anchor, facing, part);
            BlockState state = helper.getLevel().getBlockState(cell);
            helper.assertTrue(state.is(TrebuchetMod.TREBUCHET.get()), "Missing cell " + part);
            helper.assertTrue(state.getValue(TrebuchetBlock.FACING) == facing, "Wrong facing");
            helper.assertTrue(TrebuchetBlock.anchorPosition(cell, state).equals(anchor), "Incorrect anchor mapping");
            AABB collision = state.getCollisionShape(helper.getLevel(), cell, CollisionContext.empty()).bounds();
            helper.assertTrue(collision.equals(new AABB(0, 0, 0, 1, 1, 1)), "Base cell must have full one-block collision");
            helper.assertTrue(helper.getLevel().getBlockState(cell.above()).isAir(), "No block may occupy the space above the base");
            if (helper.getLevel().getBlockEntity(cell) != null) entities++;
        }
        helper.assertTrue(entities == 1, "Only one renderer block entity is allowed");
    }

    @GameTest(template = "empty")
    public static void allFacingsHaveTwelveOneBlockHighCells(GameTestHelper helper) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            placeAndCheck(helper, facing);
            helper.getLevel().destroyBlock(helper.absolutePos(ANCHOR), false);
            for (int part = 0; part < 12; part++) {
                helper.assertTrue(helper.getLevel().getBlockState(TrebuchetBlock.partPosition(helper.absolutePos(ANCHOR), facing, part)).isAir(), "Orphaned cell");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingAnyCellDropsOneItemAndRemovesAllCells(GameTestHelper helper) {
        for (int broken = 0; broken < 12; broken++) {
            placeAndCheck(helper, Direction.SOUTH);
            BlockPos anchor = helper.absolutePos(ANCHOR);
            helper.getLevel().destroyBlock(TrebuchetBlock.partPosition(anchor, Direction.SOUTH, broken), true);
            for (int part = 0; part < 12; part++) {
                helper.assertTrue(helper.getLevel().getBlockState(TrebuchetBlock.partPosition(anchor, Direction.SOUTH, part)).isAir(), "Orphaned cell after breaking " + broken);
            }
        }
        helper.assertItemEntityCountIs(TrebuchetMod.TREBUCHET_ITEM.get(), ANCHOR, 8, 12);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void obstructedAndUnsupportedPlacementLeaveWorldAndStackUntouched(GameTestHelper helper) {
        BlockPlaceContext context = placement(helper, Direction.SOUTH);
        BlockPos obstruction = TrebuchetBlock.partPosition(helper.absolutePos(ANCHOR), Direction.SOUTH, 11);
        helper.getLevel().setBlockAndUpdate(obstruction, Blocks.STONE.defaultBlockState());
        helper.assertTrue(!TrebuchetMod.TREBUCHET_ITEM.get().place(context).consumesAction(), "Obstructed footprint must reject placement");
        helper.assertTrue(context.getItemInHand().getCount() == 2, "Failed placement consumed an item");
        helper.assertTrue(helper.getLevel().getBlockState(obstruction).is(Blocks.STONE), "Obstruction was overwritten");
        helper.getLevel().removeBlock(obstruction, false);
        helper.getLevel().removeBlock(obstruction.below(), false);
        helper.assertTrue(!TrebuchetMod.TREBUCHET_ITEM.get().place(context).consumesAction(), "Unsupported footprint must reject placement");
        for (int part = 0; part < 12; part++) {
            helper.assertTrue(helper.getLevel().getBlockState(TrebuchetBlock.partPosition(helper.absolutePos(ANCHOR), Direction.SOUTH, part)).isAir(), "Failed placement left a partial structure");
        }
        helper.assertTrue(context.getItemInHand().getCount() == 2, "Unsupported placement consumed an item");
        helper.succeed();
    }
}
