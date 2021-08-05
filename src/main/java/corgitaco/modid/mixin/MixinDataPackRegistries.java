package corgitaco.modid.mixin;

import corgitaco.modid.datapack.StructureProtectorFileLoader;
import net.minecraft.command.Commands;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataPackRegistries.class)
public class MixinDataPackRegistries {


    @Shadow @Final private IReloadableResourceManager resources;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addStructureProtectorReloadListener(Commands.EnvironmentType environmentType, int i, CallbackInfo ci) {
        this.resources.registerReloadListener(new StructureProtectorFileLoader());
    }
}
