package corgitaco.structurewarden.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.structurewarden.StructureWarden;
import corgitaco.structurewarden.configuration.condition.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.*;

public class StructureStartProtection {

    public static final Map<Structure<?>, StructureStartProtection> DEFAULTS = Util.make(new Object2ObjectArrayMap<>(), (map) -> {
        map.put(Structure.VILLAGE, new StructureStartProtection(new EnumMap<>(ActionType.class), Util.make(new ArrayList<>(), (list) -> {
            list.add(new AdvancementCondition(Util.make(new HashSet<>(), (set) -> {
                set.add(new ResourceLocation("adventure/hero_of_the_village"));
            })));
            list.add(new TimeCondition(false, 12000, 15000));
        }), true));

        map.put(Structure.MINESHAFT, new StructureStartProtection(Util.make(new EnumMap<>(ActionType.class), (actionTypeConditions) -> {
            actionTypeConditions.put(ActionType.CONTAINER_OPEN, new ConditionContext(Util.make(new ArrayList<>(), (list) -> {
                list.add(new EntityTypeKillCondition(false, Util.make(new Object2ObjectArrayMap<>(), (typeKillMap) -> {
                    typeKillMap.put(EntityType.CAVE_SPIDER, new EntityTypeKillCondition.KillsTracker(15, 20));
                })));
            }), 0, true));

            actionTypeConditions.put(ActionType.BLOCK_BREAK, new ConditionContext(Util.make(new ArrayList<>(), (list) -> {
                list.add(new EntityTypeKillCondition(false, Util.make(new Object2ObjectArrayMap<>(), (typeKillMap) -> {
                    typeKillMap.put(EntityType.CAVE_SPIDER, new EntityTypeKillCondition.KillsTracker(25, 35));
                })));
            }), 0, true));

            actionTypeConditions.put(ActionType.BLOCK_PLACE, new ConditionContext(Util.make(new ArrayList<>(), (list) -> {
                list.add(new EntityTypeKillCondition(false, Util.make(new Object2ObjectArrayMap<>(), (typeKillMap) -> {
                    typeKillMap.put(EntityType.CAVE_SPIDER, new EntityTypeKillCondition.KillsTracker(45, 55));
                })));
            }), 0, true));
        }), new ArrayList<>(), true));

        map.put(Structure.OCEAN_MONUMENT, new StructureStartProtection(Util.make(new EnumMap<>(ActionType.class), (actionTypeConditions) -> {
            actionTypeConditions.put(ActionType.CONTAINER_OPEN, new ConditionContext(Util.make(new ArrayList<>(), (list) -> {
                list.add(new EntityTypeKillCondition(false, Util.make(new Object2ObjectArrayMap<>(), (typeKillMap) -> {
                    typeKillMap.put(EntityType.GUARDIAN, new EntityTypeKillCondition.KillsTracker(15, 20));
                })));
            }), 0, true));

            actionTypeConditions.put(ActionType.BLOCK_BREAK, new ConditionContext(Util.make(new ArrayList<>(), (list) -> {
                list.add(new EntityTypeKillCondition(false, Util.make(new Object2ObjectArrayMap<>(), (typeKillMap) -> {
                    typeKillMap.put(EntityType.GUARDIAN, new EntityTypeKillCondition.KillsTracker(25, 35));
                })));
            }), 0, true));

            actionTypeConditions.put(ActionType.BLOCK_PLACE, new ConditionContext(Util.make(new ArrayList<>(), (list) -> {
                list.add(new EntityTypeKillCondition(false, Util.make(new Object2ObjectArrayMap<>(), (typeKillMap) -> {
                    typeKillMap.put(EntityType.GUARDIAN, new EntityTypeKillCondition.KillsTracker(45, 55));
                })));
            }), 0, true));
        }), new ArrayList<>(), true));

        map.put(Structure.PILLAGER_OUTPOST, new StructureStartProtection(Util.make(new EnumMap<>(ActionType.class), (actionTypeConditions) -> {
            actionTypeConditions.put(ActionType.CONTAINER_OPEN, new ConditionContext(Util.make(new ArrayList<>(), (list) -> {
                list.add(new EntityTypeKillCondition(false, Util.make(new Object2ObjectArrayMap<>(), (typeKillMap) -> {
                    typeKillMap.put(EntityType.PILLAGER, new EntityTypeKillCondition.KillsTracker(2, 6));
                })));
            }), 0, true));

            actionTypeConditions.put(ActionType.BLOCK_BREAK, new ConditionContext(Util.make(new ArrayList<>(), (list) -> {
                list.add(new EntityTypeKillCondition(false, Util.make(new Object2ObjectArrayMap<>(), (typeKillMap) -> {
                    typeKillMap.put(EntityType.PILLAGER, new EntityTypeKillCondition.KillsTracker(7, 10));
                })));
            }), 0, true));

            actionTypeConditions.put(ActionType.BLOCK_PLACE, new ConditionContext(Util.make(new ArrayList<>(), (list) -> {
                list.add(new EntityTypeKillCondition(false, Util.make(new Object2ObjectArrayMap<>(), (typeKillMap) -> {
                    typeKillMap.put(EntityType.PILLAGER, new EntityTypeKillCondition.KillsTracker(11, 15));
                })));
            }), 0, true));
        }), new ArrayList<>(), true));
    });

    public static final Codec<StructureStartProtection> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.unboundedMap(ActionType.CODEC, ConditionContext.CONFIG_CODEC).fieldOf("typeConditions").orElse(new Object2ObjectArrayMap<>()).forGetter((structureStartProtection) -> {
            return structureStartProtection.typeToConditionContext;
        }), Codec.list(Condition.REGISTRY_CONFIG_CODEC).fieldOf("conditions").orElse(new ArrayList<>()).forGetter((structureStartProtection) -> {
            return structureStartProtection.globalConditions;
        }), Codec.BOOL.fieldOf("usePieceBounds").forGetter((structureStartProtection) -> {
            return structureStartProtection.usePieceBounds;
        })).apply(builder, StructureStartProtection::new);
    });

    public static final Codec<StructureStartProtection> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.unboundedMap(ActionType.CODEC, ConditionContext.DISK_CODEC).fieldOf("typeConditions").orElse(new Object2ObjectArrayMap<>()).forGetter((structureStartProtection) -> {
            return structureStartProtection.typeToConditionContext;
        }), Codec.list(Condition.REGISTRY_DISK_CODEC).fieldOf("conditions").forGetter((structureStartProtection) -> {
            return structureStartProtection.globalConditions;
        }), Codec.BOOL.fieldOf("usePieceBounds").forGetter((structureStartProtection) -> {
            return structureStartProtection.usePieceBounds;
        })).apply(builder, StructureStartProtection::new);
    });

    private final Map<ActionType, ConditionContext> typeToConditionContext = new EnumMap<>(ActionType.class);
    private final boolean usePieceBounds;
    private final List<Condition> globalConditions;

    public StructureStartProtection(Map<ActionType, ConditionContext> typeToConditionContext, List<Condition> globalConditions, boolean usePieceBounds) {
        this.globalConditions = globalConditions;
        this.typeToConditionContext.putAll(typeToConditionContext);
        this.usePieceBounds = usePieceBounds;
    }

    long lastMsgTime;

    public boolean conditionsMet(ServerPlayerEntity playerEntity, ServerWorld world, StructureStart<?> structureStart, BlockPos target, ActionType type) {
        if (playerEntity.isCreative()) {
            return true;
        }

        ArrayList<TranslationTextComponent> components = new ArrayList<>();
        int globalConditionHits = 0;

        @Nullable
        MutableBoundingBox box = usePieceBounds ? getIntersectingPieceBoxOnPlayer(playerEntity, structureStart) : structureStart.getBoundingBox();

        // We're not inside a piece here.
        if (box == null) {
            return true;
        }

        for (Condition globalCondition : this.globalConditions) {
            if (globalCondition.checkIfPasses(playerEntity, world, structureStart, box, target, type, components)) {
                globalConditionHits++;
            }
        }

        @Nullable
        ConditionContext conditionContext = this.typeToConditionContext.get(type);

        if (conditionContext != null) {
            int conditionHits = 0;
            for (Condition condition : conditionContext.conditions) {
                if (conditionContext.requiredPassedConditions > 0 && conditionHits + (conditionContext.accountGlobalPassedConditions ? globalConditionHits : 0) == conditionContext.requiredPassedConditions) {
                    return true;
                }

                if (condition.checkIfPasses(playerEntity, world, structureStart, box, target, type, components)) {
                    conditionHits++;
                }
            }
        }

        boolean empty = components.isEmpty();

        if (!empty) {
            printMissingConditions(playerEntity, world, type, components);
        }
        return empty;
    }

    private void printMissingConditions(ServerPlayerEntity playerEntity, ServerWorld world, ActionType type, ArrayList<TranslationTextComponent> components) {
        long gameTime = world.getGameTime();
        if (gameTime - lastMsgTime >= 20) {
            playerEntity.displayClientMessage(new TranslationTextComponent(StructureWarden.MOD_ID + ".condition.missing", type.getActionTranslationComponent()), false);
            for (TranslationTextComponent component : components) {
                playerEntity.displayClientMessage(component, false);
            }
            lastMsgTime = gameTime;
        }
    }

    @Nullable
    public static MutableBoundingBox getIntersectingPieceBoxOnPlayer(ServerPlayerEntity playerEntity, StructureStart<?> structureStart) {
        for (StructurePiece piece : structureStart.getPieces()) {
            MutableBoundingBox pieceBoundingBox = piece.getBoundingBox();
            if (pieceBoundingBox.isInside(playerEntity.blockPosition())) {
                return pieceBoundingBox;
            }
        }
        return null;
    }

    public void onEntityDeath(LivingEntity entity, ServerWorld world, StructureStart<?> structureStart) {
        if (usePieceBounds) {
            for (Condition condition : globalConditions) {
                for (StructurePiece piece : structureStart.getPieces()) {
                    condition.onEntityDeath(entity, world, structureStart, piece.getBoundingBox());
                }
            }
        } else {
            for (Condition condition : globalConditions) {
                condition.onEntityDeath(entity, world, structureStart, structureStart.getBoundingBox());
            }
        }


        for (Map.Entry<ActionType, ConditionContext> conditionTypeConditionContextEntry : typeToConditionContext.entrySet()) {
            ConditionContext conditionContext = conditionTypeConditionContextEntry.getValue();
            if (usePieceBounds) {
                for (StructurePiece piece : structureStart.getPieces()) {
                    for (Condition condition : conditionContext.conditions) {
                        condition.onEntityDeath(entity, world, structureStart, piece.getBoundingBox());
                    }
                }
            } else {
                for (Condition condition : conditionContext.conditions) {
                    condition.onEntityDeath(entity, world, structureStart, structureStart.getBoundingBox());
                }
            }
        }
    }

    public void playerTick(ServerPlayerEntity player, StructureStart<?> structureStart) {
        if (usePieceBounds) {
            for (Condition condition : globalConditions) {
                for (StructurePiece piece : structureStart.getPieces()) {
                    condition.playerTick(player, structureStart, piece.getBoundingBox());
                }
            }
        } else {
            for (Condition condition : globalConditions) {
                condition.playerTick(player, structureStart, structureStart.getBoundingBox());
            }
        }

        for (Map.Entry<ActionType, ConditionContext> conditionTypeConditionContextEntry : typeToConditionContext.entrySet()) {
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
            }), Codec.BOOL.fieldOf("accountGlobalPassedConditions").forGetter((structureStartProtection) -> {
                return structureStartProtection.accountGlobalPassedConditions;
            })).apply(builder, ConditionContext::new);
        });

        public static final Codec<ConditionContext> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
            return builder.group(Codec.list(Condition.REGISTRY_DISK_CODEC).fieldOf("conditions").forGetter((structureStartProtection) -> {
                return structureStartProtection.conditions;
            }), Codec.INT.fieldOf("requiredPassedConditions").forGetter((structureStartProtection) -> {
                return structureStartProtection.requiredPassedConditions;
            }), Codec.BOOL.fieldOf("accountGlobalPassedConditions").forGetter((structureStartProtection) -> {
                return structureStartProtection.accountGlobalPassedConditions;
            })).apply(builder, ConditionContext::new);
        });

        private final List<Condition> conditions;
        private final int requiredPassedConditions;
        private final boolean accountGlobalPassedConditions;

        public ConditionContext(List<Condition> conditions, int requiredPassedConditions, boolean accountGlobalPassedConditions) {
            this.conditions = conditions;
            this.requiredPassedConditions = Math.min(requiredPassedConditions, conditions.size());
            this.accountGlobalPassedConditions = accountGlobalPassedConditions;
        }
    }
}
