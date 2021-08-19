package corgitaco.structurewarden.mixin;

import corgitaco.structurewarden.PlayerToSend;
import corgitaco.structurewarden.StructureProtector;
import corgitaco.structurewarden.StructureWarden;
import corgitaco.structurewarden.StructureWardenWorldContext;
import corgitaco.structurewarden.util.DimensionHelper;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.ISpecialSpawner;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraft.world.storage.ISpawnWorldInfo;
import net.minecraft.world.storage.SaveFormat;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World implements StructureWardenWorldContext {

    public List<PlayerToSend> playersToSend = new ArrayList<>();

    boolean isStructureDimension = false;

    StructureStart<?> cachedStart = null;

    protected MixinServerWorld(ISpawnWorldInfo p_i241925_1_, RegistryKey<World> p_i241925_2_, DimensionType p_i241925_3_, Supplier<IProfiler> p_i241925_4_, boolean p_i241925_5_, boolean p_i241925_6_, long p_i241925_7_) {
        super(p_i241925_1_, p_i241925_2_, p_i241925_3_, p_i241925_4_, p_i241925_5_, p_i241925_6_, p_i241925_7_);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setStructureDimensionMarker(MinecraftServer server, Executor executor, SaveFormat.LevelSave save, IServerWorldInfo worldInfo, RegistryKey<World> worldRegistryKey, DimensionType p_i241885_6_, IChunkStatusListener p_i241885_7_, ChunkGenerator p_i241885_8_, boolean p_i241885_9_, long p_i241885_10_, List<ISpecialSpawner> p_i241885_12_, boolean p_i241885_13_, CallbackInfo ci) {
        this.isStructureDimension = worldRegistryKey.location().toString().contains(StructureWarden.MOD_ID);
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/server/ServerWorld;tickingEntities:Z", shift = At.Shift.AFTER))
    private void sendPlayers(BooleanSupplier supplier, CallbackInfo ci) {
        int size = playersToSend.size();
        for (int i = 0; i < size; i++) {
            PlayerToSend removed = playersToSend.remove(i);
            DimensionHelper.getOrCreateDimensionAndSendPlayerFromStructureStart(removed.getPlayerEntity(), removed.getStructureStart(), removed.getTarget());
        }
    }

    @Override
    public List<PlayerToSend> getPlayersToSend() {
        return this.playersToSend;
    }

    @Override
    public boolean isStructureDimension() {
        return this.isStructureDimension;
    }

    @Override
    public void setTargetStructureStart(StructureStart<?> structureStart) {
        this.cachedStart = this.getChunk(structureStart.getChunkX(), structureStart.getChunkZ()).getAllStarts().get(structureStart.getFeature());
    }

    @Override
    public void isInWorldStructure(ServerPlayerEntity serverPlayerEntity) {
        MutableBoolean foundStructure = new MutableBoolean(false);
        ((ServerWorld)(Object) this).startsForFeature(SectionPos.of(serverPlayerEntity), cachedStart.getFeature()).filter(structureStart1 -> structureStart1.getChunkX() == cachedStart.getChunkX() && structureStart1.getChunkZ() == cachedStart.getChunkZ()).forEach(start -> {
            foundStructure.setTrue();
            ((StructureProtector) start).getProtector().playerTick(serverPlayerEntity, start);
        });

        if (foundStructure.isFalse()) {
            serverPlayerEntity.hurt(DamageSource.IN_WALL, 1000F);
        }
    }
}
