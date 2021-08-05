package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import corgitaco.modid.mixin.access.StructureStartAccess;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class KillCondition extends Condition {

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
    public Codec<? extends Condition> codec() {
        return null;
    }

    @Override
    public CompoundNBT write() {
        CompoundNBT compoundNBT = new CompoundNBT();
        compoundNBT.putInt("killsLeftDefault", this.killsLeftDefault);
        compoundNBT.putInt("killsLeft", this.killsLeft);
        ListNBT listNBT = new ListNBT();
        for (Int2IntArrayMap.Entry entry : this.killsByPlayer.int2IntEntrySet()) {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("uuid", entry.getIntKey());
            nbt.putInt("killsLeft", entry.getIntValue());
            listNBT.add(nbt);
        }
        compoundNBT.put("killsLeftByPlayer", listNBT);
        return compoundNBT;
    }

    @Override
    public void read(CompoundNBT readNBT) {
        this.killsLeftDefault = readNBT.getInt("killsLeftDefault");
        this.killsLeft = readNBT.getInt("killsLeft");

        for (INBT killsLeftByPlayer : readNBT.getList("killsLeftByPlayer", 10)) {
            this.killsByPlayer.put(NBTUtil.loadUUID(killsLeftByPlayer).hashCode(), ((CompoundNBT) killsLeftByPlayer).getInt("killsLeft"));
        }
    }

    public void onEntityDie(LivingEntity dyingEntity, ServerWorld serverWorld, StructureStart<?> structureStart) {
        LivingEntity killCredit = dyingEntity.getKillCredit();
        if (dyingEntity instanceof MonsterEntity && killCredit != null && killCredit instanceof ServerPlayerEntity && structureStart.getBoundingBox().isInside(killCredit.blockPosition())) {

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
    public boolean checkIfPasses(ServerPlayerEntity playerEntity, ServerWorld serverWorld, StructureStart<?> structureStart) {
        if (structureStart.getBoundingBox().isInside(playerEntity.blockPosition())) {
            if (killsLeftDefault == -1) {
                this.killsLeftDefault = ((StructureStartAccess) structureStart).getRandom().nextInt(this.maxKillsLeft - this.minKillsLeft) + this.minKillsLeft;
            }
            if (this.killsLeft == -1) {
                this.killsLeft = this.killsLeftDefault;
            }

            return isPerPlayer() ? this.killsByPlayer.get(playerEntity.getUUID().hashCode()) == 0 : this.killsLeft == 0;
        } else {
            return true;
        }
    }

    @Override
    public TranslationTextComponent textComponent() {
        return new TranslationTextComponent("You still need to kill: %s mobs to build/destroy blocks here...", this.killsLeft);
    }
}
