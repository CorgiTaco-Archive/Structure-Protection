package corgitaco.modid.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.modid.configuration.condition.Condition;
import corgitaco.modid.configuration.condition.EntityTypeKillCondition;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StructureStartProtection {

    public static final Map<Structure<?>, StructureStartProtection> DEFAULTS = Util.make(new Object2ObjectArrayMap<>(), (map) -> {
        map.put(Structure.VILLAGE, new StructureStartProtection(Util.make(new ArrayList<>(), (list) -> {
            list.add(new EntityTypeKillCondition(true, Util.make(new Object2ObjectArrayMap<>(), (map1) -> {
                map1.put(EntityClassification.MONSTER, new EntityTypeKillCondition.KillsLeftTracker(5, 25));
            })));
        })));



    });

    public static final Codec<StructureStartProtection> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.list(Condition.REGISTRY_CONFIG_CODEC).fieldOf("conditions").forGetter((structureStartProtection) -> {
            return structureStartProtection.conditionList;
        })).apply(builder, StructureStartProtection::new);
    });

    public static final Codec<StructureStartProtection> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.list(Condition.REGISTRY_DISK_CODEC).fieldOf("conditions").forGetter((structureStartProtection) -> {
            return structureStartProtection.conditionList;
        })).apply(builder, StructureStartProtection::new);
    });

    private final List<Condition> conditionList;

    public StructureStartProtection(List<Condition> conditionList) {
        this.conditionList = conditionList;
    }

    public boolean conditionsMet(ServerPlayerEntity playerEntity, ServerWorld world, StructureStart<?> structureStart) {
        for (Condition condition : this.conditionList) {
            if (!condition.checkIfPasses(playerEntity, world, structureStart)) {
                return false;
            }

        }
        return true;
    }

    public void onEntityDeath(LivingEntity entity, ServerWorld world, StructureStart<?> structureStart) {
        for (Condition condition : this.conditionList) {
            condition.onEntityDie(entity, world, structureStart);
        }
    }

    public List<Condition> getConditionList() {
        return conditionList;
    }
}
