package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.modid.mixin.access.StructureStartAccess;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.Map;

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
        }), Codec.unboundedMap(Codec.INT, Codec.INT).fieldOf("playerTimeLeftInTicks").forGetter((killCondition) -> {
            return killCondition.playerTimeLeftInTicks;
        }), Codec.INT.fieldOf("defaultTimeLeftInTicks").forGetter((killCondition) -> {
            return killCondition.defaultTimeLeftInTicks;
        }), Codec.INT.fieldOf("timeLeft").forGetter((killCondition) -> {
            return killCondition.timeLeft;
        })).apply(builder, TimeCondition::new);
    });

    private final int minTimeInTicks;
    private final int maxTimeInTicks;
    private final Int2IntArrayMap playerTimeLeftInTicks = new Int2IntArrayMap();
    private int defaultTimeLeftInTicks;
    private int timeLeft;


    // Config
    public TimeCondition(boolean perPlayer, int minTimeInTicks, int maxTimeInTicks) {
        this(perPlayer, minTimeInTicks, maxTimeInTicks, new Int2IntArrayMap(), -1, -1);
    }

    // Disk
    private TimeCondition(boolean perPlayer, int minTimeInTicks, int maxTimeInTicks, Map<Integer, Integer> playerTimeLeftInTicks, int defaultTimeLeftInTicks, int timeLeft) {
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
    public boolean checkIfPasses(ServerPlayerEntity entity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box, BlockPos target) {
        if (box.isInside(target)) {

            if (isPerPlayer()) {
                int uuidHash = entity.getUUID().hashCode();
                int timeLeft = this.playerTimeLeftInTicks.computeIfAbsent(uuidHash, (uuid) -> {
                    if (defaultTimeLeftInTicks == -1) {
                        defaultTimeLeftInTicks = ((StructureStartAccess) structureStart).getRandom().nextInt(this.maxTimeInTicks - this.minTimeInTicks + 1) + this.minTimeInTicks;
                    }
                    return defaultTimeLeftInTicks;
                });
                return timeLeft == 0;
            } else {
                return this.timeLeft == 0;
            }
        }
        return true;
    }

    private long lastGameTime = 0L;

    @Override
    public void playerTick(ServerPlayerEntity player, StructureStart<?> structureStart, MutableBoundingBox box) {
        long gameTime = player.getLevel().getGameTime();
        if (lastGameTime != gameTime) {
            this.timeLeft--;
            lastGameTime = gameTime;
        }

        int uuidHash = player.getUUID().hashCode();
        int lastTime = this.playerTimeLeftInTicks.computeIfAbsent(uuidHash, (uuid) -> {
            if (defaultTimeLeftInTicks == -1) {
                defaultTimeLeftInTicks = ((StructureStartAccess) structureStart).getRandom().nextInt(this.maxTimeInTicks - this.minTimeInTicks + 1) + this.minTimeInTicks + 1;
            }
            return defaultTimeLeftInTicks;
        });
        if (lastTime > 0) {
            this.playerTimeLeftInTicks.put(uuidHash, lastTime - 1);
        }
    }

    @Override
    public TranslationTextComponent textComponent() {
        return null;
    }
}
