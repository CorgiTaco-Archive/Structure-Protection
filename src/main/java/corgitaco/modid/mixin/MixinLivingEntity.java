package corgitaco.modid.mixin;

import corgitaco.modid.Main;
import corgitaco.modid.StructureKillsLeft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    @Shadow
    @Nullable
    protected PlayerEntity lastHurtByPlayer;

    public MixinLivingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void applyCounter(DamageSource source, CallbackInfo ci) {
        if (!level.isClientSide) {
            // We cant use this
            for (Structure<?> structure : level.getChunkAt(this.blockPosition()).getAllReferences().keySet()) {
                Optional<? extends StructureStart<?>> structureStart = ((ServerWorld) level).startsForFeature(SectionPos.of(this.blockPosition()), structure).findFirst();
                structureStart.ifPresent(start -> {
                    if (((LivingEntity) (Object) this) instanceof MonsterEntity && this.lastHurtByPlayer != null && start.getBoundingBox().isInside(blockPosition())) {
                        ((StructureKillsLeft) start).setKillsLeft(((StructureKillsLeft) start).getKillsLeft() - 1);
                        Main.LOGGER.info("Kills left: " + ((StructureKillsLeft) start).getKillsLeft());
                    }
                });
            }
        }
    }
}
