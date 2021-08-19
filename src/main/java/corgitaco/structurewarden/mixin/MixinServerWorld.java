package corgitaco.structurewarden.mixin;

import corgitaco.structurewarden.PlayerToSend;
import corgitaco.structurewarden.StructureWardenWorldContext;
import corgitaco.structurewarden.util.DimensionHelper;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public class MixinServerWorld implements StructureWardenWorldContext {

    public List<PlayerToSend> playersToSend = new ArrayList<>();

    boolean isStructureDimension = false;

    @Inject(method = "tick" ,at = @At(value = "FIELD", target = "Lnet/minecraft/world/server/ServerWorld;tickingEntities:Z", shift = At.Shift.AFTER))
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
    public boolean setStructureDimension() {
        return this.isStructureDimension = true;
    }
}
