package corgitaco.modid;

import corgitaco.modid.configuration.StructureStartProtection;

import javax.annotation.Nullable;

public interface StructureProtector {

    @Nullable
    StructureStartProtection getProtector();

    void setProtection(StructureStartProtection killsLeft);
}
