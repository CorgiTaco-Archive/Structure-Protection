package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.modid.Main;
import corgitaco.modid.mixin.access.StructureStartAccess;
import corgitaco.modid.util.UUIDStringCodec;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TimeCondition extends Condition {

    public static final Codec<TimeCondition> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.BOOL.fieldOf("perPlayer").forGetter((killCondition) -> {
            return killCondition.isPerPlayer();
        }), Codec.INT.fieldOf("minTimeInTicks").forGetter((killCondition) -> {
            return killCondition.minTimeInTicks;
        }), Codec.INT.fieldOf("maxTimeInTicks").forGetter((killCondition) -> {
            return killCondition.maxTimeInTicks;
        })).apply(builder, TimeCondition::new);
    });

    public static final Codec<TimeCondition> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.BOOL.fieldOf("perPlayer").forGetter((killCondition) -> {
            return killCondition.isPerPlayer();
        }), Codec.INT.fieldOf("minTimeInTicks").forGetter((killCondition) -> {
            return killCondition.minTimeInTicks;
        }), Codec.INT.fieldOf("maxTimeInTicks").forGetter((killCondition) -> {
            return killCondition.maxTimeInTicks;
        }), Codec.unboundedMap(UUIDStringCodec.CODEC, Codec.INT).fieldOf("playerTimeLeftInTicks").forGetter((killCondition) -> {
            return killCondition.playerTimeLeftInTicks;
        }), Codec.INT.fieldOf("defaultTimeLeftInTicks").forGetter((killCondition) -> {
            return killCondition.defaultTimeLeftInTicks;
        }), Codec.INT.fieldOf("timeLeft").forGetter((killCondition) -> {
            return killCondition.timeLeft;
        })).apply(builder, TimeCondition::new);
    });

    private final int minTimeInTicks;
    private final int maxTimeInTicks;
    private final Object2IntArrayMap<UUID> playerTimeLeftInTicks = new Object2IntArrayMap<>();
    private int defaultTimeLeftInTicks;
    private int timeLeft;


    // Config
    public TimeCondition(boolean perPlayer, int minTimeInTicks, int maxTimeInTicks) {
        this(perPlayer, minTimeInTicks, maxTimeInTicks, new Object2IntArrayMap<>(), -1, -1);
    }

    // Disk
    private TimeCondition(boolean perPlayer, int minTimeInTicks, int maxTimeInTicks, Map<UUID, Integer> playerTimeLeftInTicks, int defaultTimeLeftInTicks, int timeLeft) {
        super(perPlayer);
        this.minTimeInTicks = minTimeInTicks;
        this.maxTimeInTicks = maxTimeInTicks;
        this.timeLeft = timeLeft;
        this.playerTimeLeftInTicks.putAll(playerTimeLeftInTicks);
        this.defaultTimeLeftInTicks = defaultTimeLeftInTicks;
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
    public boolean checkIfPasses(ServerPlayerEntity entity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box, BlockPos target, ActionType type, List<TranslationTextComponent> requirements) {
        if (box.isInside(target)) {

            if (isPerPlayer()) {
                UUID uuid = entity.getUUID();
                int timeLeft = this.playerTimeLeftInTicks.computeIfAbsent(uuid, (uuid1) -> {
                    if (defaultTimeLeftInTicks == -1) {
                        defaultTimeLeftInTicks = ((StructureStartAccess) structureStart).getRandom().nextInt(this.maxTimeInTicks - this.minTimeInTicks + 1) + this.minTimeInTicks;
                    }
                    return defaultTimeLeftInTicks;
                });
                boolean passes = timeLeft <= 0;

                if (!passes) {
                    requirements.add(new TranslationTextComponent(Main.MOD_ID + ".condition.time", String.format("%.2f", (double) timeLeft / 24000)));
                }
                return passes;
            } else {
                if (this.timeLeft == -1) {
                    if (defaultTimeLeftInTicks == -1) {
                        defaultTimeLeftInTicks = ((StructureStartAccess) structureStart).getRandom().nextInt(this.maxTimeInTicks - this.minTimeInTicks + 1) + this.minTimeInTicks;
                    }
                    this.timeLeft = defaultTimeLeftInTicks;
                }

                boolean passes = this.timeLeft <= 0;

                if (!passes) {
                    requirements.add(new TranslationTextComponent(Main.MOD_ID + ".condition.time", String.format("%.2f", (double) timeLeft / 24000D)));
                }
                return passes;
            }
        }
        return true;
    }

    private long lastGameTime = 0L;

    @Override
    public void playerTick(ServerPlayerEntity player, StructureStart<?> structureStart, MutableBoundingBox box) {
        long gameTime = player.getLevel().getGameTime();
        if (lastGameTime != gameTime && timeLeft > 0) {
            this.timeLeft--;
            lastGameTime = gameTime;
        }

        UUID uuid = player.getUUID();
        int lastTime = this.playerTimeLeftInTicks.computeIfAbsent(uuid, (uuid1) -> {
            if (defaultTimeLeftInTicks == -1) {
                defaultTimeLeftInTicks = ((StructureStartAccess) structureStart).getRandom().nextInt(this.maxTimeInTicks - this.minTimeInTicks + 1) + this.minTimeInTicks + 1;
            }
            return defaultTimeLeftInTicks;
        });
        if (lastTime > 0) {
            this.playerTimeLeftInTicks.put(uuid, lastTime - 1);
        }
    }
}
