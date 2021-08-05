package corgitaco.modid.configuration;

import corgitaco.modid.configuration.condition.Condition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.List;

public class StructureStartProtection {

    private final List<Condition> conditionList;

    public StructureStartProtection(List<Condition> conditionList) {
        this.conditionList = conditionList;
    }


    public boolean conditionsMet(ServerPlayerEntity playerEntity, ServerWorld world, StructureStart<?> structureStart) {
        for (Condition condition : this.conditionList) {
            if (!condition.checkIfPasses(playerEntity, world, structureStart)) {
                return false;
            }

        }
        return true;
    }


    public CompoundNBT write() {
        CompoundNBT compoundNBT = new CompoundNBT();

        ListNBT listNBT = new ListNBT();
        for (Condition condition : conditionList) {
            listNBT.add(condition.write());
        }

        compoundNBT.put("conditions", listNBT);
        return compoundNBT;
    }

    public void read(CompoundNBT nbt) {
        for (Condition condition : this.conditionList) {
            condition.read(nbt);
        }
    }

    public void onEntityDeath(LivingEntity entity, ServerWorld world, StructureStart<?> structureStart) {
        for (Condition condition : this.conditionList) {
            condition.onEntityDie(entity, world, structureStart);
        }
    }
}
