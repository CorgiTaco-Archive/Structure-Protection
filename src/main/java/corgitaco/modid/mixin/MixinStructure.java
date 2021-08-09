package corgitaco.modid.mixin;

import corgitaco.modid.Main;
import corgitaco.modid.StructureProtector;
import corgitaco.modid.configuration.StructureStartProtection;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
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
    private static void loadProtector(TemplateManager manager, CompoundNBT nbt, long seed, CallbackInfoReturnable<StructureStart<?>> cir) {
        if (nbt.contains(Main.PROTECTOR_NBT_TAG)) {
            ((StructureProtector) cir.getReturnValue()).setProtection(StructureStartProtection.DISK_CODEC.decode(NBTDynamicOps.INSTANCE, nbt.get(Main.PROTECTOR_NBT_TAG)).result().get().getFirst());
        }
    }
}
