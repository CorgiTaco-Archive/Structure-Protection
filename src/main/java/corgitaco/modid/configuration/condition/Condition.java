package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import corgitaco.modid.api.StructureProtectionRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.function.Function;

public abstract class Condition {

    public static final Codec<Condition> REGISTRY_CONFIG_CODEC = StructureProtectionRegistry.CONFIG_CONDITION.dispatchStable(Condition::configCodec, Function.identity());
    public static final Codec<Condition> REGISTRY_DISK_CODEC = StructureProtectionRegistry.DISK_CONDITION.dispatchStable(Condition::diskCodec, Function.identity());

    private final boolean perPlayer;

    protected Condition(boolean perPlayer) {
        this.perPlayer = perPlayer;
    }

    public abstract Codec<? extends Condition> configCodec();

    public abstract Codec<? extends Condition> diskCodec();

    public abstract boolean checkIfPasses(ServerPlayerEntity entity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box, BlockPos target, ConditionType type, List<TranslationTextComponent> requirements);

    public void onEntityDeath(LivingEntity dyingEntity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box) {
    }

    public void playerTick(ServerPlayerEntity player, StructureStart<?> structureStart, MutableBoundingBox box) {
    }

    public boolean isPerPlayer() {
        return perPlayer;
    }
}
