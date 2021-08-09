package corgitaco.structurewarden.datapack;

import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import corgitaco.structurewarden.StructureWarden;
import corgitaco.structurewarden.configuration.StructureStartProtection;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.structure.Structure;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class StructureProtectorFileLoader extends JsonReloadListener {

    public static final Map<Structure<?>, StructureStartProtection> PROTECTOR = new Object2ObjectArrayMap<>();

    private static final Gson GSON = (new GsonBuilder()).create();

    public StructureProtectorFileLoader() {
        super(GSON, "structure_protector");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> elements, IResourceManager resourceManager, IProfiler profiler) {
        PROTECTOR.clear();

        for (Map.Entry<ResourceLocation, JsonElement> locationConfiguration : elements.entrySet()) {
            deserializeFromDataPackDirectory(locationConfiguration);
        }

        serializeFromConfigDirectory();
    }

    public static void serializeFromConfigDirectory() {
        File configDir = StructureWarden.CONFIG_PATH.toFile();
        if (!configDir.exists()) {
            try {
                Files.createDirectories(configDir.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File[] files = configDir.listFiles();
        if (files.length == 0) {
            Object2ObjectArrayMap<Structure<?>, StructureStartProtection> serializedMap = new Object2ObjectArrayMap<>();
            serializedMap.putAll(PROTECTOR);
            serializedMap.putAll(StructureStartProtection.DEFAULTS);
            serializeProtections(serializedMap, true);
        } else {
            for (File file : files) {
                String fileName = file.getName();
                try {
                    JsonObject object = new JsonParser().parse(new FileReader(file)).getAsJsonObject();
                    if (fileName.equalsIgnoreCase("structureProtector.json")) {
                        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                            ResourceLocation entryLocation = ResourceLocation.tryParse(entry.getKey());
                            if (entryLocation == null) {
                                StructureWarden.LOGGER.error(entry.getKey() + " is not a correct resource location! Skipping entry...");
                                continue;
                            }
                            getStructureStartProtectionFromJson(entryLocation, entry.getValue(), file.getAbsolutePath());
                        }
                    } else {
                        ResourceLocation location = ResourceLocation.tryParse(fileName.replace("-", ":"));
                        if (location != null) {
                            getStructureStartProtectionFromJson(location, object, file.getAbsolutePath());
                        } else {
                            StructureWarden.LOGGER.error("\"" + fileName + " is an invalid resource location. The file name should follow this naming scheme: \"modid-structurename.json\" or for a single file, \"structureprotector.json\". Skipping this file & its entries...");
                        }
                    }
                } catch (IOException e) {
                    StructureWarden.LOGGER.error(e.toString());
                }
            }
        }
    }

    private void deserializeFromDataPackDirectory(Map.Entry<ResourceLocation, JsonElement> locationConfiguration) {
        JsonElement value = locationConfiguration.getValue();

        ResourceLocation fileLocation = locationConfiguration.getKey();
        ResourceLocation location = new ResourceLocation(fileLocation.toString().replace("structure_protector/", ""));
        if (location.getPath().equalsIgnoreCase("structureprotector")) {
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                ResourceLocation entryLocation = ResourceLocation.tryParse(entry.getKey());
                if (entryLocation == null) {
                    StructureWarden.LOGGER.error(entry.getKey() + " is not a correct resource location! Skipping entry...");
                    continue;
                }
                getStructureStartProtectionFromJson(entryLocation, value, fileLocation.toString());
            }
        } else {
            getStructureStartProtectionFromJson(location, value, fileLocation.toString());
        }
    }

    public static void getStructureStartProtectionFromJson(ResourceLocation structureLocation, JsonElement element, String fileLocation) {
        Optional<Structure<?>> optional = Registry.STRUCTURE_FEATURE.getOptional(structureLocation);

        if (optional.isPresent()) {
            DataResult<StructureStartProtection> parsed = StructureStartProtection.CONFIG_CODEC.parse(JsonOps.INSTANCE, element);
            Optional<StructureStartProtection> result = parsed.result();

            if (result.isPresent()) {
                PROTECTOR.put(optional.get(), result.get());
            } else {
                StructureWarden.LOGGER.error("Could not parse structure start protector in file: \"" + fileLocation + "\". Skipping entry...\nException: " + parsed.error().get());
            }
        } else {
            StructureWarden.LOGGER.error(structureLocation.toString() + " is not a structure ID in the registry in file: \"" + fileLocation + "\". Skipping entry...");
        }
    }

    public static void serializeProtections(Map<Structure<?>, StructureStartProtection> protections, boolean singleFile) {
        JsonObject object = new JsonObject();

        if (singleFile) {
            Map<Structure<?>, StructureStartProtection> structureObjectTreeMap = new TreeMap<>(Comparator.comparing(Registry.STRUCTURE_FEATURE::getKey));
            structureObjectTreeMap.putAll(protections);
            protections = structureObjectTreeMap;
        }

        for (Map.Entry<Structure<?>, StructureStartProtection> entry : protections.entrySet()) {
            ResourceLocation location = Registry.STRUCTURE_FEATURE.getKey(entry.getKey());

            Path configFile = StructureWarden.CONFIG_PATH.resolve(location.toString().replace(":", "-") + ".json");
            JsonElement jsonElement = StructureStartProtection.CONFIG_CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).result().get();

            if (!singleFile) {
                try {
                    Files.createDirectories(configFile.getParent());
                    Files.write(configFile, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(jsonElement).getBytes());
                } catch (IOException e) {
                    StructureWarden.LOGGER.error(e.toString());
                }
            } else {
                object.add(location.toString(), jsonElement);
            }
        }
        try {
            Path configTarget = StructureWarden.CONFIG_PATH.resolve("structureprotector.json");
            Files.createDirectories(configTarget.getParent());
            Files.write(configTarget, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(object).getBytes());
        } catch (IOException e) {
            StructureWarden.LOGGER.error(e.toString());
        }
    }
}
