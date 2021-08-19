package corgitaco.structurewarden;

import java.util.List;

public interface StructureWardenWorldContext {

    List<PlayerToSend> getPlayersToSend();

    boolean isStructureDimension();

    boolean setStructureDimension();
}