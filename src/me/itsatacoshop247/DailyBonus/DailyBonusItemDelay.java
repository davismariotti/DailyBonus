package me.itsatacoshop247.DailyBonus;

import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DailyBonusItemDelay implements Runnable {
	public final DailyBonus plugin;
	private Player player;
	private int num;

	public DailyBonusItemDelay(DailyBonus instance, Player importPlayer,
			int importNum) {
		this.plugin = instance;
		this.player = importPlayer;
		this.num = importNum;
	}

	public void run() {
		if ((this.player.isOnline()) && (this.plugin.isEnabled()) && (!this.plugin.numEarly.containsKey(this.player.getName()))) {
			int amount = 0;
			String amt = this.plugin.config.getString("Tier." + this.num + ".Economy Amount");
			if (amt.split(";").length > 1) {
				amount = Integer.parseInt(amt.split(";")[0]) + (int) (Math.random() * (Integer.parseInt(amt.split(";")[1]) * 2) - Integer.parseInt(amt.split(";")[1]));
			} else {
				amount = this.plugin.config.getInt("Tier." + this.num + ".Economy Amount");
			}
			if (amount > 0) {
				EconomyResponse r;
				if (DailyBonus.econ != null) {
					r = DailyBonus.econ.depositPlayer(this.player.getName(), amount);
				} else {
					this.player.sendMessage(ChatColor.DARK_RED + "The DailyBonus plugin would have given you economy money, but the server doesn't have Vault enabled, or it is not enabled correctly!");
				}
			}
			this.player.sendMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.config.getString("Tier." + this.num + ".Message").replaceAll("!amount", "" + amount)));
			if (this.plugin.config.get("Tier." + this.num + ".Items") != null) {
				List<String> items = this.plugin.config.getStringList("Tier." + this.num + ".Items");
				for (String itemsline : items) {
					String[] line = itemsline.split(";");
					String[] data = itemsline.split("-");
					if (!line[0].equals("0")) {
						ItemStack is = new ItemStack(Material.getMaterial(Integer.parseInt(line[0])), Integer.parseInt(line[1]));
						if (data.length > 1) {
							is.setDurability(Short.parseShort(data[1]));
						}

						if (line.length > 2) {
							is.setAmount(Integer.parseInt(line[1]) + (int) (Math.random() * (Integer.parseInt(line[2].split("-")[0]) * 2) - Integer.parseInt(line[2].split("-")[0])));
						}

						if (this.player.getInventory().firstEmpty() < 0) {
							this.player.getWorld().dropItemNaturally(
									this.player.getEyeLocation(), is);
						} else {
							this.player.getInventory().addItem(new ItemStack[] {is});
						}
					}
				}
			}
			if (this.plugin.config.get("Tier." + this.num + ".Commands") != null) {
				List<String> cmds = this.plugin.config.getStringList("Tier." + this.num + ".Commands");

				for (String cmd : cmds) {
					cmd = cmd.replaceAll("!player", this.player.getName());
					this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(), cmd);
				}
			}
			if (this.plugin.config.getBoolean("Main.Global Message is Enabled")) {
				for (Player p : this.plugin.getServer().getOnlinePlayers()) {
					if (p.equals(this.player)) continue;
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.config.getString("Main.Global Message").replaceAll("!amount", "" + amount).replaceAll("!playername", this.player.getDisplayName()).replaceAll("!type", DailyBonus.econ.currencyNamePlural())));
				}
			}
		}

		this.plugin.playerList.remove(this.player.getName());

		if (this.plugin.numEarly.containsKey(this.player.getName())) {
			int num = ((Integer) this.plugin.numEarly.get(this.player.getName())).intValue();
			if (num <= 1) {
				this.plugin.numEarly.remove(this.player.getName());
			} else {
				this.plugin.numEarly.remove(this.player.getName());
				this.plugin.numEarly.put(this.player.getName(), Integer.valueOf(num - 1));
			}
		}
	}
}