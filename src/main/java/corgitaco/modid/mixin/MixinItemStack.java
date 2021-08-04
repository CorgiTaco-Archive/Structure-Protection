package corgitaco.modid.mixin;

import corgitaco.modid.StructureKillsLeft;
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
            for (Structure<?> structure : level.getChunkAt(itemUseContext.getPlayer().blockPosition()).getAllReferences().keySet()) {
                Optional<? extends StructureStart<?>> structureStart = ((ServerWorld) level).startsForFeature(SectionPos.of(itemUseContext.getPlayer().blockPosition()), structure).findFirst();
                structureStart.ifPresent(start -> {
                    if (start.getBoundingBox().isInside(itemUseContext.getClickedPos())) {
                        if (((StructureKillsLeft) start).getKillsLeft() > 0) {
                            itemUseContext.getPlayer().displayClientMessage(new TranslationTextComponent("No bad"), true);
                            cir.setReturnValue(ActionResultType.FAIL);
                        }
                    }
                });
            }
        }
    }
}
