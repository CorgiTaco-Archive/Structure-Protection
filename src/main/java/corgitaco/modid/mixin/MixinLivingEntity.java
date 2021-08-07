package corgitaco.modid.mixin;

import corgitaco.modid.StructureProtector;
import corgitaco.modid.configuration.StructureStartProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    public MixinLivingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void applyCounter(DamageSource source, CallbackInfo ci) {
        if (!level.isClientSide) {
            for (Structure<?> structure : level.getChunkAt(this.blockPosition()).getAllReferences().keySet()) {
                ((ServerWorld) level).startsForFeature(SectionPos.of(this.blockPosition()), structure).filter(structureStart1 -> structureStart1.getBoundingBox().isInside(blockPosition())).forEach(start -> {
                    StructureStartProtection protector = ((StructureProtector) start).getProtector();
                    if (protector != null) {
                        protector.onEntityDeath((LivingEntity) (Object) this, (ServerWorld) level, start);
                    }
                });

            }
        }
    }
}
