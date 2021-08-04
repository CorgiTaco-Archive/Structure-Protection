package corgitaco.modid.mixin;

import corgitaco.modid.StructureKillsLeft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureStart.class)
public abstract class MixinStructureStart<C extends IFeatureConfig> implements StructureKillsLeft {


    @Shadow
    @Final
    protected SharedSeedRandom random;
    private int killsLeft;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void killsLeft(Structure<C> structure, int x, int z, MutableBoundingBox boundingBox, int i, long seed, CallbackInfo ci) {
        this.killsLeft = this.random.nextInt(1000);
    }


    @Inject(method = "createTag", at = @At("RETURN"))
    private void attachConditions(int x, int z, CallbackInfoReturnable<CompoundNBT> cir) {
        cir.getReturnValue().putInt("killsLeft", this.killsLeft);
    }

    @Override
    public int getKillsLeft() {
        return killsLeft;
    }

    @Override
    public void setKillsLeft(int killsLeft) {
        this.killsLeft = killsLeft;
    }
}
