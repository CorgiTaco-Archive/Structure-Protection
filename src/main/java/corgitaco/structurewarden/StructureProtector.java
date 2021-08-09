package corgitaco.structurewarden;

import corgitaco.structurewarden.configuration.StructureStartProtection;

import javax.annotation.Nullable;

public interface StructureProtector {

    @Nullable
    StructureStartProtection getProtector();

    void setProtection(StructureStartProtection killsLeft);
}
