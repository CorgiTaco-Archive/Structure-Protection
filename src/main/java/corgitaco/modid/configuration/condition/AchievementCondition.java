package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class AchievementCondition extends Condition {

    public static final Codec<AchievementCondition> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.list(ResourceLocation.CODEC).fieldOf("requiredAdvancements").forGetter((achievementCondition) -> {
            return new ArrayList<>(achievementCondition.requiredAdvancements);
        })).apply(builder, (list) -> new AchievementCondition(new HashSet<>(list)));
    });

    private final Set<ResourceLocation> requiredAdvancements;
    private final Set<Advancement> fastAdvancements = new ObjectOpenHashSet<>();

    public AchievementCondition(Set<ResourceLocation> requiredAdvancements) {
        super(true);
        this.requiredAdvancements = requiredAdvancements;
    }


    @Override
    public Codec<? extends Condition> configCodec() {
        return CONFIG_CODEC;
    }

    @Override
    public Codec<? extends Condition> diskCodec() {
        return CONFIG_CODEC;
    }

    @Override
    public boolean checkIfPasses(ServerPlayerEntity playerEntity, ServerWorld serverWorld, StructureStart<?> structureStart) {
        AdvancementManager advancements = playerEntity.getLevel().getServer().getAdvancements();

        if (fastAdvancements.isEmpty()) {
            for (ResourceLocation requiredAdvancementID : requiredAdvancements) {
                Advancement advancement = advancements.getAdvancement(requiredAdvancementID);
                if (advancement == null) {
                    requiredAdvancements.remove(requiredAdvancementID);
                    continue;
                }
                fastAdvancements.add(advancement);
            }
        }

        for (Advancement advancement : fastAdvancements) {
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
