package corgitaco.structurewarden.mixin.access;

import com.google.common.collect.BiMap;
import com.mojang.serialization.Lifecycle;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SimpleRegistry.class)
public interface SimpleRegistryAccess<T> {

    @Accessor
    BiMap<RegistryKey<T>, T> getKeyStorage();

    @Accessor
    Map<T, Lifecycle> getLifecycles();
}
