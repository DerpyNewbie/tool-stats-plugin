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

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ToolStatsPlugin extends JavaPlugin implements Listener, TabExecutor {

    private static ToolStatsPlugin INSTANCE;

    private static Pattern GET_STATS_PATTERN = Pattern.compile("stats|getstats", Pattern.CASE_INSENSITIVE);
    private static Pattern UPDATE_LORE_PATTERN = Pattern.compile("updatelore", Pattern.CASE_INSENSITIVE);
    private static Pattern REMOVE_LORE_PATTERN = Pattern.compile("resetlore|removelore", Pattern.CASE_INSENSITIVE);
    private static Pattern REMOVE_STATS_PATTERN = Pattern.compile("reset|removestats", Pattern.CASE_INSENSITIVE);
    private static Pattern REFRESH_MATERIALS_PATTERN = Pattern.compile("refreshmaterials|refresh", Pattern.CASE_INSENSITIVE);
    private static Pattern HELP_PATTERN = Pattern.compile("help|h", Pattern.CASE_INSENSITIVE);
    private static List<String> POSSIBLE_COMMANDS = Arrays.asList("help", "getStats", "updateLore", "removeLore", "removeStats", "refreshMaterials");

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

        for (ItemInfoType type :
                ItemInfoType.values()) {
            type.lastCacheUpdate = -1;
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (ItemInfoType.TOOL.contains(event.getPlayer().getInventory().getItemInMainHand().getType()))
            increment(event.getPlayer().getInventory().getItemInMainHand(), ItemInfoType.TOOL);
    }

    @EventHandler
    public void onEntityKilled(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();

        if (player != null && ItemInfoType.WEAPON.contains(player.getInventory().getItemInMainHand().getType())) {
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
        else if (REFRESH_MATERIALS_PATTERN.matcher(match).matches())
            return commandRefreshMaterials(sender);
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

    private boolean commandRefreshMaterials(CommandSender sender) {
        int curr = 0;
        int max = ItemInfoType.values().length;
        sender.sendMessage(ChatColor.GRAY + "Refreshing material lists... " + ChatColor.DARK_GRAY + "[" + curr + "/" + max + "]");
        for (ItemInfoType type :
                ItemInfoType.values()) {
            try {
                curr++;
                sender.sendMessage(ChatColor.GRAY + "Refreshing in " + type.name() + ". " + ChatColor.DARK_GRAY +  "[" + curr + "/" + max + "]");
                type.reloadMaterials();
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + "Refreshing failed in " + type.name() +". " + ChatColor.DARK_GRAY +  "[" + curr + "/" + max + "] : " + ex.getMessage());
                curr--;
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Successfully refreshed material lists. " + ChatColor.DARK_GREEN +  "[" + curr + "/" + max + "]");
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
        else if (REFRESH_MATERIALS_PATTERN.matcher(match).matches())
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(
                            getConfig().getString("help.refresh-materials")).replaceAll("<command>", label)
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

    @SuppressWarnings("WeakerAccess")
    public static ToolStatsPlugin getInstance() {
        return INSTANCE;
    }

    public enum ItemInfoType {
        TOOL("tooltip.tool", "item-info-type.tool", "toolStats", "toolStatsDisplay"),
        WEAPON("tooltip.weapon", "item-info-type.weapon", "weaponStats", "weaponStatsDisplay"),
        ;

        String msgConfigPath;
        String materialConfigPath;
        String key;
        String displayKey;
        Material[] cachedMats = null;
        public long lastCacheUpdate = -1;

        ItemInfoType(String msgConfigPath, String materialConfigPath, String key, String displayKey) {
            this.msgConfigPath = msgConfigPath;
            this.materialConfigPath = materialConfigPath;
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

        public Material[] getMaterials() {
            long currentTime = System.currentTimeMillis();
            if (cachedMats == null || lastCacheUpdate - currentTime > 28800000) { // max cache time = 8 hours - refreshes it every 8 hour or after onDisable() was called.
                reloadMaterials();
            }

            return cachedMats;
        }

        public Material[] reloadMaterials() {
            ToolStatsPlugin.getInstance().getLogger().warning("Refreshing material list in " + this.name() + ". this might cause lag.");
            List<String> rawMatList = ToolStatsPlugin.getInstance().getConfig().getStringList(materialConfigPath);
            List<Material> materialList = new ArrayList<>();
            for (String s :
                    rawMatList) {
                Material m = Material.getMaterial(s);
                if (m != null)
                    materialList.add(m);
            }

            cachedMats = materialList.toArray(new Material[0]);
            lastCacheUpdate = System.currentTimeMillis();
            ToolStatsPlugin.getInstance().getLogger().info("Material list has successfully refreshed in " + this.name() + ".");
            return materialList.toArray(new Material[0]);
        }

        public boolean contains(Material refMat) {
            for (Material mat :
                    getMaterials()) {
                if (mat.equals(refMat)) {
                    return true;
                }
            }

            return false;
        }

        public ItemMeta getUpdatedLoreItemMeta(@NotNull ItemMeta meta) {
            PersistentDataContainer data = meta.getPersistentDataContainer();
            String format = ChatColor.WHITE + ToolStatsPlugin.getInstance().getConfig().getString(msgConfigPath);
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
