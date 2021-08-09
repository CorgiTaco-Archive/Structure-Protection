package corgitaco.structurewarden.configuration.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.structurewarden.StructureWarden;
import corgitaco.structurewarden.mixin.access.StructureStartAccess;
import corgitaco.structurewarden.util.UUIDStringCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.*;

@SuppressWarnings("ConstantConditions")
public class EntityTypeKillCondition extends Condition {

    public static final Codec<EntityTypeKillCondition> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.BOOL.fieldOf("perPlayer").forGetter((killCondition) -> {
            return killCondition.isPerPlayer();
        }), Codec.unboundedMap(Codec.STRING, KillsTracker.CONFIG_CODEC).fieldOf("killTracker").forGetter((killCondition) -> {
            Map<String, KillsTracker> serializable = new Object2ObjectArrayMap<>();
            killCondition.killsLeft.forEach((o, killsTracker) -> {
                if (o instanceof EntityClassification) {
                    serializable.put("category/" + o.toString(), killsTracker);
                } else if (o instanceof EntityType) {
                    serializable.put(Registry.ENTITY_TYPE.getKey((EntityType<?>) o).toString(), killsTracker);
                } else {
                    throw new IllegalArgumentException("Illegal kill key type class: " + o.getClass().getSimpleName());
                }
            });
            return serializable;
        })).apply(builder, EntityTypeKillCondition::new);
    });

    public static final Codec<EntityTypeKillCondition> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.BOOL.fieldOf("perPlayer").forGetter((killCondition) -> {
            return killCondition.isPerPlayer();
        }), Codec.unboundedMap(Codec.STRING, KillsTracker.DISK_CODEC).fieldOf("killTracker").forGetter((killCondition) -> {
            Map<String, KillsTracker> serializable = new Object2ObjectArrayMap<>();
            killCondition.killsLeft.forEach((o, killsTracker) -> {
                if (o instanceof EntityClassification) {
                    serializable.put("category/" + o.toString(), killsTracker);
                } else if (o instanceof EntityType) {
                    serializable.put(Registry.ENTITY_TYPE.getKey((EntityType<?>) o).toString(), killsTracker);
                } else {
                    throw new IllegalArgumentException("Illegal kill key type class: " + o.getClass().getSimpleName());
                }
            });
            return serializable;
        }), Codec.unboundedMap(UUIDStringCodec.CODEC, Codec.unboundedMap(Codec.STRING, KillsTracker.DISK_CODEC)).fieldOf("playerKillTracker").forGetter((killCondition) -> {
            Map<UUID, Map<String, KillsTracker>> serializable = new HashMap<>();
            killCondition.killsLeftByPlayer.forEach((player, kills) -> {
                kills.forEach(((o, killsTracker) -> {
                    if (o instanceof EntityClassification) {
                        serializable.computeIfAbsent(player, (player1) -> new HashMap<>()).put("category/" + o.toString(), killsTracker);
                    } else if (o instanceof EntityType) {
                        serializable.computeIfAbsent(player, (player1) -> new HashMap<>()).put(Registry.ENTITY_TYPE.getKey((EntityType<?>) o).toString(), killsTracker);
                    } else {
                        throw new IllegalArgumentException("Illegal kill key type class: " + o.getClass().getSimpleName());
                    }
                }));
            });
            return serializable;
        })).apply(builder, EntityTypeKillCondition::new);
    });


    private final Object2ObjectArrayMap<Object, KillsTracker> killsLeft = new Object2ObjectArrayMap<>();
    private final Object2ObjectArrayMap<UUID, Object2ObjectArrayMap<Object, KillsTracker>> killsLeftByPlayer = new Object2ObjectArrayMap<>();
    private final TranslationTextComponent types;

    //Config
    protected EntityTypeKillCondition(boolean perPlayer, Map<String, KillsTracker> killsLeft) {
        this(perPlayer, killsLeft, new Object2ObjectArrayMap<>());
    }

    public EntityTypeKillCondition(boolean perPlayer, Object2ObjectArrayMap<Object, KillsTracker> killsLeft) {
        super(perPlayer);
        this.killsLeft.putAll(killsLeft);
        TranslationTextComponent textComponent = null;

        for (Object object : this.killsLeft.keySet()) {
            if (textComponent == null) {
                textComponent = getComponentForType(object);
            } else {
                textComponent.append(getComponentForType(object));
            }
        }
        this.types = textComponent;
    }


    // Disk
    private EntityTypeKillCondition(boolean perPlayer, Map<String, KillsTracker> killsLeft, Map<UUID, Map<String, KillsTracker>> killsLeftByPlayer) {
        super(perPlayer);
        Map<EntityClassification, List<EntityType<?>>> mobCategoryEntityTypes = new EnumMap<>(EntityClassification.class);

        for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
            mobCategoryEntityTypes.computeIfAbsent(entityType.getCategory(), (mobCategory -> new ArrayList<>())).add(entityType);
        }

        killsLeft.forEach((s, killsTracker) -> {
            this.killsLeft.put(type(s), killsTracker);
        });

        killsLeftByPlayer.forEach((uuid, stringKillsLeftTrackerMap) -> {
            stringKillsLeftTrackerMap.forEach(((s, killsTracker) -> {
                this.killsLeftByPlayer.computeIfAbsent(uuid, (uuid1) -> new Object2ObjectArrayMap<>()).put(type(s), killsTracker);
            }));
        });
        TranslationTextComponent textComponent = null;

        for (Object object : this.killsLeft.keySet()) {
            if (textComponent == null) {
                textComponent = getComponentForType(object);
            } else {
                textComponent.append(", ").append(getComponentForType(object));
            }
        }
        this.types = textComponent;
    }

    public static TranslationTextComponent getComponentForType(Object object) {
        if (object instanceof EntityType<?>) {
            return new TranslationTextComponent(((EntityType<?>) object).getDescriptionId());
        } else if (object instanceof EntityClassification) {
            return CATEGORY_TRANSLATION.get(object);
        }
        throw new IllegalArgumentException("Illegal kill key type class: " + object.getClass().getSimpleName());
    }

    @Nullable
    public static Object type(String key) {
        if (key.startsWith("category/")) {
            String mobCategory = key.substring("category/".length()).toUpperCase();

            EntityClassification[] values = EntityClassification.values();
            if (Arrays.stream(values).noneMatch(difficulty -> difficulty.toString().equals(mobCategory))) {
                StructureWarden.LOGGER.error("\"" + mobCategory + "\" is not a valid mob category value. mob category entry...Valid Mob Categories: " + Arrays.toString(values));
            } else {
                return EntityClassification.valueOf(mobCategory.toUpperCase());
            }
        } else {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(key);

            if (resourceLocation != null) {
                return Registry.ENTITY_TYPE.get(resourceLocation);
            }
        }
        return null;
    }

    @Override
    public Codec<? extends Condition> configCodec() {
        return CONFIG_CODEC;
    }

    @Override
    public Codec<? extends Condition> diskCodec() {
        return DISK_CODEC;
    }

    @Override
    public void onEntityDeath(LivingEntity dyingEntity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box) {
        LivingEntity killCredit = dyingEntity.getKillCredit();
        if (killCredit != null && killCredit instanceof ServerPlayerEntity && box.isInside(killCredit.blockPosition())) {

            EntityType<?> dyingType = dyingEntity.getType();
            if (this.killsLeft.containsKey(dyingType)) {
                KillsTracker killsTracker = this.killsLeft.get(dyingType);
                setDefaultsAndUpdate((StructureStartAccess) structureStart, killsTracker);

            } else if (this.killsLeft.containsKey(dyingType.getCategory())) {
                KillsTracker killsTracker = this.killsLeft.get(dyingType.getCategory());
                setDefaultsAndUpdate((StructureStartAccess) structureStart, killsTracker);
            }


            UUID killCreditUUID = killCredit.getUUID();


            Object2ObjectArrayMap<Object, KillsTracker> playerKillsLeft = this.killsLeftByPlayer.computeIfAbsent(killCreditUUID, (uuid -> {
                Object2ObjectArrayMap<Object, KillsTracker> playerTracker = new Object2ObjectArrayMap<>();

                this.killsLeft.forEach((condition, killsTracker) -> {
                    playerTracker.put(condition, new KillsTracker(killsTracker.getMinKillsLeft(), killsTracker.getMaxKillsLeft(), killsTracker.getKillsLeftDefault(), killsTracker.getKillsLeftDefault()));
                });

                return playerTracker;
            }));

            if (playerKillsLeft.containsKey(dyingType)) {
                setDefaultsAndUpdate((StructureStartAccess) structureStart, playerKillsLeft.get(dyingType));
            } else if (playerKillsLeft.containsKey(dyingType.getCategory())) {
                setDefaultsAndUpdate((StructureStartAccess) structureStart, playerKillsLeft.get(dyingType.getCategory()));
            }
        }
    }

    private void setDefaultsAndUpdate(StructureStartAccess structureStart, KillsTracker killsTracker) {
        setDefaultsAndUpdate(structureStart, killsTracker, true);
    }


    private void setDefaultsAndUpdate(StructureStartAccess structureStart, KillsTracker killsTracker, boolean update) {
        if (killsTracker.getKillsLeftDefault() == -1) {
            killsTracker.setKillsLeftDefault(structureStart.getRandom().nextInt(killsTracker.getMaxKillsLeft() - killsTracker.getMinKillsLeft() + 1) + killsTracker.getMinKillsLeft());
        }

        if (killsTracker.getKillsLeft() == -1) {
            killsTracker.setKillsLeft(killsTracker.getKillsLeftDefault());
        }

        if (update) {
            killsTracker.setKillsLeft(killsTracker.getKillsLeft() - 1);
        }
    }

    @Override
    public boolean checkIfPasses(ServerPlayerEntity playerEntity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box, BlockPos target, ActionType type, List<TranslationTextComponent> remainingRequirements) {
        if (box.isInside(target)) {
            if (!isPerPlayer()) {
                for (Map.Entry<Object, KillsTracker> entry : this.killsLeft.entrySet()) {
                    KillsTracker killsTracker = entry.getValue();
                    setDefaultsAndUpdate((StructureStartAccess) structureStart, killsTracker, false);
                    int killsLeft = killsTracker.getKillsLeft();

                    if (killsLeft > 0) {
                        remainingRequirements.add(new TranslationTextComponent(StructureWarden.MOD_ID + ".condition.entity_type_kill.structurekillsleft", killsLeft, this.types));
                        return false;
                    }
                }
            } else {
                Object2ObjectArrayMap<Object, KillsTracker> killsLeftByPlayer = this.killsLeftByPlayer.computeIfAbsent(playerEntity.getUUID(), (uuid) -> {
                    Object2ObjectArrayMap<Object, KillsTracker> playerTracker = new Object2ObjectArrayMap<>();
                    this.killsLeft.forEach((condition, killsTracker) -> {
                        KillsTracker killsTracker1 = new KillsTracker(killsTracker.getMinKillsLeft(), killsTracker.getMaxKillsLeft(), killsTracker.getKillsLeftDefault(), killsTracker.getKillsLeftDefault());
                        playerTracker.put(condition, killsTracker1);
                        setDefaultsAndUpdate((StructureStartAccess) structureStart, killsTracker1, false);
                    });
                    return playerTracker;
                });


                for (Map.Entry<Object, KillsTracker> entry1 : killsLeftByPlayer.entrySet()) {
                    int killsLeft = entry1.getValue().getKillsLeft();
                    if (killsLeft > 0) {
                        remainingRequirements.add(new TranslationTextComponent(StructureWarden.MOD_ID + ".condition.entity_type_kill.structurekillsleft", killsLeft, this.types));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static class KillsTracker {

        public static Codec<KillsTracker> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
            return builder.group(Codec.INT.fieldOf("minAllowedKills").forGetter((killCondition) -> {
                return killCondition.minKillsLeft;
            }), Codec.INT.fieldOf("maxAllowedKills").forGetter((killCondition) -> {
                return killCondition.maxKillsLeft;
            })).apply(builder, KillsTracker::new);
        });

        public static final Codec<KillsTracker> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
            return builder.group(Codec.INT.fieldOf("minAllowedKills").forGetter((killCondition) -> {
                return killCondition.minKillsLeft;
            }), Codec.INT.fieldOf("maxAllowedKills").forGetter((killCondition) -> {
                return killCondition.maxKillsLeft;
            }), Codec.INT.fieldOf("killsLeft").forGetter((killCondition) -> {
                return killCondition.killsLeft;
            }), Codec.INT.fieldOf("killsLeftDefault").forGetter((killCondition) -> {
                return killCondition.killsLeftDefault;
            })).apply(builder, KillsTracker::new);
        });

        private final int minKillsLeft;
        private final int maxKillsLeft;
        private int killsLeft;
        private int killsLeftDefault;

        public KillsTracker(int minKillsLeft, int maxKillsLeft) {
            this(minKillsLeft, maxKillsLeft, -1, -1);
        }

        private KillsTracker(int minKillsLeft, int maxKillsLeft, int killsLeft, int killsLeftDefault) {
            this.minKillsLeft = minKillsLeft;
            this.maxKillsLeft = maxKillsLeft;
            this.killsLeft = killsLeft;
            this.killsLeftDefault = killsLeftDefault;
        }

        public int getMinKillsLeft() {
            return minKillsLeft;
        }

        public int getMaxKillsLeft() {
            return maxKillsLeft;
        }

        public int getKillsLeft() {
            return killsLeft;
        }

        public void setKillsLeft(int killsLeft) {
            if (killsLeft > 0) {
                this.killsLeft = killsLeft;
            }
        }

        public int getKillsLeftDefault() {
            return killsLeftDefault;
        }

        public void setKillsLeftDefault(int killsLeftDefault) {
            this.killsLeftDefault = killsLeftDefault;
        }
    }
}
