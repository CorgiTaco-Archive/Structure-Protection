package corgitaco.structurewarden.mixin.access;

import net.minecraft.util.SharedSeedRandom;
import net.minecraft.world.gen.feature.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureStart.class)
public interface StructureStartAccess {

    @Accessor
    SharedSeedRandom getRandom();
}
