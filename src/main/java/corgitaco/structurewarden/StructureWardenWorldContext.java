package corgitaco.structurewarden;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.gen.feature.structure.StructureStart;

import java.util.List;

public interface StructureWardenWorldContext {

    List<PlayerToSend> getPlayersToSend();

    boolean isStructureDimension();

    void setTargetStructureStart(StructureStart<?> structureStart);

    void isInWorldStructure(ServerPlayerEntity pos);
}