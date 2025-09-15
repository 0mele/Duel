package org.gbing.horse;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;

import java.util.HashMap;
import java.util.UUID;

public class DuelPlugin extends JavaPlugin implements Listener, CommandExecutor {

    // Maps: Challenger UUID -> Challenged UUID
    private final HashMap<UUID, UUID> pendingDuels = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("duel").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("accept")) {
                return handleAccept(player);
            }
            if (args[0].equalsIgnoreCase("deny")) {
                return handleDeny(player);
            }
            // Duel request
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("Player not found or offline.");
                return true;
            }
            if (player.getUniqueId().equals(target.getUniqueId())) {
                player.sendMessage("You cannot duel yourself!");
                return true;
            }
            pendingDuels.put(player.getUniqueId(), target.getUniqueId());
            player.sendMessage("Duel request sent to " + target.getName() + ".");
            target.sendMessage(player.getName() + " wants to duel you! Type /duel accept to fight or /duel deny to refuse.");
            return true;
        }
        // Usage message
        player.sendMessage("Usage: /duel <player> OR /duel accept OR /duel deny");
        return true;
    }

    private boolean handleAccept(Player challenged) {
        UUID challengedId = challenged.getUniqueId();
        UUID challengerId = null;
        for (UUID k : pendingDuels.keySet()) {
            if (pendingDuels.get(k).equals(challengedId)) {
                challengerId = k;
                break;
            }
        }
        if (challengerId == null) {
            challenged.sendMessage("No duel request to accept.");
            return true;
        }
        Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger == null || !challenger.isOnline()) {
            challenged.sendMessage("Challenger not online.");
            pendingDuels.remove(challengerId);
            return true;
        }
        challenged.sendMessage("Duel accepted! Equipping gear...");
        challenger.sendMessage(challenged.getName() + " accepted your duel! Equipping gear...");
        setupDuelGear(challenger);
        setupDuelGear(challenged);
        spawnDuelHorse(challenger);
        spawnDuelHorse(challenged);
        pendingDuels.remove(challengerId);
        return true;
    }

    private boolean handleDeny(Player challenged) {
        UUID challengedId = challenged.getUniqueId();
        UUID challengerId = null;
        for (UUID k : pendingDuels.keySet()) {
            if (pendingDuels.get(k).equals(challengedId)) {
                challengerId = k;
                break;
            }
        }
        if (challengerId == null) {
            challenged.sendMessage("No duel request to deny.");
            return true;
        }
        Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger != null && challenger.isOnline()) {
            challenger.sendMessage(challenged.getName() + " denied your duel request.");
        }
        challenged.sendMessage("You denied the duel request.");
        pendingDuels.remove(challengerId);
        return true;
    }

    private void setupDuelGear(Player player) {
        player.getInventory().clear();

        // Armor
        ItemStack helmet = ench(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        ItemStack chest = ench(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        ItemStack legs = ench(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        ItemStack boots = ench(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION_ENVIRONMENTAL, 2);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chest);
        player.getInventory().setLeggings(legs);
        player.getInventory().setBoots(boots);

        // Sword
        ItemStack sword = ench(new ItemStack(Material.DIAMOND_SWORD), Enchantment.DAMAGE_ALL, 2);
        player.getInventory().addItem(sword);
    }

    private void spawnDuelHorse(Player player) {
        Horse horse = (Horse) player.getWorld().spawnEntity(player.getLocation(), EntityType.HORSE);
        horse.setOwner(player);
        horse.setTamed(true);
        horse.setAdult();

        horse.setMaxHealth(30.0);
        horse.setHealth(30.0);

        AttributeInstance speed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.30);

        horse.setJumpStrength(0.37);

        // Diamond horse armor with Protection 1
        ItemStack armor = ench(new ItemStack(Material.DIAMOND_HORSE_ARMOR), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        ItemStack saddle = new ItemStack(Material.SADDLE);

        // Set horse inventory: needs to be cast to HorseInventory
        HorseInventory inv = horse.getInventory();
        inv.setArmor(armor);
        inv.setSaddle(saddle);

        player.sendMessage("Your duel horse is ready!");
    }

    // Helper to enchant item
    private ItemStack ench(ItemStack item, Enchantment ench, int level) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(ench, level, true);
        item.setItemMeta(meta);
        return item;
    }
}