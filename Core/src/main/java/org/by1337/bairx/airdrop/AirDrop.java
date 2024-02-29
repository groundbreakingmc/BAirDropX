package org.by1337.bairx.airdrop;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.by1337.bairx.event.EventType;
import org.by1337.bairx.inventory.InventoryManager;
import org.by1337.bairx.timer.Timer;
import org.by1337.blib.chat.Placeholderable;
import org.by1337.blib.util.NameKey;
import org.by1337.blib.util.SpacedNameKey;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;

public interface AirDrop extends Placeholderable {
    World getWorld();

    void tick();

    Set<SpacedNameKey> getSignedListeners();

    boolean isStarted();

    NameKey getId();

    boolean isUseDefaultTimer();

    Location getLocation();

    InventoryManager getInventoryManager();

    void callEvent(@Nullable Player player, EventType eventType);
    void save() throws IOException, InvalidConfigurationException;
    void trySave();
}
