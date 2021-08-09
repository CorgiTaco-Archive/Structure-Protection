package corgitaco.structurewarden.api;

import com.mojang.serialization.Codec;
import corgitaco.structurewarden.StructureWarden;
import corgitaco.structurewarden.configuration.condition.Condition;
import corgitaco.structurewarden.mixin.access.RegistryAccess;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

public class StructureProtectionRegistry {

    public static final RegistryKey<Registry<Codec<? extends Condition>>> DISK_CONDITION_KEY = RegistryKey.createRegistryKey(new ResourceLocation(StructureWarden.MOD_ID, "disk_condition"));

    public static final Registry<Codec<? extends Condition>> DISK_CONDITION = RegistryAccess.invokeRegisterSimple(DISK_CONDITION_KEY, () -> Condition.REGISTRY_DISK_CODEC);

    public static final RegistryKey<Registry<Codec<? extends Condition>>> CONFIG_CONDITION_KEY = RegistryKey.createRegistryKey(new ResourceLocation(StructureWarden.MOD_ID, "condition"));

    public static final Registry<Codec<? extends Condition>> CONFIG_CONDITION = RegistryAccess.invokeRegisterSimple(CONFIG_CONDITION_KEY, () -> Condition.REGISTRY_CONFIG_CODEC);
}
