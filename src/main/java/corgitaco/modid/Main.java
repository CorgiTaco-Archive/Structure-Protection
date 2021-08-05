package corgitaco.modid;

import corgitaco.modid.api.StructureProtectionRegistry;
import corgitaco.modid.configuration.condition.AdvancementCondition;
import corgitaco.modid.configuration.condition.EntityTypeKillCondition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Path CONFIG_PATH = new File(String.valueOf(FMLPaths.CONFIGDIR.get().resolve(MOD_ID))).toPath();

    public Main() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        registerConditions();
    }

    public static void registerConditions() {
        Registry.register(StructureProtectionRegistry.CONFIG_CONDITION, new ResourceLocation(MOD_ID, "entity_type_kill"), EntityTypeKillCondition.CONFIG_CODEC);
        Registry.register(StructureProtectionRegistry.DISK_CONDITION, new ResourceLocation(MOD_ID, "entity_type_kill"), EntityTypeKillCondition.DISK_CODEC);
        Registry.register(StructureProtectionRegistry.CONFIG_CONDITION, new ResourceLocation(MOD_ID, "advancement"), AdvancementCondition.CONFIG_CODEC);
        Registry.register(StructureProtectionRegistry.DISK_CONDITION, new ResourceLocation(MOD_ID, "advancement"), AdvancementCondition.CONFIG_CODEC);
    }
}
