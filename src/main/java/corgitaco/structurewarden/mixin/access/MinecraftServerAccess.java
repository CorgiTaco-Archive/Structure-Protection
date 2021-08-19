package corgitaco.structurewarden.mixin.access;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccess {

    @Accessor
    IChunkStatusListenerFactory getProgressListenerFactory();

    @Accessor
    Executor getExecutor();

    @Accessor
    SaveFormat.LevelSave getStorageSource();

    @Accessor
    Map<RegistryKey<World>, ServerWorld> getLevels();
}
