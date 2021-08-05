package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.modid.Main;
import corgitaco.modid.mixin.access.StructureStartAccess;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
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
        }), Codec.unboundedMap(Codec.STRING, KillsLeftTracker.CONFIG_CODEC).fieldOf("killsLeft").forGetter((killCondition) -> {
            Map<String, KillsLeftTracker> serializable = new Object2ObjectArrayMap<>();
            killCondition.killsLeft.forEach((o, killsLeftTracker) -> {
                if (o instanceof EntityClassification) {
                    serializable.put("category/" + o.toString(), killsLeftTracker);
                } else if (o instanceof EntityType) {
                    serializable.put(Registry.ENTITY_TYPE.getKey((EntityType<?>) o).toString(), killsLeftTracker);
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
        }), Codec.unboundedMap(Codec.STRING, KillsLeftTracker.DISK_CODEC).fieldOf("minKillsLeft").forGetter((killCondition) -> {
            Map<String, KillsLeftTracker> serializable = new Object2ObjectArrayMap<>();
            killCondition.killsLeft.forEach((o, killsLeftTracker) -> {
                if (o instanceof EntityClassification) {
                    serializable.put("category/" + o.toString(), killsLeftTracker);
                } else if (o instanceof EntityType) {
                    serializable.put(Registry.ENTITY_TYPE.getKey((EntityType<?>) o).toString(), killsLeftTracker);
                } else {
                    throw new IllegalArgumentException("Illegal kill key type class: " + o.getClass().getSimpleName());
                }
            });
            return serializable;
        }), Codec.unboundedMap(Codec.INT, Codec.unboundedMap(Codec.STRING, KillsLeftTracker.DISK_CODEC)).fieldOf("killsByPlayer").forGetter((killCondition) -> {
            Map<Integer, Map<String, KillsLeftTracker>> serializable = new Int2ObjectArrayMap<>();
            killCondition.killsLeftByPlayer.forEach((player, kills) -> {
                kills.forEach(((o, killsLeftTracker) -> {
                    if (o instanceof EntityClassification) {
                        serializable.computeIfAbsent(player.intValue(), (player1) -> new Object2ObjectArrayMap<>()).put("category/" + o.toString(), killsLeftTracker);
                    } else if (o instanceof EntityType) {
                        serializable.computeIfAbsent(player.intValue(), (player1) -> new Object2ObjectArrayMap<>()).put(Registry.ENTITY_TYPE.getKey((EntityType<?>) o).toString(), killsLeftTracker);
                    } else {
                        throw new IllegalArgumentException("Illegal kill key type class: " + o.getClass().getSimpleName());
                    }
                }));
            });
            return serializable;
        })).apply(builder, EntityTypeKillCondition::new);
    });


    private final Object2ObjectArrayMap<Object, KillsLeftTracker> killsLeft = new Object2ObjectArrayMap<>();
    private final Int2ObjectArrayMap<Object2ObjectArrayMap<Object, KillsLeftTracker>> killsLeftByPlayer = new Int2ObjectArrayMap<>();

    //Config
    protected EntityTypeKillCondition(boolean perPlayer, Map<String, KillsLeftTracker> killsLeft) {
        this(perPlayer, killsLeft, new Object2ObjectArrayMap<>());
    }

    public EntityTypeKillCondition(boolean perPlayer, Object2ObjectArrayMap<Object, KillsLeftTracker> killsLeft) {
        super(perPlayer);
        this.killsLeft.putAll(killsLeft);
    }


    // Disk
    private EntityTypeKillCondition(boolean perPlayer, Map<String, KillsLeftTracker> killsLeft, Map<Integer, Map<String, KillsLeftTracker>> killsLeftByPlayer) {
        super(perPlayer);
        Map<EntityClassification, List<EntityType<?>>> mobCategoryEntityTypes = new EnumMap<>(EntityClassification.class);

        for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
            mobCategoryEntityTypes.computeIfAbsent(entityType.getCategory(), (mobCategory -> new ArrayList<>())).add(entityType);
        }

        killsLeft.forEach((s, killsLeftTracker) -> {
            this.killsLeft.put(type(s), killsLeftTracker);
        });

        killsLeftByPlayer.forEach((uuid, stringKillsLeftTrackerMap) -> {
            stringKillsLeftTrackerMap.forEach(((s, killsLeftTracker) -> {
                this.killsLeftByPlayer.computeIfAbsent(uuid.intValue(), (uuid1) -> new Object2ObjectArrayMap<>()).put(type(s), killsLeftTracker);
            }));
        });
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
                KillsLeftTracker killsLeftTracker = this.killsLeft.get(dyingType);
                setDefaultsAndUpdate((StructureStartAccess) structureStart, killsLeftTracker);

            } else if (this.killsLeft.containsKey(dyingType.getCategory())) {
                KillsLeftTracker killsLeftTracker = this.killsLeft.get(dyingType.getCategory());
                setDefaultsAndUpdate((StructureStartAccess) structureStart, killsLeftTracker);
            }


            int playerUUIDHash = killCredit.getUUID().hashCode();


            Object2ObjectArrayMap<Object, KillsLeftTracker> playerKillsLeft = this.killsLeftByPlayer.computeIfAbsent(playerUUIDHash, (uuid -> {
                Object2ObjectArrayMap<Object, KillsLeftTracker> playerTracker = new Object2ObjectArrayMap<>();

                this.killsLeft.forEach((condition, killsLeftTracker) -> {
                    playerTracker.put(condition, new KillsLeftTracker(killsLeftTracker.getMinKillsLeft(), killsLeftTracker.getMaxKillsLeft(), killsLeftTracker.getKillsLeftDefault(), killsLeftTracker.getKillsLeftDefault()));
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

    private void setDefaultsAndUpdate(StructureStartAccess structureStart, KillsLeftTracker killsLeftTracker) {
        setDefaultsAndUpdate(structureStart, killsLeftTracker, true);
    }


        private void setDefaultsAndUpdate(StructureStartAccess structureStart, KillsLeftTracker killsLeftTracker, boolean update) {
        if (killsLeftTracker.getKillsLeftDefault() == -1) {
            killsLeftTracker.setKillsLeftDefault(structureStart.getRandom().nextInt(killsLeftTracker.getMaxKillsLeft() - killsLeftTracker.getMinKillsLeft() + 1) + killsLeftTracker.getMinKillsLeft());
        }

        if (killsLeftTracker.getKillsLeft() == -1) {
            killsLeftTracker.setKillsLeft(killsLeftTracker.getKillsLeftDefault());
        }

        if (update) {
            killsLeftTracker.setKillsLeft(killsLeftTracker.getKillsLeft() - 1);
        }
    }

    @Override
    public boolean checkIfPasses(ServerPlayerEntity playerEntity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box, BlockPos target) {
        if (box.isInside(target)) {
            if (!isPerPlayer()) {
                for (Map.Entry<Object, KillsLeftTracker> entry : this.killsLeft.entrySet()) {
                    if (entry.getValue().getKillsLeft() > 0) {
                        return false;
                    }
                }
            } else {
                Object2ObjectArrayMap<Object, KillsLeftTracker> killsLeftByPlayer = this.killsLeftByPlayer.computeIfAbsent(playerEntity.getUUID().hashCode(), (uuid) -> {
                    Object2ObjectArrayMap<Object, KillsLeftTracker> playerTracker = new Object2ObjectArrayMap<>();
                    this.killsLeft.forEach((condition, killsLeftTracker) -> {
                        KillsLeftTracker killsLeftTracker1 = new KillsLeftTracker(killsLeftTracker.getMinKillsLeft(), killsLeftTracker.getMaxKillsLeft(), killsLeftTracker.getKillsLeftDefault(), killsLeftTracker.getKillsLeftDefault());
                        playerTracker.put(condition, killsLeftTracker1);
                        setDefaultsAndUpdate((StructureStartAccess) structureStart, killsLeftTracker1, false);
                    });
                    return playerTracker;
                });


                for (Map.Entry<Object, KillsLeftTracker> entry1 : killsLeftByPlayer.entrySet()) {
                    if (entry1.getValue().getKillsLeft() > 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public TranslationTextComponent textComponent() {
        return new TranslationTextComponent("You still need to kill: %s mobs to build/destroy blocks here...", this.killsLeft);
    }

    public static class KillsLeftTracker {

        public static Codec<KillsLeftTracker> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
            return builder.group(Codec.INT.fieldOf("minKillsLeft").forGetter((killCondition) -> {
                return killCondition.minKillsLeft;
            }), Codec.INT.fieldOf("maxKillsLeft").forGetter((killCondition) -> {
                return killCondition.maxKillsLeft;
            })).apply(builder, KillsLeftTracker::new);
        });

        public static final Codec<KillsLeftTracker> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
            return builder.group(Codec.INT.fieldOf("minKillsLeft").forGetter((killCondition) -> {
                return killCondition.minKillsLeft;
            }), Codec.INT.fieldOf("maxKillsLeft").forGetter((killCondition) -> {
                return killCondition.maxKillsLeft;
            }), Codec.INT.fieldOf("killsLeftDefault").forGetter((killCondition) -> {
                return killCondition.killsLeftDefault;
            }), Codec.INT.fieldOf("killsLeft").forGetter((killCondition) -> {
                return killCondition.killsLeft;
            })).apply(builder, KillsLeftTracker::new);
        });

        private final int minKillsLeft;
        private final int maxKillsLeft;
        private int killsLeft;
        private int killsLeftDefault;

        public KillsLeftTracker(int minKillsLeft, int maxKillsLeft) {
            this(minKillsLeft, maxKillsLeft, -1, -1);
        }

        private KillsLeftTracker(int minKillsLeft, int maxKillsLeft, int killsLeft, int killsLeftDefault) {
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
