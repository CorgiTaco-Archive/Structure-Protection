package corgitaco.structurewarden;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.gen.feature.structure.StructureStart;

public class PlayerToSend {

    private final ServerPlayerEntity playerEntity;
    private final StructureStart<?> structureStart;
    private final Vector3d target;

    public PlayerToSend(ServerPlayerEntity playerEntity, StructureStart<?> structureStart, Vector3d target) {
        this.playerEntity = playerEntity;
        this.structureStart = structureStart;
        this.target = target;
    }

    public ServerPlayerEntity getPlayerEntity() {
        return playerEntity;
    }

    public StructureStart<?> getStructureStart() {
        return structureStart;
    }

    public Vector3d getTarget() {
        return target;
    }
}
