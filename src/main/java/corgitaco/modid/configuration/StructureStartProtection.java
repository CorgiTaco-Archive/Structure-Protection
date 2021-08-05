package corgitaco.modid.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.modid.configuration.condition.AdvancementCondition;
import corgitaco.modid.configuration.condition.Condition;
import corgitaco.modid.configuration.condition.EntityTypeKillCondition;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class StructureStartProtection {

    public static final Map<Structure<?>, StructureStartProtection> DEFAULTS = Util.make(new Object2ObjectArrayMap<>(), (map) -> {
        map.put(Structure.VILLAGE, new StructureStartProtection(Util.make(new ArrayList<>(), (list) -> {
            list.add(new EntityTypeKillCondition(true, Util.make(new Object2ObjectArrayMap<>(), (map1) -> {
                map1.put(EntityClassification.MONSTER, new EntityTypeKillCondition.KillsLeftTracker(5, 25));
            })));
            list.add(new AdvancementCondition(Util.make(new HashSet<>(), (set) -> {
                set.add(new ResourceLocation("adventure/hero_of_the_village"));
            })));
        }), true));
    });

    public static final Codec<StructureStartProtection> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.list(Condition.REGISTRY_CONFIG_CODEC).fieldOf("conditions").forGetter((structureStartProtection) -> {
            return structureStartProtection.conditionList;
        }), Codec.BOOL.fieldOf("usePieceBounds").forGetter((structureStartProtection) -> {
            return structureStartProtection.usePieceBounds;
        })).apply(builder, StructureStartProtection::new);
    });

    public static final Codec<StructureStartProtection> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.list(Condition.REGISTRY_DISK_CODEC).fieldOf("conditions").forGetter((structureStartProtection) -> {
            return structureStartProtection.conditionList;
        }), Codec.BOOL.fieldOf("usePieceBounds").forGetter((structureStartProtection) -> {
            return structureStartProtection.usePieceBounds;
        })).apply(builder, StructureStartProtection::new);
    });

    private final List<Condition> conditionList;
    private final boolean usePieceBounds;

    public StructureStartProtection(List<Condition> conditionList, boolean usePieceBounds) {
        this.conditionList = conditionList;
        this.usePieceBounds = usePieceBounds;
    }

    public boolean conditionsMet(ServerPlayerEntity playerEntity, ServerWorld world, StructureStart<?> structureStart, BlockPos target) {
        if (usePieceBounds) {
            for (StructurePiece piece : structureStart.getPieces()) {
                for (Condition condition : this.conditionList) {
                    if (!condition.checkIfPasses(playerEntity, world, structureStart, piece.getBoundingBox(), target)) {
                        return false;
                    }
                }
            }
        } else {
            for (Condition condition : this.conditionList) {
                if (!condition.checkIfPasses(playerEntity, world, structureStart, structureStart.getBoundingBox(), target)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void onEntityDeath(LivingEntity entity, ServerWorld world, StructureStart<?> structureStart) {
        if (usePieceBounds) {
            for (StructurePiece piece : structureStart.getPieces()) {
                for (Condition condition : this.conditionList) {
                    condition.onEntityDie(entity, world, structureStart, piece.getBoundingBox());
                }
            }
        } else {
            for (Condition condition : this.conditionList) {
                condition.onEntityDie(entity, world, structureStart, structureStart.getBoundingBox());
            }
        }
    }

    public List<Condition> getConditionList() {
        return conditionList;
    }
}
