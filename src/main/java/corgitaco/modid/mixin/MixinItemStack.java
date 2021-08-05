package corgitaco.modid.mixin;

import corgitaco.modid.StructureProtector;
import corgitaco.modid.configuration.StructureStartProtection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public class MixinItemStack {


    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void cancelUseIfIncorrect(ItemUseContext itemUseContext, CallbackInfoReturnable<ActionResultType> cir) {
        World level = itemUseContext.getLevel();
        if (!level.isClientSide) {
            PlayerEntity player = itemUseContext.getPlayer();
            for (Structure<?> structure : level.getChunkAt(player.blockPosition()).getAllReferences().keySet()) {
                Optional<? extends StructureStart<?>> structureStart = ((ServerWorld) level).startsForFeature(SectionPos.of(player.blockPosition()), structure).findFirst();
                structureStart.ifPresent(start -> {
                    StructureStartProtection protector = ((StructureProtector) start).getProtector();
                    if (protector != null && !protector.conditionsMet((ServerPlayerEntity) player, (ServerWorld) player.level, start, itemUseContext.getClickedPos())) {
                        player.displayClientMessage(new TranslationTextComponent("No bad"), true);
                        cir.setReturnValue(ActionResultType.FAIL);
                    }
                });
            }
        }
    }
}
