package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.modid.Main;
import corgitaco.modid.mixin.access.StructureStartAccess;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
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
        }), Codec.unboundedMap(Codec.INT, Codec.unboundedMap(Codec.STRING, KillsTracker.DISK_CODEC)).fieldOf("playerKillTracker").forGetter((killCondition) -> {
            Map<Integer, Map<String, KillsTracker>> serializable = new Int2ObjectArrayMap<>();
            killCondition.killsLeftByPlayer.forEach((player, kills) -> {
                kills.forEach(((o, killsTracker) -> {
                    if (o instanceof EntityClassification) {
                        serializable.computeIfAbsent(player.intValue(), (player1) -> new Object2ObjectArrayMap<>()).put("category/" + o.toString(), killsTracker);
                    } else if (o instanceof EntityType) {
                        serializable.computeIfAbsent(player.intValue(), (player1) -> new Object2ObjectArrayMap<>()).put(Registry.ENTITY_TYPE.getKey((EntityType<?>) o).toString(), killsTracker);
                    } else {
                        throw new IllegalArgumentException("Illegal kill key type class: " + o.getClass().getSimpleName());
                    }
                }));
            });
            return serializable;
        })).apply(builder, EntityTypeKillCondition::new);
    });


    private final Object2ObjectArrayMap<Object, KillsTracker> killsLeft = new Object2ObjectArrayMap<>();
    private final Int2ObjectArrayMap<Object2ObjectArrayMap<Object, KillsTracker>> killsLeftByPlayer = new Int2ObjectArrayMap<>();
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
    private EntityTypeKillCondition(boolean perPlayer, Map<String, KillsTracker> killsLeft, Map<Integer, Map<String, KillsTracker>> killsLeftByPlayer) {
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
                this.killsLeftByPlayer.computeIfAbsent(uuid.intValue(), (uuid1) -> new Object2ObjectArrayMap<>()).put(type(s), killsTracker);
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
                Main.LOGGER.error("\"" + mobCategory + "\" is not a valid mob category value. mob category entry...Valid Mob Categories: " + Arrays.toString(values));
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

    public void onEntityDie(LivingEntity dyingEntity, ServerWorld serverWorld, StructureStart<?> structureStart) {
        LivingEntity killCredit = dyingEntity.getKillCredit();
        if (dyingEntity instanceof MonsterEntity && killCredit != null && killCredit instanceof ServerPlayerEntity && structureStart.getBoundingBox().isInside(killCredit.blockPosition())) {

            EntityType<?> dyingType = dyingEntity.getType();
            if (this.killsLeft.containsKey(dyingType)) {
                KillsTracker killsTracker = this.killsLeft.get(dyingType);
                setDefaultsAndUpdate((StructureStartAccess) structureStart, killsTracker);

            } else if (this.killsLeft.containsKey(dyingType.getCategory())) {
                KillsTracker killsTracker = this.killsLeft.get(dyingType.getCategory());
                setDefaultsAndUpdate((StructureStartAccess) structureStart, killsTracker);
            }


            int playerUUIDHash = killCredit.getUUID().hashCode();


            Object2ObjectArrayMap<Object, KillsTracker> playerKillsLeft = this.killsLeftByPlayer.computeIfAbsent(playerUUIDHash, (uuid -> {
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
    public boolean checkIfPasses(ServerPlayerEntity playerEntity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box, BlockPos target, ConditionType type, List<TranslationTextComponent> remainingRequirements) {
        if (box.isInside(target)) {
            if (!isPerPlayer()) {
                for (Map.Entry<Object, KillsTracker> entry : this.killsLeft.entrySet()) {
                    int killsLeft = entry.getValue().getKillsLeft();
                    if (killsLeft > 0) {
                        remainingRequirements.add(new TranslationTextComponent(Main.MOD_ID + ".condition.entity_type_kill.structurekillsleft", killsLeft, this.types));
                        return false;
                    }
                }
            } else {
                Object2ObjectArrayMap<Object, KillsTracker> killsLeftByPlayer = this.killsLeftByPlayer.computeIfAbsent(playerEntity.getUUID().hashCode(), (uuid) -> {
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


                        remainingRequirements.add(new TranslationTextComponent(Main.MOD_ID + ".condition.entity_type_kill.playerstructurekillsleft", killsLeft, this.types));
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
            }), Codec.INT.fieldOf("killsLeftDefault").forGetter((killCondition) -> {
                return killCondition.killsLeftDefault;
            }), Codec.INT.fieldOf("killsLeft").forGetter((killCondition) -> {
                return killCondition.killsLeft;
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
            this.killsLeft = killsLeft;
        }

        public int getKillsLeftDefault() {
            return killsLeftDefault;
        }

        public void setKillsLeftDefault(int killsLeftDefault) {
            this.killsLeftDefault = killsLeftDefault;
        }
    }
}
