package corgitaco.modid.configuration.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.modid.Main;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdvancementCondition extends Condition {

    public static final Codec<AdvancementCondition> CONFIG_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.list(ResourceLocation.CODEC).fieldOf("requiredAdvancements").forGetter((advancementCondition) -> {
            return new ArrayList<>(advancementCondition.requiredAdvancements);
        })).apply(builder, (list) -> new AdvancementCondition(new HashSet<>(list)));
    });

    private final Set<ResourceLocation> requiredAdvancements;
    private final Set<Advancement> fastAdvancements = new ObjectOpenHashSet<>();

    public AdvancementCondition(Set<ResourceLocation> requiredAdvancements) {
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
    public boolean checkIfPasses(ServerPlayerEntity playerEntity, ServerWorld serverWorld, StructureStart<?> structureStart, MutableBoundingBox box, BlockPos target, ActionType actionType, List<TranslationTextComponent> requirements) {
        AdvancementManager advancements = serverWorld.getServer().getAdvancements();
        if (box.isInside(target)) {
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

            boolean missingAdvancement = false;
            TranslationTextComponent component = null;
            for (Advancement advancement : fastAdvancements) {
                AdvancementProgress advancementProgress = playerEntity.getAdvancements().getOrStartProgress(advancement);

                if (!advancementProgress.isDone()) {
                    missingAdvancement = true;
                    if (component == null) {
                        component = new TranslationTextComponent(Main.MOD_ID + ".condition.advancement", advancement.getChatComponent());
                    } else {
                        component.append(", ").append(advancement.getChatComponent());
                    }
                }
            }
            if (component != null) {
                component.append(".");
                requirements.add(component);
            }

            return missingAdvancement;
        }
        return true;
    }
}
