/*
 * This class was taken and modified from: https://github.com/Commoble/hyperbox/blob/379c81c86122e4fbc4029fb247e853e87ba68b85/src/main/java/commoble/hyperbox/dimension/DimensionRemover.java
 *
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020 Joseph Bettendorff aka "Commoble"
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package corgitaco.structurewarden.util;

import com.google.common.collect.BiMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import corgitaco.structurewarden.mixin.access.DimensionGeneratorSettingsAccess;
import corgitaco.structurewarden.mixin.access.SimpleRegistryAccess;
import corgitaco.structurewarden.mixin.access.WorldBorderAccess;
import corgitaco.structurewarden.mixin.access.WorldBorderImplAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.Dimension;
import net.minecraft.world.World;
import net.minecraft.world.border.IBorderListener;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Map.Entry;

public class DimensionRemover {
    public static final Logger LOGGER = LogManager.getLogger();

    /**
     * @param server A server instance
     * @param keys   The IDs of the dimensions to unregister
     * @return the set of dimension IDs that were successfully unregistered, and a list of the worlds corresponding to them.
     * Be aware that these serverworlds will no longer be accessible via the MinecraftServer after calling this method.
     */
    @SuppressWarnings("deprecation")
    public static Pair<Set<RegistryKey<Dimension>>, List<ServerWorld>> unregisterDimensions(MinecraftServer server, Set<RegistryKey<World>> keys) {
        // we need to remove the dimension/world from three places
        // the dimension registry, the world registry, and the world border listener
        // the world registry is just a simple map and the world border listener has a remove() method
        // the dimension registry has five sub-collections that need to be cleaned up
        // we should also probably move players from that world into the overworld if possible
        DimensionGeneratorSettings dimensionGeneratorSettings = server.getWorldData().worldGenSettings();

        // set of keys whose worlds were found and removed
        Set<RegistryKey<Dimension>> removedKeys = new HashSet<>();
        List<ServerWorld> removedWorlds = new ArrayList<>(keys.size());
        for (RegistryKey<World> key : keys) {
            RegistryKey<Dimension> dimensionKey = RegistryKey.create(Registry.LEVEL_STEM_REGISTRY, key.location());
            ServerWorld removedWorld = server.forgeGetWorldMap().remove(key);
            if (removedWorld != null) {
                // iterate over a copy as the world will remove players from the original list
//                for (ServerPlayerEntity player : Lists.newArrayList((removedWorld.players()))) {
//                    DimensionHelper.ejectPlayerFromDeadWorld(player);
//					server.getPlayerList().func_232644_a_(player, true); // respawn player
//					player.connection.disconnect(new StringTextComponent("Localized existence failure"));
//					Vector3d targetVec = 
//					DimensionHelper.sendPlayerToDimension(player, overworld, targetVec);
//                }
                removedWorld.save(null, false, removedWorld.noSave());
                removeWorldBorderListener(server, removedWorld);
                removedKeys.add(dimensionKey);
                removedWorlds.add(removedWorld);
            }
        }

        if (!removedKeys.isEmpty()) {
            removeRegisteredDimensions(server, dimensionGeneratorSettings, removedKeys);
            server.markWorldsDirty();
        }
        return Pair.of(removedKeys, removedWorlds);
    }

    private static void removeWorldBorderListener(MinecraftServer server, ServerWorld removedWorld) {
        ServerWorld overworld = server.getLevel(World.OVERWORLD);
        WorldBorder overworldBorder = overworld.getWorldBorder();
        List<IBorderListener> listeners = ((WorldBorderAccess) overworldBorder).getListeners();
        IBorderListener target = null;
        for (IBorderListener listener : listeners) {
            if (listener instanceof IBorderListener.Impl) {
                IBorderListener.Impl impl = (IBorderListener.Impl) listener;
                WorldBorder border = ((WorldBorderImplAccess) impl).getWorldBorder();
                if (removedWorld.getWorldBorder() == border) {
                    target = listener;
                    break;
                }
            }
        }
        if (target != null) {
            overworldBorder.removeListener(target);
        }
    }

    private static void removeRegisteredDimensions(MinecraftServer server, DimensionGeneratorSettings settings, Set<RegistryKey<Dimension>> keysToRemove) {
        // get all the old dimensions except the given one, add them to a new registry in the same order
        SimpleRegistry<Dimension> oldRegistry = settings.dimensions();
        SimpleRegistry<Dimension> newRegistry = new SimpleRegistry<Dimension>(Registry.LEVEL_STEM_REGISTRY, oldRegistry.elementsLifecycle());

        BiMap<RegistryKey<Dimension>, Dimension> oldMap = ((SimpleRegistryAccess) oldRegistry).getKeyStorage();
        Map<Dimension, Lifecycle> oldLifecycles = ((SimpleRegistryAccess) oldRegistry).getLifecycles();

        for (Entry<RegistryKey<Dimension>, Dimension> entry : oldMap.entrySet()) {
            RegistryKey<Dimension> key = entry.getKey();
            Dimension dimension = entry.getValue();
            if (key != null && dimension != null && !keysToRemove.contains(key)) // these shouldn't be null but we got an NPE here regardless
            {
                newRegistry.register(key, dimension, oldLifecycles.get(dimension));
            }
        }

        // then replace the old registry with the new registry
        ((DimensionGeneratorSettingsAccess) settings).setDimensions(newRegistry);
    }
}