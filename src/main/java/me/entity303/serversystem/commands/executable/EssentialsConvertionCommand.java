package me.entity303.serversystem.commands.executable;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.Warps;
import com.earth2me.essentials.commands.WarpNotFoundException;
import me.entity303.serversystem.main.ServerSystem;
import me.entity303.serversystem.utils.MessageUtils;
import net.ess3.api.InvalidWorldException;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class EssentialsConvertionCommand extends MessageUtils implements CommandExecutor {
    private boolean starting = false;

    public EssentialsConvertionCommand(ServerSystem plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (!this.isAllowed(cs, "convertfromessentials")) {
            cs.sendMessage(this.getPrefix() + this.getNoPermission(this.Perm("convertfromessentials")));
            return true;
        }

        if (!this.starting) {
            cs.sendMessage(this.getPrefix() + this.getMessage("ConvertFromEssentials.WarnNotTested", label, cmd.getName(), cs, null));
            this.starting = true;
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.starting = false, 20 * 10);
            return true;
        }

        cs.sendMessage(this.getPrefix() + this.getMessage("ConvertFromEssentials.Start", label, cmd.getName(), cs, null));

        File essentialsDirectory = new File("plugins//Essentials");

        if (!essentialsDirectory.exists()) {
            cs.sendMessage(this.getPrefix() + this.getMessage("ConvertFromEssentials.Failed.NoDirectory", label, cmd.getName(), cs, null));
            return true;
        }

        Essentials essentials = (Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");

        File userDirectory = new File("plugins//Essentials//userdata");
        if (userDirectory.exists())
            for (File userData : userDirectory.listFiles())
                try {
                    User offlineUser = essentials.getUser(UUID.fromString(userData.getName().split("\\.")[0]));

                    if (offlineUser == null) {
                        this.plugin.error("User '" + userData.getName().split("\\.")[0] + "' is null?!");
                        continue;
                    }

                    this.plugin.getEconomyManager().setMoney(offlineUser.getBase(), offlineUser.getMoney().doubleValue());

                    File homeFile = new File("plugins//ServerSystem//Homes", offlineUser.getConfigUUID().toString() + ".yml");
                    FileConfiguration homeCfg = YamlConfiguration.loadConfiguration(homeFile);

                    boolean setHomes = false;

                    for (String home : offlineUser.getHomes())
                        try {
                            setHomes = true;
                            homeCfg.set("Homes." + home.toUpperCase(), offlineUser.getHome(home));
                        } catch (Exception e) {
                            e.printStackTrace();
                            cs.sendMessage(this.getPrefix() + this.getMessageWithStringTarget("ConvertFromEssentials.Failed.Unknown", label, cmd.getName(), cs, "homeSetting;" + offlineUser.getName()) + ";" + home);
                            return true;
                        }

                    if (setHomes) try {
                        homeCfg.save(homeFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        cs.sendMessage(this.getPrefix() + this.getMessageWithStringTarget("ConvertFromEssentials.Failed.Unknown", label, cmd.getName(), cs, "homeSaving;" + userData.getName()));
                        return true;
                    }

                    this.plugin.getVanish().setVanish(offlineUser.isVanished(), offlineUser.getConfigUUID());
                } catch (Exception e) {
                    this.plugin.error("Failed to process userdata '" + userData.getName() + "'!");
                }

        Warps warps = essentials.getWarps();

        if (warps != null)
            if (!warps.isEmpty())
                for (String warp : warps.getList())
                    try {
                        this.plugin.getWarpManager().addWarp(warp, warps.getWarp(warp));
                    } catch (WarpNotFoundException | InvalidWorldException e) {
                        e.printStackTrace();
                        cs.sendMessage(this.getPrefix() + this.getMessageWithStringTarget("ConvertFromEssentials.Failed.Unknown", label, cmd.getName(), cs, "warpSetting;" + warp));
                        return true;
                    }

        if (this.plugin != null)
            if (this.plugin.getVaultHookManager() != null)
                this.plugin.getVaultHookManager().hook(true);

        cs.sendMessage(this.getPrefix() + this.getMessage("ConvertFromEssentials.Finished", label, cmd.getName(), cs, null));
        return true;
    }
}
