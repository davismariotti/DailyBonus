package info.gomeow.dailybonus;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class DailyBonusPlayerListener implements Listener {
    public DailyBonus plugin;

    public DailyBonusPlayerListener(DailyBonus instance) {
        this.plugin = instance;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if(!checkLastLogin(player)) {
            return;
        }
        int tiers = this.plugin.config.getInt("Main.Number of Tiers");
        for(int x = tiers; x > 0; x--) {
            if(!player.hasPermission("dailybonus.tier." + x)) {
                continue;
            }
            if(this.plugin.config.getInt("Main.Item Give Delay (In Seconds)") > 0) {
                Runnable r = new DailyBonusItemDelay(this.plugin, player, x);
                this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, r, 20 * this.plugin.config.getInt("Main.Item Give Delay (In Seconds)"));
                this.plugin.playerList.add(player.getName());
                x = -666;
            } else {
                int amount = 0;
                String amt = this.plugin.config.getString("Tier." + x + ".Economy Amount");
                if(amt.split(";").length > 1) {
                    amount = Integer.parseInt(amt.split(";")[0]) + (int) (Math.random() * (Integer.parseInt(amt.split(";")[1]) * 2) - Integer.parseInt(amt.split(";")[1]));
                } else {
                    amount = this.plugin.config.getInt("Tier." + x + ".Economy Amount");
                }
                if(amount > 0) {
                    if(DailyBonus.econ != null) {
                        DailyBonus.econ.depositPlayer(player.getName(), amount);
                    } else {
                        player.sendMessage(ChatColor.DARK_RED + "The DailyBonus plugin would have given you economy money, but the server doesn't have Vault enabled, or it is not enabled correctly!");
                    }
                }
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.config.getString("Tier." + x + ".Message").replaceAll("!amount", "" + amount).replaceAll("!type", DailyBonus.econ.currencyNamePlural())));
                if(this.plugin.config.get("Tier." + x + ".Items") != null) {
                    List<String> items = this.plugin.config.getStringList("Tier." + x + ".Items");

                    for(String itemsline:items) {
                        String[] line = itemsline.split(";");
                        String[] data = itemsline.split("-");
                        if(!line[0].equals("0")) {
                            ItemStack is = new ItemStack(Material.getMaterial(Integer.parseInt(line[0])), Integer.parseInt(line[1]));
                            if(data.length > 1) {
                                is.setDurability(Short.parseShort(data[1]));
                            }

                            if(line.length > 2) {
                                is.setAmount(Integer.parseInt(line[1]) + (int) (Math.random() * (Integer.parseInt(line[2].split("-")[0]) * 2) - Integer.parseInt(line[2].split("-")[0])));
                            }

                            if(player.getInventory().firstEmpty() < 0) {
                                player.getWorld().dropItemNaturally(player.getEyeLocation(), is);
                            } else {
                                player.getInventory().addItem(new ItemStack[] { is });
                            }
                        }
                    }
                }
                if(this.plugin.config.get("Tier." + x + ".Commands") != null) {
                    List<String> cmds = this.plugin.config.getStringList("Tier." + x + ".Commands");

                    for(String cmd:cmds) {
                        cmd = cmd.replaceAll("!player", player.getName());
                        this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(), cmd);
                    }
                }
                if(this.plugin.config.getBoolean("Main.Global Message is Enabled")) {
                    for(Player p:this.plugin.getServer().getOnlinePlayers()) {
                        if(p.equals(player)) continue;
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.config.getString("Main.Global Message").replaceAll("!amount", "" + amount).replaceAll("!playername", player.getDisplayName()).replaceAll("!type", DailyBonus.econ.currencyNamePlural())));
                    }
                }
                x = -666;
            }
        }

        File file = new File(this.plugin.getDataFolder().getAbsolutePath() + File.separator + "players" + File.separator + player.getName() + ".yml");
        YamlConfiguration pfile = YamlConfiguration.loadConfiguration(file);

        pfile.set("Time.Last", Long.valueOf(System.currentTimeMillis()));
        try {
            pfile.save(file);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        File file = new File(this.plugin.getDataFolder().getAbsolutePath() + File.separator + "players" + File.separator + player.getName() + ".yml");
        YamlConfiguration playerFile = YamlConfiguration.loadConfiguration(file);

        long login = playerFile.getLong("Time.Last");

        playerFile.set("Time.Last", Long.valueOf(System.currentTimeMillis()));

        if(this.plugin.playerList.contains(player.getName())) {
            playerFile.set("Time.Logged Early", true);
            this.plugin.playerList.remove(player.getName());

            if(plugin.numEarly.containsKey(player.getName())) {
                int already = plugin.numEarly.get(player.getName());
                plugin.numEarly.remove(player.getName());
                plugin.numEarly.put(player.getName(), already + 1);
            } else {
                plugin.numEarly.put(player.getName(), 1);
            }
        }

        Calendar current = Calendar.getInstance();
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(login);

        if((last.get(5) < current.get(5)) || (last.get(2) + 1 < current.get(2) + 1) || (last.get(1) < current.get(1))) {
            playerFile.set("Time.Logged Early", true);
        }
        try {
            playerFile.save(file);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkLastLogin(Player p) {
        File file = new File(this.plugin.getDataFolder().getAbsolutePath() + File.separator + "players" + File.separator + p.getName() + ".yml");
        YamlConfiguration playerFile;
        if(!file.exists()) {
            try {
                file.createNewFile();
                playerFile = YamlConfiguration.loadConfiguration(file);
                playerFile.set("Time.Last", Long.valueOf(System.currentTimeMillis()));
                playerFile.set("Time.Logged Early", Boolean.valueOf(false));
                playerFile.save(file);
            } catch(IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        playerFile = YamlConfiguration.loadConfiguration(file);

        if(playerFile.get("Time.Logged Early") != null) {
            if(playerFile.getBoolean("Time.Logged Early")) {
                playerFile.set("Time.Logged Early", Boolean.valueOf(false));
                try {
                    playerFile.save(file);
                } catch(IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }

        if(playerFile.get("Time.Last") != null) {
            Calendar current = Calendar.getInstance();
            Calendar last = Calendar.getInstance();
            last.setTimeInMillis(playerFile.getLong("Time.Last"));

            if((last.get(5) < current.get(5)) || (last.get(2) + 1 < current.get(2) + 1) || (last.get(1) < current.get(1))) {
                return true;
            }
        }
        return false;
    }
}