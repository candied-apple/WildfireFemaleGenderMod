/*
    Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 WildfireRomeo

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.wildfire.main;

import com.mojang.logging.LogUtils;
import com.wildfire.main.entitydata.PlayerConfig;
import com.wildfire.main.networking.WildfireSync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

public class WildfireGender implements ModInitializer {
    public static final String VERSION = "3.1";
    public static final String MODID = "wildfire_gender";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<UUID, PlayerConfig> PLAYER_CACHE = new HashMap<>();

    public void onInitialize() {
        WildfireSounds.register();
        ServerPlayNetworking.registerGlobalReceiver(WildfireSync.SEND_GENDER_IDENTIFIER, WildfireSync::handle);
        EntityTrackingEvents.START_TRACKING.register(this::onBeginTracking);
    }

    private void onBeginTracking(Entity tracked, ServerPlayerEntity syncTo) {
        if(tracked instanceof PlayerEntity toSync) {
            PlayerConfig genderToSync = WildfireGender.getPlayerById(toSync.getUuid());
            if(genderToSync == null) return;
            // Note that we intentionally don't check if we've previously synced a player with this code path;
            // because we use entity tracking to sync, it's entirely possible that one player would leave the
            // tracking distance of another, change their settings, and then re-enter their tracking distance;
            // we wouldn't sync while they're out of tracking distance, and as such, their settings would be out
            // of sync until they relog.
            WildfireSync.sendToClient(syncTo, genderToSync);
        }
    }

    public static @Nullable PlayerConfig getPlayerById(UUID id) {
        return PLAYER_CACHE.get(id);
    }

    public static @Nonnull PlayerConfig getOrAddPlayerById(UUID id) {
        return PLAYER_CACHE.computeIfAbsent(id, PlayerConfig::new);
    }

    public static Future<Optional<PlayerConfig>> loadGenderInfo(UUID uuid, boolean markForSync) {
        return Util.getIoWorkerExecutor().submit(() -> Optional.ofNullable(PlayerConfig.loadCachedPlayer(uuid, markForSync)));
    }
}
