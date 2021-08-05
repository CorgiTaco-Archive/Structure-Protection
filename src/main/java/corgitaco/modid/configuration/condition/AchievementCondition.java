package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.Set;

public class AchievementCondition extends Condition {

    private final Set<ResourceLocation> requiredAdvancements;

    public AchievementCondition(Set<ResourceLocation> requiredAdvancements) {
        super(true);
        this.requiredAdvancements = requiredAdvancements;
    }


    @Override
    public Codec<? extends Condition> codec() {
        return null;
    }

    @Override
    public CompoundNBT write() {
        return new CompoundNBT();
    }

    @Override
    public void read(CompoundNBT readNBT) {
    }

    @Override
    public boolean checkIfPasses(ServerPlayerEntity playerEntity, ServerWorld serverWorld, StructureStart<?> structureStart) {
        AdvancementManager advancements = playerEntity.getLevel().getServer().getAdvancements();

        for (ResourceLocation requiredAdvancementID : requiredAdvancements) {
            Advancement advancement = advancements.getAdvancement(requiredAdvancementID);
            if (advancement == null) {
                requiredAdvancements.remove(requiredAdvancementID);
                continue;
            }
            AdvancementProgress advancementProgress = playerEntity.getAdvancements().getOrStartProgress(advancement);

            if (!advancementProgress.isDone()) {
                return false;
            }

        }
        return true;
    }

    @Override
    public TranslationTextComponent textComponent() {
        return null;
    }
}
