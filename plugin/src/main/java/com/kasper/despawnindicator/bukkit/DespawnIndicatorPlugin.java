package com.kasper.despawnindicator.bukkit;

import com.kasper.despawnindicator.BuildInfo;
import com.kasper.despawnindicator.ConfigFile;
import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.HelloMessage;
import com.kasper.despawnindicator.ModLog;
import com.kasper.despawnindicator.ServerSettings;
import com.kasper.despawnindicator.ServerText;
import com.kasper.despawnindicator.TimeText;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server side of the mod for Paper, Folia and Spigot: players with the mod say hello and from then on get
 * the exact despawn times of the items around them and the timer of the items they lost when they died.
 */
public final class DespawnIndicatorPlugin extends JavaPlugin implements Listener, PluginMessageListener {

    static final String HELLO = DespawnIndicator.MOD_ID + ":" + DespawnIndicator.CHANNEL_HELLO;
    static final String CLOCKS = DespawnIndicator.MOD_ID + ":" + DespawnIndicator.CHANNEL_CLOCKS;

    private final Map<UUID, Viewer> viewers = new ConcurrentHashMap<>();
    private ConfigFile config;
    private Tasks tasks;
    private DeathLoot loot;

    @Override
    public void onEnable() {
        ModLog.use(new ModLog.Sink() {
            @Override
            public void info(String message) {
                getLogger().info(message);
            }

            @Override
            public void warn(String message) {
                getLogger().warning(message);
            }
        });
        config = new ConfigFile(getDataFolder().toPath().resolve(ServerSettings.FILE), ServerSettings.DEFAULTS);
        config.get();
        SpigotConfig.reload();
        tasks = Tasks.of(this);
        loot = new DeathLoot(tasks);
        getServer().getMessenger().registerIncomingPluginChannel(this, HELLO, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CLOCKS);
        getServer().getPluginManager().registerEvents(this, this);
        // After /reload the clients do not say hello again; those that listen to the mod's channel have it.
        // Their protocol is unknown, so they get what the first one has: item clocks, no loot timer.
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getListeningPluginChannels().contains(CLOCKS)) {
                greet(player, HelloMessage.LOOT - 1);
            }
        }
    }

    @Override
    public void onDisable() {
        for (Viewer v : viewers.values()) {
            v.stop();
        }
        viewers.clear();
        if (loot != null) {
            loot.clear();
        }
    }

    ServerSettings settings() {
        return ServerSettings.parse(config.get());
    }

    DeathLoot loot() {
        return loot;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!HELLO.equals(channel)) {
            return;
        }
        HelloMessage hello = HelloMessage.decode(message);
        if (hello != null) {
            listen(player);
            greet(player, hello.protocol);
        }
    }

    /** A player with the mod: they get clocks from now on, all of them anew. */
    private void greet(Player player, int protocol) {
        Viewer old = viewers.get(player.getUniqueId());
        if (old != null && old.player == player && old.protocol == protocol) {
            old.hello();
            return;
        }
        Viewer v = new Viewer(this, player, protocol);
        if (old != null) {
            old.stop();
        }
        viewers.put(player.getUniqueId(), v);
        v.start(tasks);
    }

    /**
     * The server only sends a player messages on channels the client said it listens to; clients on some
     * loaders do not say so, and saying hello is enough to know.
     */
    private void listen(Player player) {
        if (player.getListeningPluginChannels().contains(CLOCKS)) {
            return;
        }
        try {
            Method add = player.getClass().getMethod("addChannel", String.class);
            add.invoke(player, CLOCKS);
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModLog.warn("Could not open the despawn times channel to " + player.getName() + ": " + e);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Viewer v = viewers.remove(event.getPlayer().getUniqueId());
        if (v != null) {
            v.stop();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Viewer v = viewers.get(player.getUniqueId());
        if (v != null && v.protocol >= HelloMessage.LOOT) {
            loot.died(player, player.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getRemaining() == 0) {
            loot.gone(event.getItem().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDespawn(ItemDespawnEvent event) {
        loot.gone(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        loot.gone(event.getEntity().getUniqueId());
    }

    /** {@code /despawnindicator}: the plugin's status on this server, in the language of whoever asks. */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String lang = "";
        World world = null;
        if (sender instanceof Player) {
            Player player = (Player) sender;
            lang = language(player);
            world = player.getWorld();
        } else {
            List<World> worlds = Bukkit.getWorlds();
            if (!worlds.isEmpty()) {
                world = worlds.get(0);
            }
        }
        int lifetime = world == null ? DespawnIndicator.VANILLA_LIFETIME : SpigotConfig.itemDespawnRate(world);
        sender.sendMessage(ServerText.get(lang, "status.title", BuildInfo.version()));
        sender.sendMessage(ServerText.get(lang, "status.players", viewers.size(), Bukkit.getOnlinePlayers().size()));
        sender.sendMessage(ServerText.get(lang, "status.lifetime", TimeText.format(lifetime, false)));
        return true;
    }

    private static String language(Player player) {
        try {
            return player.getLocale();
        } catch (LinkageError | RuntimeException e) {
            return "";
        }
    }
}
