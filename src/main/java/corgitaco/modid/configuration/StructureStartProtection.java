package corgitaco.modid.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.modid.configuration.condition.AdvancementCondition;
import corgitaco.modid.configuration.condition.Condition;
import corgitaco.modid.configuration.condition.ConditionType;
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

import java.util.*;

public class StructureStartProtection {

    public static final Map<Structure<?>, StructureStartProtection> DEFAULTS = Util.make(new Object2ObjectArrayMap<>(), (map) -> {
        map.put(Structure.VILLAGE, new StructureStartProtection(Util.make(new EnumMap<ConditionType, ConditionContext>(ConditionType.class), (map1) -> {

            for (ConditionType value : ConditionType.values()) {
                map1.put(value, new ConditionContext(Util.make(new ArrayList<>(), (list) -> {
                    list.add(new EntityTypeKillCondition(true, Util.make(new Object2ObjectArrayMap<>(), (map2) -> {
                        map2.put(EntityClassification.MONSTER, new EntityTypeKillCondition.KillsTracker(5, 25));
                    })));

                    list.add(new AdvancementCondition(Util.make(new HashSet<>(), (set) -> {
                        set.add(new ResourceLocation("adventure/hero_of_the_village"));
                    })));
                }), 0));
            }
        }), true));
    });

    public static final Codec<StructureStartProtection> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.unboundedMap(ConditionType.CODEC, ConditionContext.CONFIG_CODEC).fieldOf("conditions").forGetter((structureStartProtection) -> {
            return structureStartProtection.typeToConditionContext;
        }), Codec.BOOL.fieldOf("usePieceBounds").forGetter((structureStartProtection) -> {
            return structureStartProtection.usePieceBounds;
        })).apply(builder, StructureStartProtection::new);
    });

    public static final Codec<StructureStartProtection> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.unboundedMap(ConditionType.CODEC, ConditionContext.DISK_CODEC).fieldOf("conditions").forGetter((structureStartProtection) -> {
            return structureStartProtection.typeToConditionContext;
        }), Codec.BOOL.fieldOf("usePieceBounds").forGetter((structureStartProtection) -> {
            return structureStartProtection.usePieceBounds;
        })).apply(builder, StructureStartProtection::new);
    });

    private final Map<ConditionType, ConditionContext> typeToConditionContext = new EnumMap<>(ConditionType.class);
    private final boolean usePieceBounds;

    public StructureStartProtection(Map<ConditionType, ConditionContext> typeToConditionContext, boolean usePieceBounds) {
        this.typeToConditionContext.putAll(typeToConditionContext);
        this.usePieceBounds = usePieceBounds;
    }

    public boolean conditionsMet(ServerPlayerEntity playerEntity, ServerWorld world, StructureStart<?> structureStart, BlockPos target, ConditionType type) {
        int hits = 0;
        ConditionContext conditionContext = this.typeToConditionContext.get(type);
        if (usePieceBounds) {
            for (StructurePiece piece : structureStart.getPieces()) {
                for (Condition condition : conditionContext.conditions) {
                    if (conditionContext.requiredPassedConditions <= 0) {
                        if (!condition.checkIfPasses(playerEntity, world, structureStart, piece.getBoundingBox(), target)) {
                            return false;
                        }
                    } else {
                        if (hits == conditionContext.requiredPassedConditions) {
                            return true;
                        }

                        if (condition.checkIfPasses(playerEntity, world, structureStart, piece.getBoundingBox(), target)) {
                            hits++;
                        }
                    }
                }
            }
        } else {
            for (Condition condition : conditionContext.conditions) {
                if (conditionContext.requiredPassedConditions <= 0) {
                    if (!condition.checkIfPasses(playerEntity, world, structureStart, structureStart.getBoundingBox(), target)) {
                        return false;
                    }
                } else {
                    if (hits == conditionContext.requiredPassedConditions) {
                        return true;
                    }

                    if (condition.checkIfPasses(playerEntity, world, structureStart, structureStart.getBoundingBox(), target)) {
                        hits++;
                    }
                }
            }
        }
        return true;
    }

    public void onEntityDeath(LivingEntity entity, ServerWorld world, StructureStart<?> structureStart) {
        for (Map.Entry<ConditionType, ConditionContext> conditionTypeConditionContextEntry : typeToConditionContext.entrySet()) {
            ConditionContext conditionContext = conditionTypeConditionContextEntry.getValue();
            if (usePieceBounds) {
                for (StructurePiece piece : structureStart.getPieces()) {
                    for (Condition condition : conditionContext.conditions) {
                        condition.onEntityDie(entity, world, structureStart, piece.getBoundingBox());
                    }
                }
            } else {
                for (Condition condition : conditionContext.conditions) {
                    condition.onEntityDie(entity, world, structureStart, structureStart.getBoundingBox());
                }
            }
        }
    }

    public void playerTick(ServerPlayerEntity player, StructureStart<?> structureStart) {
        for (Map.Entry<ConditionType, ConditionContext> conditionTypeConditionContextEntry : typeToConditionContext.entrySet()) {
            ConditionContext conditionContext = conditionTypeConditionContextEntry.getValue();
            if (usePieceBounds) {
                for (StructurePiece piece : structureStart.getPieces()) {
                    for (Condition condition : conditionContext.conditions) {
                        condition.playerTick(player, structureStart, piece.getBoundingBox());
                    }
                }
            } else {
                for (Condition condition : conditionContext.conditions) {
                    condition.playerTick(player, structureStart, structureStart.getBoundingBox());
                }
            }
        }

    }

    public static class ConditionContext {
        public static final Codec<ConditionContext> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
            return builder.group(Codec.list(Condition.REGISTRY_CONFIG_CODEC).fieldOf("conditions").forGetter((structureStartProtection) -> {
                return structureStartProtection.conditions;
            }), Codec.INT.fieldOf("requiredPassedConditions").forGetter((structureStartProtection) -> {
                return structureStartProtection.requiredPassedConditions;
            })).apply(builder, ConditionContext::new);
        });

        public static final Codec<ConditionContext> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
            return builder.group(Codec.list(Condition.REGISTRY_DISK_CODEC).fieldOf("conditions").forGetter((structureStartProtection) -> {
                return structureStartProtection.conditions;
            }), Codec.INT.fieldOf("requiredPassedConditions").forGetter((structureStartProtection) -> {
                return structureStartProtection.requiredPassedConditions;
            })).apply(builder, ConditionContext::new);
        });

        private final List<Condition> conditions;
        private final int requiredPassedConditions;

        public ConditionContext(List<Condition> conditions, int requiredPassedConditions) {
            this.conditions = conditions;
            this.requiredPassedConditions = Math.min(requiredPassedConditions, conditions.size());
        }
    }
}
