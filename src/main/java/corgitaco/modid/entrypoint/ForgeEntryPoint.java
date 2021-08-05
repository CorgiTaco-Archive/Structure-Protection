package corgitaco.modid.entrypoint;


import corgitaco.modid.Main;
import corgitaco.modid.StructureProtector;
import corgitaco.modid.configuration.StructureStartProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEntryPoint {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof PlayerEntity) {
            for (Structure<?> structure : entity.level.getChunkAt(entity.blockPosition()).getAllReferences().keySet()) {
                Optional<? extends StructureStart<?>> structureStart = ((ServerWorld) entity.level).startsForFeature(SectionPos.of(entity.blockPosition()), structure).findFirst();
                structureStart.ifPresent(start -> {
                    StructureStartProtection protector = ((StructureProtector) start).getProtector();
                    if (protector != null && !protector.conditionsMet((ServerPlayerEntity) entity, (ServerWorld) entity.level, start, event.getPos())) {
                        ((ServerPlayerEntity) entity).displayClientMessage(new TranslationTextComponent("No bad"), true);
                        event.setCanceled(true);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onBlockDestroy(BlockEvent.BreakEvent event) {
        Entity entity = event.getPlayer();
        for (Structure<?> structure : entity.level.getChunkAt(entity.blockPosition()).getAllReferences().keySet()) {
            Optional<? extends StructureStart<?>> structureStart = ((ServerWorld) entity.level).startsForFeature(SectionPos.of(entity.blockPosition()), structure).findFirst();
            structureStart.ifPresent(start -> {
                StructureStartProtection protector = ((StructureProtector) start).getProtector();
                if (protector != null && !protector.conditionsMet((ServerPlayerEntity) entity, (ServerWorld) entity.level, start, event.getPos())) {
                    event.setCanceled(true);
                }
            });
        }
    }
}
