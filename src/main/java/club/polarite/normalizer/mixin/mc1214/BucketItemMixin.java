package club.polarite.normalizer.mixin.mc1214;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * <=1.12.2 bucket port
 */
@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Shadow
    @Final
    private Fluid content;

    @Shadow
    public static ItemStack getEmptySuccessItem(ItemStack itemStack, Player player) {
        return ItemStack.EMPTY;
    }

    /**
     * @author ffqq
     * @reason <=1.12.2 buckets ported to modern MC
     */
    @Overwrite
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean isEmpty = this.content == Fluids.EMPTY;

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Vec3 reachVec = eyePos.add(lookVec.scale(reach));
        BlockHitResult hitResult = level.clip(new ClipContext(
                eyePos, reachVec,
                ClipContext.Block.COLLIDER,
                isEmpty ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE,
                player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos targetPos = hitResult.getBlockPos();
        Direction direction = hitResult.getDirection();
        BlockPos placementPos = targetPos.relative(direction);

        if (!level.mayInteract(player, targetPos) || !player.mayUseItemAt(placementPos, direction, stack)) {
            return InteractionResult.FAIL;
        }

        if (isEmpty) {
            BlockState state = level.getBlockState(targetPos);
            ItemStack filledStack = fillBucket(level, player, stack, targetPos, state);
            if (!filledStack.isEmpty()) {
                player.setItemInHand(hand, filledStack);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));

                if (!level.isClientSide && player instanceof ServerPlayer) {
                    CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer) player, filledStack);
                }
                return InteractionResult.SUCCESS.heldItemTransformedTo(filledStack);
            }
            return InteractionResult.FAIL;
        } else {
            if (this.content instanceof FlowingFluid) {
                if (tryPlaceContainedLiquid(level, player, placementPos)) {
                    ItemStack emptyStack = ItemUtils.createFilledResult(stack, player, getEmptySuccessItem(stack, player));
                    player.awardStat(Stats.ITEM_USED.get(stack.getItem()));

                    if (player instanceof ServerPlayer) {
                        CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, placementPos, stack);
                    }
                    return InteractionResult.SUCCESS.heldItemTransformedTo(emptyStack);
                }
            }
            return InteractionResult.FAIL;
        }
    }

    private ItemStack fillBucket(Level level, Player player, ItemStack emptyStack, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof BucketPickup) {
            ItemStack resultStack = ((BucketPickup) block).pickupBlock(player, level, pos, state);
            if (!resultStack.isEmpty()) {
                return ItemUtils.createFilledResult(emptyStack, player, resultStack);
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean tryPlaceContainedLiquid(Level level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (level.dimensionType().ultraWarm() && this.content.is(FluidTags.WATER)) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + level.random.nextFloat() * 0.8F);

            for (int i = 0; i < 8; ++i) {
                level.addParticle(ParticleTypes.LARGE_SMOKE,
                        pos.getX() + Math.random(),
                        pos.getY() + Math.random(),
                        pos.getZ() + Math.random(),
                        0.0D, 0.0D, 0.0D);
            }
        } else {
            if (!level.isClientSide && state.canBeReplaced(this.content)) {
                level.destroyBlock(pos, true);
            }
            level.setBlock(pos, this.content.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        }
        return true;
    }
}