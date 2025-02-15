package org.venompvp.staff.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.venompvp.staff.Staff;
import org.venompvp.staff.enums.Messages;
import org.venompvp.staff.objs.StaffPlayer;
import org.venompvp.venom.utils.Utils;

import java.util.Optional;

public class EntityListener implements Listener {

    private static final Staff INSTANCE = Staff.getInstance();

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        StaffPlayer staffPlayer = INSTANCE.getStaffPlayer(player);
        if (!event.isCancelled() && staffPlayer != null && staffPlayer.isStaffChat()) {
            INSTANCE.getStaffPlayers().forEach((key, value) -> value.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Messages.STAFFCHAT_FORMAT.toString().replace("{player}", player.getDisplayName()).replace("{message}", event.getMessage()))));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!Utils.compareLocations(event.getFrom(), event.getTo()) && INSTANCE.getFrozenPlayers().contains(event.getPlayer().getUniqueId())) {
            event.setTo(event.getFrom());
            event.getPlayer().sendMessage(Messages.YOU_ARE_FROZEN.toString());
        }
    }

    @EventHandler
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (event.getWhoClicked() instanceof Player && checkStaffMode((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (INSTANCE.getFrozenPlayers().contains(player.getUniqueId())) {
            INSTANCE.getConfig()
                    .getStringList("logged-while-frozen.commands")
                    .forEach(s -> INSTANCE.getServer().dispatchCommand(INSTANCE.getServer().getConsoleSender(), s.replace("{player}", player.getName())));
            if (INSTANCE.getConfig().getBoolean("logged-while-frozen.broadcast.enabled")) {
                INSTANCE.getConfig()
                        .getStringList("logged-while-frozen.broadcast.message")
                        .forEach(s -> INSTANCE.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', s.replace("{player}", player.getName()))));
            }
        } else if (event.getPlayer().hasPermission("venom.staff")) {
            StaffPlayer staffPlayer = INSTANCE.getStaffPlayer(player);
            if (staffPlayer != null) {
                staffPlayer.setVanish(false);
                staffPlayer.setStaffChat(false);
                staffPlayer.removeStaffMode();
                INSTANCE.getStaffPlayers().remove(staffPlayer.getUuid());
            }
        }
    }

    private boolean checkFrozen(Player player) {
        if (INSTANCE.getFrozenPlayers().contains(player.getUniqueId())) {
            player.sendMessage(Messages.YOU_ARE_FROZEN.toString());
            return true;
        }
        return false;
    }

    private boolean checkStaffMode(Player player) {
        StaffPlayer staffPlayer = INSTANCE.getStaffPlayer(player);
        return staffPlayer != null && staffPlayer.isStaffMode();
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (checkFrozen(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        if (checkFrozen(player)) {
            event.setAmount(0);
        } else if (checkStaffMode(player)) {
            player.sendMessage(Messages.NO_PERMISSION.toString());
            event.setAmount(0);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (checkFrozen(player)) {
            event.setCancelled(true);
        } else if (checkStaffMode(player)) {
            player.sendMessage(Messages.NO_PERMISSION.toString());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (checkFrozen(player)) {
                event.setCancelled(true);
            } else if (checkStaffMode(player)) {
                player.sendMessage(Messages.NO_PERMISSION.toString());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (checkFrozen(player)) {
            event.setCancelled(true);
        } else if (checkStaffMode(player)) {
            player.sendMessage(Messages.NO_PERMISSION.toString());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (checkFrozen(player)) {
            event.setCancelled(true);
        } else if (checkStaffMode(player)) {
            player.sendMessage(Messages.NO_PERMISSION.toString());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (checkFrozen(player)) {
                event.setCancelled(true);
            } else if (checkStaffMode(player) && event.isCancelled()) {
                player.sendMessage(Messages.NO_PERMISSION.toString());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (checkFrozen(player)) {
            event.setCancelled(true);
        } else if (event.getItem() != null) {
            StaffPlayer staffPlayer = INSTANCE.getStaffPlayer(player);
            if (staffPlayer != null) {
                if (Utils.isItem(event.getItem(), INSTANCE.getRandomPlayerItemStack())) {
                    final Optional<? extends Player> randomPlayer = INSTANCE.getRandomPlayer("venom.staff");
                    if (randomPlayer.isPresent()) {
                        player.teleport(randomPlayer.get());
                        player.sendMessage(Messages.RANDOM_TELEPORT.toString().replace("{player}", randomPlayer.get().getName()));
                    } else {
                        player.sendMessage(Messages.RANDOM_TELEPORT_ERROR.toString());
                    }
                } else if (Utils.isItem(event.getItem(), INSTANCE.getVanishOnItemStack()) || Utils.isItem(event.getItem(), INSTANCE.getVanishOffItemStack())) {
                    staffPlayer.toggleVanish();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = player.getItemInHand();
        if (event.getRightClicked() != null &&
                event.getRightClicked() instanceof Player &&
                Utils.isItem(itemStack, INSTANCE.getFreezePlayerItemStack()) &&
                INSTANCE.getStaffPlayer(player) != null) {
            final Player clicked = (Player) event.getRightClicked();
            if (INSTANCE.getFrozenPlayers().contains(event.getRightClicked().getUniqueId())) {
                INSTANCE.unfreeze(clicked);
            } else {
                INSTANCE.freeze(clicked);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity entity = event.getEntity();
        if (entity instanceof Player && (INSTANCE.getFrozenPlayers().contains(entity.getUniqueId()) || checkStaffMode((Player) entity)) || damager instanceof Player && (INSTANCE.getFrozenPlayers().contains(damager.getUniqueId()) || checkStaffMode((Player) damager))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player && (INSTANCE.getFrozenPlayers().contains(entity.getUniqueId()) || checkStaffMode((Player) entity))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("venom.staff")) {
            INSTANCE.getStaffPlayers().put(player.getUniqueId(), new StaffPlayer(player.getUniqueId()));
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        StaffPlayer staffPlayer = INSTANCE.getStaffPlayer(event.getPlayer());
        if (staffPlayer != null && staffPlayer.isStaffMode()) {
            event.getPlayer().setGameMode(GameMode.CREATIVE);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (checkFrozen(event.getPlayer()) || checkStaffMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
