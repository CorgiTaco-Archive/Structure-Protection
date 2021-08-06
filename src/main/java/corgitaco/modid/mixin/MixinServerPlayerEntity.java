package corgitaco.modid.mixin;

import corgitaco.modid.StructureProtector;
import corgitaco.modid.configuration.StructureStartProtection;
import corgitaco.modid.configuration.condition.ConditionType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.OptionalInt;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity {


    @Inject(method = "openMenu", at = @At("HEAD"), cancellable = true)
    private void protectContainers(INamedContainerProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        if (provider instanceof TileEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            for (Structure<?> structure : player.level.getChunkAt(player.blockPosition()).getAllReferences().keySet()) {
                Optional<? extends StructureStart<?>> structureStart = ((ServerWorld) player.level).startsForFeature(SectionPos.of(player.blockPosition()), structure).findFirst();
                structureStart.ifPresent(start -> {
                    StructureStartProtection protector = ((StructureProtector) start).getProtector();
                    if (protector != null && !protector.conditionsMet(player, (ServerWorld) player.level, start, ((TileEntity) provider).getBlockPos(), ConditionType.CONTAINER_OPEN)) {
                        cir.setReturnValue(OptionalInt.empty());
                    }
                });
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void playerTickProtector(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        for (Structure<?> structure : player.level.getChunkAt(player.blockPosition()).getAllReferences().keySet()) {
            Optional<? extends StructureStart<?>> structureStart = ((ServerWorld) player.level).startsForFeature(SectionPos.of(player.blockPosition()), structure).findFirst();
            structureStart.ifPresent(start -> {
                StructureStartProtection protector = ((StructureProtector) start).getProtector();
                if (protector != null) {
                    protector.playerTick(player, start);
                }
            });
        }
    }
}
