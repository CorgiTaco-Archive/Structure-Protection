package corgitaco.structurewarden.mixin.access;

import net.minecraft.world.border.IBorderListener;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(WorldBorder.class)
public interface WorldBorderAccess {

    @Accessor
    List<IBorderListener> getListeners();
}
