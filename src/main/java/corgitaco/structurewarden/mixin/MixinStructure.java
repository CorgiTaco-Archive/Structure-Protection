package corgitaco.structurewarden.mixin;

import corgitaco.structurewarden.StructureWarden;
import corgitaco.structurewarden.StructureProtector;
import corgitaco.structurewarden.configuration.StructureStartProtection;
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
        if (nbt.contains(StructureWarden.PROTECTOR_NBT_TAG)) {
            ((StructureProtector) cir.getReturnValue()).setProtection(StructureStartProtection.DISK_CODEC.decode(NBTDynamicOps.INSTANCE, nbt.get(StructureWarden.PROTECTOR_NBT_TAG)).result().get().getFirst());
        }
    }
}
