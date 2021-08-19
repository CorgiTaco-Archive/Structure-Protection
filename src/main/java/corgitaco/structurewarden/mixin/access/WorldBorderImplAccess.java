package corgitaco.structurewarden.mixin.access;

import net.minecraft.world.border.IBorderListener;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IBorderListener.Impl.class)
public interface WorldBorderImplAccess {

    @Accessor
    WorldBorder getWorldBorder();
}
