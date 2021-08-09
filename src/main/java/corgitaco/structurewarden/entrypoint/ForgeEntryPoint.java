package corgitaco.structurewarden.entrypoint;


import corgitaco.structurewarden.StructureWarden;
import corgitaco.structurewarden.StructureProtector;
import corgitaco.structurewarden.configuration.StructureStartProtection;
import corgitaco.structurewarden.configuration.condition.ActionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StructureWarden.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEntryPoint {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayerEntity) {
            World level = entity.level;
            for (Structure<?> structure : level.getChunkAt(entity.blockPosition()).getAllReferences().keySet()) {
                ((ServerWorld) level).startsForFeature(SectionPos.of(entity.blockPosition()), structure).filter(structureStart1 -> structureStart1.getBoundingBox().isInside(entity.blockPosition())).forEach(start -> {
                    StructureStartProtection protector = ((StructureProtector) start).getProtector();
                    if (protector != null && !protector.conditionsMet((ServerPlayerEntity) entity, (ServerWorld) level, start, event.getPos(), ActionType.BLOCK_PLACE)) {
                        event.setCanceled(true);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onBlockDestroy(BlockEvent.BreakEvent event) {
        Entity entity = event.getPlayer();
        World level = entity.level;
        for (Structure<?> structure : level.getChunkAt(entity.blockPosition()).getAllReferences().keySet()) {
            ((ServerWorld) level).startsForFeature(SectionPos.of(entity.blockPosition()), structure).filter(structureStart1 -> structureStart1.getBoundingBox().isInside(entity.blockPosition())).forEach(start -> {
                StructureStartProtection protector = ((StructureProtector) start).getProtector();
                if (protector != null && !protector.conditionsMet((ServerPlayerEntity) entity, (ServerWorld) level, start, event.getPos(), ActionType.BLOCK_BREAK)) {
                    event.setCanceled(true);
                }
            });
        }
    }
}
