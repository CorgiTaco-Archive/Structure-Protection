package corgitaco.structurewarden.mixin.access;

import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.Dimension;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DimensionGeneratorSettings.class)
public interface DimensionGeneratorSettingsAccess {

    @Accessor
    void setDimensions(SimpleRegistry<Dimension> dimensionSimpleRegistry);
}
