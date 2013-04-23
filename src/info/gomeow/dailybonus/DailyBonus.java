package info.gomeow.dailybonus;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class DailyBonus extends JavaPlugin {

    public static Economy econ = null;

    public HashSet<String> playerList = new HashSet<String>();

    public HashMap<String, Integer> numEarly = new HashMap<String, Integer>();
    File configFile;
    FileConfiguration config;
    public Logger log = getLogger();

    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        Player[] players = getServer().getOnlinePlayers();
        for(Player player:players) {
            File file = new File(getDataFolder().getAbsolutePath() + File.separator + "players" + File.separator + player.getName() + ".yml");
            YamlConfiguration pfile = YamlConfiguration.loadConfiguration(file);
            pfile.set("Time.Last", Long.valueOf(System.currentTimeMillis()));
            try {
                pfile.save(file);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onEnable() {
        getServer().getPluginManager().registerEvents(new DailyBonusPlayerListener(this), this);
        setupEconomy();

        configFile = new File(getDataFolder(), "config.yml");
        firstRun();
        config = new YamlConfiguration();
        loadConfig();
        saveDefaultConfig();
        updateConfig();
    }

    @EventHandler
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if(cmd.getName().equalsIgnoreCase("dailybonus")) {
            if(args.length > 0) {
                if(args[0].equalsIgnoreCase("reload")) {
                    if(sender.hasPermission("dailybonus.reload")) {
                        Player[] players = getServer().getOnlinePlayers();
                        for(Player player:players) {
                            File file = new File(getDataFolder().getAbsolutePath() + File.separator + "players" + File.separator + player.getName() + ".yml");
                            YamlConfiguration playerFile = YamlConfiguration.loadConfiguration(file);
                            playerFile.set("Time.Last", System.currentTimeMillis());
                            try {
                                playerFile.save(file);
                            } catch(IOException e) {
                                e.printStackTrace();
                            }
                        }
                        loadConfig();
                        sender.sendMessage(ChatColor.GOLD + "DailyBonus has been reloaded.");
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "You don't have dailybonus.reload permissions!");
                    }
                }
            } else {
                return false;
            }
        }
        return false;
    }

    private void updateConfig() {
        HashMap<String, String> items = new HashMap<String, String>();
        items.put("Main.Number of Tiers", "1");
        items.put("Main.Item Give Delay (In Seconds)", "0");
        items.put("Main.Global Message", "&9[DailyBonus] &6!playername just got abonus of !amount !type for logging in today!");
        items.put("Main.Global Message is Enabled", "true");

        int num = 0;
        for(Entry<String, String> item:items.entrySet()) {
            if(this.config.get(item.getKey()) == null) {
                if((item.getValue()).equalsIgnoreCase("LIST")) {
                    List<String> list = Arrays.asList(new String[] { "LIST ITEMS GO HERE" });
                    this.config.addDefault((String) item.getKey(), list);
                } else if((item.getValue()).equalsIgnoreCase("true")) {
                    this.config.addDefault(item.getKey(), true);
                } else if(item.getValue().equalsIgnoreCase("false")) {
                    this.config.addDefault(item.getKey(), false);
                } else if(isInteger(item.getValue())) {
                    this.config.addDefault(item.getKey(), Integer.valueOf(Integer.parseInt((String) item.getValue())));
                } else {
                    this.config.addDefault(item.getKey(), item.getValue());
                }
                num++;
            }
        }
        if(num > 0) {
            log.info("[DailyBonus] " + num + " missing items added to config file.");
        }
        saveConfig();
    }

    public boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    private void firstRun() {
        if(!this.configFile.exists()) {
            getDataFolder().mkdir();
            this.config = YamlConfiguration.loadConfiguration(this.getResource("config.yml"));
        }
        new File(getDataFolder().getAbsolutePath() + File.separator + "players").mkdir();
    }

    public void loadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            this.config.save(this.configFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private boolean setupEconomy() {
        if(getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if(rsp == null) {
            return false;
        }
        econ = (Economy) rsp.getProvider();
        return econ != null;
    }
}