package corgitaco.structurewarden.mixin;

import corgitaco.structurewarden.StructureProtector;
import corgitaco.structurewarden.configuration.StructureStartProtection;
import corgitaco.structurewarden.configuration.condition.ActionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class MixinItemStack {


    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void cancelUseIfIncorrect(ItemUseContext itemUseContext, CallbackInfoReturnable<ActionResultType> cir) {
        World level = itemUseContext.getLevel();
        if (!level.isClientSide) {
            PlayerEntity player = itemUseContext.getPlayer();
            for (Structure<?> structure : level.getChunkAt(player.blockPosition()).getAllReferences().keySet()) {
                ((ServerWorld) level).startsForFeature(SectionPos.of(player.blockPosition()), structure).filter(structureStart1 -> structureStart1.getBoundingBox().isInside(player.blockPosition())).forEach(start -> {
                    StructureStartProtection protector = ((StructureProtector) start).getProtector();
                    if (protector != null && !protector.conditionsMet((ServerPlayerEntity) player, (ServerWorld) player.level, start, itemUseContext.getClickedPos(), ActionType.BLOCK_PLACE)) {
                        cir.setReturnValue(ActionResultType.FAIL);
                    }
                });
            }
        }
    }
}
