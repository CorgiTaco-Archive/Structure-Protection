package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import corgitaco.modid.api.StructureProtectionRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.function.Function;

public abstract class Condition {

    public static final Codec<Condition> CODEC = StructureProtectionRegistry.CONDITION.dispatchStable(Condition::codec, Function.identity());

    private final boolean perPlayer;

    protected Condition(boolean perPlayer) {
        this.perPlayer = perPlayer;
    }

    public abstract Codec<? extends Condition> codec();

    public abstract CompoundNBT write();

    public abstract void read(CompoundNBT readNBT);

    public abstract boolean checkIfPasses(ServerPlayerEntity entity, ServerWorld serverWorld, StructureStart<?> structureStart);

    public void onEntityDie(LivingEntity dyingEntity, ServerWorld serverWorld, StructureStart<?> structureStart) {
    }

    public abstract TranslationTextComponent textComponent();

    public boolean isPerPlayer() {
        return perPlayer;
    }
}
