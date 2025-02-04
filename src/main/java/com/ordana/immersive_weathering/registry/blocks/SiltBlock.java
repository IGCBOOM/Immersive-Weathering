package com.ordana.immersive_weathering.registry.blocks;

import com.ordana.immersive_weathering.ImmersiveWeathering;
import com.ordana.immersive_weathering.block_growth.IConditionalGrowingBlock;
import com.ordana.immersive_weathering.registry.ModParticles;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Random;

public class SiltBlock extends Block implements IConditionalGrowingBlock {
    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    protected static final VoxelShape PUDDLE_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

    public SiltBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(SOAKED, false).with(FERTILE, true));
    }

    public static final BooleanProperty SOAKED = BooleanProperty.of("soaked");
    public static final BooleanProperty FERTILE = BooleanProperty.of("fertile");

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(SOAKED);
        stateManager.add(FERTILE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(SOAKED)) {
            return PUDDLE_SHAPE;
        }
        else return SHAPE;
    }

    @Override
    public VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        if (state.get(SOAKED)) {
            return PUDDLE_SHAPE;
        }
        else return SHAPE;
    }

    @Override
    public boolean canGrow(BlockState state) {
        return state.get(FERTILE) && state.get(SOAKED);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.hasRain(pos.up())) {
            world.setBlockState(pos, state.with(SOAKED, true));
        }
        else world.setBlockState(pos, state.with(SOAKED, false));
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (state.get(SOAKED)) {
            if (world.isClient && (!(entity instanceof LivingEntity) || entity.getBlockStateAtPos().isOf(this))) {
                Random random = world.getRandom();
                boolean bl = entity.lastRenderX != entity.getX() || entity.lastRenderZ != entity.getZ();
                if (bl && random.nextBoolean()) {
                    world.addParticle(
                            ParticleTypes.SPLASH,
                            false,
                            entity.getX() + MathHelper.nextBetween(random, -0.2f, 0.2f),
                            pos.getY() + 1D,
                            entity.getZ() + MathHelper.nextBetween(random, -0.2f, 0.2f),
                            0D, 0.25D, 0D);
                }
            }
        }
    }
}


