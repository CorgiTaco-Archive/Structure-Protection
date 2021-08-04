package corgitaco.modid.mixin;

import corgitaco.modid.StructureKillsLeft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Structure.class)
public class MixinStructure {


    @Inject(method = "loadStaticStart", at = @At("RETURN"))
    private static void attachKillsLeft(TemplateManager manager, CompoundNBT nbt, long seed, CallbackInfoReturnable<StructureStart<?>> cir) {
        if (nbt.contains("killsLeft")) {
            ((StructureKillsLeft) cir.getReturnValue()).setKillsLeft(nbt.getInt("killsLeft"));
        }
    }
}
