package corgitaco.structurewarden.mixin;

import corgitaco.structurewarden.StructureWarden;
import corgitaco.structurewarden.StructureProtector;
import corgitaco.structurewarden.configuration.StructureStartProtection;
import corgitaco.structurewarden.datapack.StructureProtectorFileLoader;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(StructureStart.class)
public abstract class MixinStructureStart<C extends IFeatureConfig> implements StructureProtector {

    @Shadow
    public abstract boolean isValid();

    @Nullable
    StructureStartProtection structureStartProtection;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void createProtectorDefaultFromConfiguration(Structure<C> structure, int x, int z, MutableBoundingBox boundingBox, int i, long seed, CallbackInfo ci) {
        if (StructureProtectorFileLoader.PROTECTOR.containsKey(structure)) {
            // Make sure we create a new instance!
            this.structureStartProtection = StructureStartProtection.CONFIG_CODEC.parse(NBTDynamicOps.INSTANCE, StructureStartProtection.CONFIG_CODEC.encodeStart(NBTDynamicOps.INSTANCE, StructureProtectorFileLoader.PROTECTOR.get(structure)).result().get()).result().get();
        }
    }


    @Inject(method = "createTag", at = @At("RETURN"))
    private void saveProtector(int x, int z, CallbackInfoReturnable<CompoundNBT> cir) {
        if (this.structureStartProtection != null && isValid()) {
            cir.getReturnValue().put(StructureWarden.PROTECTOR_NBT_TAG, StructureStartProtection.DISK_CODEC.encodeStart(NBTDynamicOps.INSTANCE, structureStartProtection).result().get());
        }
    }

    @Override
    public StructureStartProtection getProtector() {
        return this.structureStartProtection;
    }

    @Override
    public void setProtection(StructureStartProtection structureStartProtection) {
        this.structureStartProtection = structureStartProtection;
    }
}
