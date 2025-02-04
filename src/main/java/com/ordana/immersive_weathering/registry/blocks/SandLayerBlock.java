

package com.ordana.immersive_weathering.registry.blocks;

import com.ordana.immersive_weathering.registry.entities.FallingSandLayerEntity;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Random;

/**
 * Author: MehVahdJukaar
 */

public class SandLayerBlock extends FallingBlock {
    private static final int MAX_LAYERS = 8;
    public static final IntProperty LAYERS = Properties.LAYERS;
    protected static final VoxelShape[] SHAPE_BY_LAYER = new VoxelShape[MAX_LAYERS + 1];
    private final int color;
    private final BlockState sandState;

    static {
        Arrays.setAll(SHAPE_BY_LAYER, l -> Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, l * 2, 16.0D));
        SHAPE_BY_LAYER[0] = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 0.1f, 16.0D);
    }

    public SandLayerBlock(BlockState sandState, int color, Settings properties) {
        super(properties);
        this.color = color;
        this.sandState = sandState;
        this.setDefaultState(this.stateManager.getDefaultState().with(LAYERS, 1));
    }

    @Override
    public int getColor(BlockState state, BlockView reader, BlockPos pos) {
        return this.color;
    }

    @Override
    public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (state.getBlock() != oldState.getBlock())
            worldIn.createAndScheduleBlockTick(pos, this, this.getFallDelay());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
        return SHAPE_BY_LAYER[pState.get(LAYERS)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
        if (pContext instanceof EntityShapeContext c) {
            var e = c.getEntity();
            if (e instanceof LivingEntity) {
                return SHAPE_BY_LAYER[pState.get(LAYERS)];
            }
        }
        return this.getOutlineShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public VoxelShape getSidesShape(BlockState pState, BlockView pReader, BlockPos pPos) {
        return SHAPE_BY_LAYER[pState.get(LAYERS)];
    }

    @Override
    public VoxelShape getCameraCollisionShape(BlockState pState, BlockView pReader, BlockPos pPos, ShapeContext pContext) {
        return SHAPE_BY_LAYER[pState.get(LAYERS)];
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView blockGetter, BlockPos pos, NavigationType pathType) {
        if (pathType == NavigationType.LAND) {
            return state.get(LAYERS) <= MAX_LAYERS;
        }
        return false;
    }

    @Override
    public boolean hasSidedTransparency(BlockState state) {
        return true;
    }

    //ugly but works
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState facingState, WorldAccess world, BlockPos currentPos, BlockPos otherPos) {
        if (world instanceof ServerWorld serverLevel) {
            BlockPos pos = currentPos.up();
            BlockState state1 = world.getBlockState(pos);

            while (state1.isOf(this)) {
                serverLevel.createAndScheduleBlockTick(pos, this, this.getFallDelay());
                pos = pos.up();
                state1 = serverLevel.getBlockState(pos);
            }
        }
        return super.getStateForNeighborUpdate(state, direction, facingState, world, currentPos, otherPos);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld level, BlockPos pos, Random pRand) {
        BlockState below = level.getBlockState(pos.down());
        if ((FallingSandLayerEntity.isFree(below) || hasIncompleteSandPileBelow(below)) && pos.getY() >= level.getBottomY()) {

            while (state.isOf(this)) {
                FallingBlockEntity fallingblockentity = FallingSandLayerEntity.fall(level, pos, state);
                this.configureFallingBlockEntity(fallingblockentity);

                pos = pos.up();
                state = level.getBlockState(pos);
            }
        }
    }

    private boolean hasIncompleteSandPileBelow(BlockState state) {
        return state.isOf(this) && state.get(LAYERS) != MAX_LAYERS;
    }

    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos());
        if (blockState.isOf(this)) {
            int i = blockState.get(LAYERS);
            return blockState.with(LAYERS, Math.min(MAX_LAYERS, i + 1));
        } else {
            return super.getPlacementState(ctx);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(LAYERS);
    }

    @Override
    public boolean canReplace(BlockState pState, ItemPlacementContext pUseContext) {
        int i = pState.get(LAYERS);
        if (pUseContext.getStack().isOf(this.asItem()) && i < MAX_LAYERS) {
            return true;
        } else {
            return i == 1;
        }
    }

    private void addParticle(Entity entity, BlockPos pos, World level, int layers, float upSpeed) {
        level.addParticle(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, sandState), entity.getX(), pos.getY() + layers * (1 / 8f), entity.getZ(),
                MathHelper.nextBetween(level.random, -1.0F, 1.0F) * 0.083333336F,
                upSpeed,
                MathHelper.nextBetween(level.random, -1.0F, 1.0F) * 0.083333336F);
    }

    @Override
    public void onSteppedOn(World level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClient && level.random.nextInt(8) == 0 && (entity.lastRenderX != entity.getX() || entity.lastRenderZ != entity.getZ())) {
            addParticle(entity, pos, level, state.get(LAYERS), 0.05f);
        }
        super.onSteppedOn(level, pos, state, entity);
    }

    @Override
    public void onLandedUpon(World level, BlockState state, BlockPos pos, Entity entity, float height) {
        int layers = state.get(LAYERS);
        entity.handleFallDamage(height, layers > 2 ? 0.3f : 1, DamageSource.FALL);
        if (level.isClient) {
            for (int i = 0; i < Math.min(12, height * 1.4); i++) {

                addParticle(entity, pos, level, layers, 0.12f);
            }
        }
    }
}