package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.modid.mixin.access.StructureStartAccess;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class KillCondition extends Condition {

    public static final Codec<KillCondition> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.BOOL.fieldOf("perPlayer").forGetter((killCondition) -> {
            return killCondition.isPerPlayer();
        }), Codec.INT.fieldOf("minKillsLeft").forGetter((killCondition) -> {
            return killCondition.minKillsLeft;
        }), Codec.INT.fieldOf("maxKillsLeft").forGetter((killCondition) -> {
            return killCondition.maxKillsLeft;
        })).apply(builder, KillCondition::new);
    });

    public static final Codec<KillCondition> DISK_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.BOOL.fieldOf("perPlayer").forGetter((killCondition) -> {
            return killCondition.isPerPlayer();
        }), Codec.INT.fieldOf("minKillsLeft").forGetter((killCondition) -> {
            return killCondition.minKillsLeft;
        }), Codec.INT.fieldOf("maxKillsLeft").forGetter((killCondition) -> {
            return killCondition.maxKillsLeft;
        }), Codec.unboundedMap(Codec.INT, Codec.INT).fieldOf("killsByPlayer").forGetter((killCondition) -> {
            return killCondition.killsByPlayer;
        }), Codec.INT.fieldOf("killsLeftDefault").forGetter((killCondition) -> {
            return killCondition.killsLeftDefault;
        }), Codec.INT.fieldOf("killsLeft").forGetter((killCondition) -> {
            return killCondition.killsLeft;
        })).apply(builder, KillCondition::new);
    });


    private final int minKillsLeft;
    private final int maxKillsLeft;
    private final Int2IntArrayMap killsByPlayer = new Int2IntArrayMap();
    private int killsLeftDefault = -1;
    private int killsLeft = -1;

    protected KillCondition(boolean perPlayer, int minKillsLeft, int maxKillsLeft, Map<Integer, Integer> map, int killsLeftDefault, int killsLeft) {
        super(perPlayer);
        this.minKillsLeft = minKillsLeft;
        this.maxKillsLeft = maxKillsLeft;
        map.forEach((uuid, killsLeft1) -> killsByPlayer.put(uuid.intValue(), killsLeft1.intValue()));
        this.killsLeftDefault = killsLeftDefault;
        this.killsLeft = killsLeft;
    }

    public KillCondition(boolean perPlayer, int minKillsLeft, int maxKillsLeft) {
        super(perPlayer);
        this.minKillsLeft = minKillsLeft;
        this.maxKillsLeft = maxKillsLeft;
    }

    @Override
    public Codec<? extends Condition> configCodec() {
        return CONFIG_CODEC;
    }

    @Override
    public Codec<? extends Condition> diskCodec() {
        return DISK_CODEC;
    }

    public void onEntityDie(LivingEntity dyingEntity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box) {
        LivingEntity killCredit = dyingEntity.getKillCredit();
        if (dyingEntity instanceof MonsterEntity && killCredit != null && killCredit instanceof ServerPlayerEntity && box.isInside(killCredit.blockPosition())) {

            if (killsLeftDefault == -1) {
                this.killsLeftDefault = ((StructureStartAccess) structureStart).getRandom().nextInt(this.maxKillsLeft - this.minKillsLeft) + this.minKillsLeft;
            }
            if (this.killsLeft == -1) {
                this.killsLeft = this.killsLeftDefault;
            }

            int playerUUIDHash = killCredit.getUUID().hashCode();
            this.killsByPlayer.computeIfAbsent(playerUUIDHash, (uuid -> this.killsLeftDefault));
            int prevValue = this.killsByPlayer.get(playerUUIDHash);
            if (prevValue > 0) {
                this.killsByPlayer.put(playerUUIDHash, prevValue - 1);
            }

            if (this.killsLeft > 0) {
                this.killsLeft--;
            }
        }
    }

    @Override
    public boolean checkIfPasses(ServerPlayerEntity playerEntity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box, BlockPos target) {
        if (structureStart.getBoundingBox().isInside(playerEntity.blockPosition())) {
            if (killsLeftDefault == -1) {
                this.killsLeftDefault = ((StructureStartAccess) structureStart).getRandom().nextInt(this.maxKillsLeft - this.minKillsLeft) + this.minKillsLeft;
            }
            if (this.killsLeft == -1) {
                this.killsLeft = this.killsLeftDefault;
            }


            boolean condition = isPerPlayer() ? this.killsByPlayer.get(playerEntity.getUUID().hashCode()) == 0 : this.killsLeft == 0;
            if (!condition) {
                playerEntity.displayClientMessage(textComponent(), true);
            }
            return condition;
        } else {
            return true;
        }
    }

    @Override
    public TranslationTextComponent textComponent() {
        return new TranslationTextComponent("You still need to kill: %s mobs to build/destroy blocks here...", this.killsLeft);
    }
}
