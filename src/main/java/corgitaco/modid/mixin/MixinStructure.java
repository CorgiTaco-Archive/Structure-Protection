package corgitaco.modid.mixin;

import corgitaco.modid.StructureProtector;
import corgitaco.modid.configuration.StructureStartProtection;
import corgitaco.modid.configuration.condition.Condition;
import corgitaco.modid.configuration.condition.KillCondition;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;


@Mixin(Structure.class)
public class MixinStructure {


    @Inject(method = "loadStaticStart", at = @At("RETURN"))
    private static void attachKillsLeft(TemplateManager manager, CompoundNBT nbt, long seed, CallbackInfoReturnable<StructureStart<?>> cir) {
        ArrayList<Condition> conditions = new ArrayList<>();
        conditions.add(new KillCondition(false, 100, 500));

        ((StructureProtector) cir.getReturnValue()).setProtection(new StructureStartProtection(conditions));
    }
}
