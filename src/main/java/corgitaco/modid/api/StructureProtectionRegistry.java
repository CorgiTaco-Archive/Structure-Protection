package corgitaco.modid.api;

import com.mojang.serialization.Codec;
import corgitaco.modid.Main;
import corgitaco.modid.configuration.condition.Condition;
import corgitaco.modid.mixin.access.RegistryAccess;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

public class StructureProtectionRegistry {

    public static final RegistryKey<Registry<Codec<? extends Condition>>> CONDITION_KEY = RegistryKey.createRegistryKey(new ResourceLocation(Main.MOD_ID, "condition"));

    public static final Registry<Codec<? extends Condition>> CONDITION = RegistryAccess.invokeRegisterSimple(CONDITION_KEY, () -> Condition.CODEC);


}
