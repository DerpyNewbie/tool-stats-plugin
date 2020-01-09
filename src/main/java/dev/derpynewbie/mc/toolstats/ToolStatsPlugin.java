package dev.derpynewbie.mc.toolstats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ToolStatsPlugin extends JavaPlugin implements Listener, TabExecutor {

    private static ToolStatsPlugin INSTANCE;

    private static Pattern GET_STATS_PATTERN = Pattern.compile("stats|getstats", Pattern.CASE_INSENSITIVE);
    private static Pattern UPDATE_LORE_PATTERN = Pattern.compile("updatelore", Pattern.CASE_INSENSITIVE);
    private static Pattern REMOVE_LORE_PATTERN = Pattern.compile("resetlore|removelore", Pattern.CASE_INSENSITIVE);
    private static Pattern REMOVE_STATS_PATTERN = Pattern.compile("reset|removestats", Pattern.CASE_INSENSITIVE);
    private static Pattern HELP_PATTERN = Pattern.compile("help|h", Pattern.CASE_INSENSITIVE);
    private static List<String> POSSIBLE_COMMANDS = Arrays.asList("help", "getStats", "updateLore", "removeLore", "removeStats");

    @Override
    public void onEnable() {
        super.onEnable();

        INSTANCE = this;

        saveDefaultConfig();
        reloadConfig();

        Bukkit.getPluginManager().registerEvents(this, this);
        PluginCommand command = Bukkit.getPluginCommand("toolstats");

        if (command == null) {
            getLogger().severe("Could not get plugin command. please report this on " + getDescription().getWebsite() + ".");
            getLogger().severe("Plugin will be disabled due to this problem!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        command.setExecutor(this);
        command.setTabCompleter(this);

    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        increment(event.getPlayer().getInventory().getItemInMainHand(), ItemInfoType.TOOL);
    }

    @EventHandler
    public void onEntityKilled(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();

        if (player != null) {
            increment(player.getInventory().getItemInMainHand(), ItemInfoType.WEAPON);
        }
    }

    private void increment(ItemStack item, ItemInfoType type) {
        if (item.getItemMeta() != null && type.contains(item.getType())) {
            int i = type.getValue(item.getItemMeta());
            i++;
            type.setValue(item, i);

            ItemMeta meta = item.getItemMeta();

            item.setItemMeta(type.getUpdatedLoreItemMeta(meta));
        } else {
            getLogger().warning("Failed to increment stats of item: " + item.getType().name() + ", info type of: " + type.name());
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 0)
            return commandHelp(sender, label, args);

        String match = args[0];

        if (GET_STATS_PATTERN.matcher(match).matches())
            return commandGetStats(sender);
        else if (UPDATE_LORE_PATTERN.matcher(match).matches())
            return commandUpdateLore(sender);
        else if (REMOVE_LORE_PATTERN.matcher(match).matches())
            return commandRemoveLore(sender);
        else if (REMOVE_STATS_PATTERN.matcher(match).matches())
            return commandResetStats(sender);
        else
            return commandHelp(sender, label, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) {
            return POSSIBLE_COMMANDS;
        } else if (args.length == 1) {
            return POSSIBLE_COMMANDS.stream().filter(s -> s.startsWith(args[0])).sorted().collect(Collectors.toCollection(ArrayList::new));
        } else if (args.length == 2) {
            if (HELP_PATTERN.matcher(args[0]).matches()) {
                return POSSIBLE_COMMANDS.stream().filter(s -> s.startsWith(args[1])).sorted().collect(Collectors.toCollection(ArrayList::new));
            }
        }

        return super.onTabComplete(sender, command, alias, args);
    }

    private boolean commandGetStats(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command cannot be executed from console.");
            return true;
        }

        Player player = (Player) sender;
        ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();

        if (meta != null) {
            for (ItemInfoType type :
                    ItemInfoType.values()) {
                player.sendMessage("Type: " + type.name() + ", Value: " + type.getValue(meta) + ", Display: " + meta.getPersistentDataContainer().getOrDefault(type.getDisplayKey(), PersistentDataType.INTEGER, -1));
            }
            player.sendMessage(ChatColor.GREEN + "Successfully sent stats info.");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Item does not have item meta.");
            return true;
        }
    }

    private boolean commandResetStats(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command cannot be executed from console.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        for (ItemInfoType type :
                ItemInfoType.values()) {
            type.setValue(item, null);
            type.updateLore(item);
        }
        player.sendMessage(ChatColor.GREEN + "Successfully removed stats.");
        return true;
    }

    private boolean commandRemoveLore(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command cannot be executed from console.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setLore(null);
            item.setItemMeta(meta);
            player.sendMessage(ChatColor.GREEN + "Successfully removed lore.");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Item does not have item meta.");
            return true;
        }
    }

    private boolean commandUpdateLore(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command cannot be executed from console.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        for (ItemInfoType type :
                ItemInfoType.values()) {
            type.updateLore(item);
        }
        player.sendMessage(ChatColor.GREEN + "Successfully updated lore.");
        return true;

    }

    private boolean commandHelp(CommandSender sender, String label, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(
                            getConfig().getString("help.index")).replaceAll("<command>", label)
                    )
            );
            return true;
        }

        String match = args[1];

        if (GET_STATS_PATTERN.matcher(match).matches())
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(
                            getConfig().getString("help.get-stats")).replaceAll("<command>", label)
                    )
            );
        else if (UPDATE_LORE_PATTERN.matcher(match).matches())
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(
                            getConfig().getString("help.update-lore")).replaceAll("<command>", label)
                    )
            );
        else if (REMOVE_LORE_PATTERN.matcher(match).matches())
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(
                            getConfig().getString("help.remove-lore")).replaceAll("<command>", label)
                    )
            );
        else if (REMOVE_STATS_PATTERN.matcher(match).matches())
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(
                            getConfig().getString("help.remove-stats")).replaceAll("<command>", label)
                    )
            );
        else
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(
                            getConfig().getString("help.index")).replaceAll("<command>", label)
                    )
            );

        return true;
    }

    public static ToolStatsPlugin getInstance() {
        return INSTANCE;
    }

    public enum ItemInfoType {
        TOOL("tooltip.tool", new Material[]{
                Material.WOODEN_SHOVEL, Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_HOE,
                Material.STONE_SHOVEL, Material.STONE_PICKAXE, Material.WOODEN_AXE, Material.STONE_HOE,
                Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_HOE,
                Material.GOLDEN_SHOVEL, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_HOE,
                Material.DIAMOND_SHOVEL, Material.DIAMOND_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_HOE
        }, "toolStats", "toolStatsDisplay"),
        WEAPON("tooltip.weapon", new Material[]{
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD
        }, "weaponStats", "weaponStatsDisplay"),
        ;


        String configPath;
        Material[] materials;
        String key;
        String displayKey;

        ItemInfoType(String msgConfigPath, Material[] checkingMaterials, String key, String displayKey) {
            this.configPath = msgConfigPath;
            this.materials = checkingMaterials;
            this.key = key;
            this.displayKey = displayKey;
        }

        public NamespacedKey getKey() {
            return new NamespacedKey(ToolStatsPlugin.getInstance(), key);
        }

        public NamespacedKey getDisplayKey() {
            return new NamespacedKey(ToolStatsPlugin.getInstance(), displayKey);
        }

        public int getValue(ItemMeta meta) {
            return meta.getPersistentDataContainer().getOrDefault(getKey(), PersistentDataType.INTEGER, 0);
        }

        public void setValue(ItemStack item, @Nullable Integer value) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return;
            }

            if (value == null)
                meta.getPersistentDataContainer().remove(getKey());
            else
                meta.getPersistentDataContainer().set(getKey(), PersistentDataType.INTEGER, value);

            item.setItemMeta(meta);
        }

        public boolean contains(Material refMat) {
            for (Material mat :
                    materials) {
                if (mat.equals(refMat)) {
                    return true;
                }
            }

            return false;
        }

        public ItemMeta getUpdatedLoreItemMeta(@NotNull ItemMeta meta) {
            PersistentDataContainer data = meta.getPersistentDataContainer();
            String format = ChatColor.WHITE + ToolStatsPlugin.getInstance().getConfig().getString(configPath);
            List<String> lore = meta.getLore();
            int value = getValue(meta);
            int displayPos;
            NamespacedKey dKey = getDisplayKey();

            if (lore == null) {
                lore = new ArrayList<>();
            }

            if (data.has(dKey, PersistentDataType.INTEGER)) {
                displayPos = data.getOrDefault(dKey, PersistentDataType.INTEGER, -1);

                if (lore.size() == displayPos) {
                    ToolStatsPlugin.getInstance().getLogger().severe("Display key containing invalid value of " + displayPos + ", when lore size is " + lore.size());
                    data.remove(dKey);
                    meta.setLore(lore);
                    return meta;
                }
            } else {
                displayPos = lore.size();
                data.set(dKey, PersistentDataType.INTEGER, displayPos);
            }

            if (value <= 0) {
                if (lore.size() > displayPos)
                    lore.remove(displayPos);
                data.remove(dKey);
                meta.setLore(lore);
                return meta;
            }

            String display = String.format(format, value);
            if (lore.size() > displayPos)
                lore.remove(displayPos);
            lore.add(displayPos, display);
            meta.setLore(lore);
            return meta;
        }

        public void updateLore(@NotNull ItemStack item) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                item.setItemMeta(getUpdatedLoreItemMeta(meta));
            }
        }
    }

}
