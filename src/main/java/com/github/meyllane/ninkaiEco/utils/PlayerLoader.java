package com.github.meyllane.ninkaiEco.utils;

import com.lishid.openinv.OpenInv;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlayerLoader {

    public static Player getOrLoadPlayer(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) return null;
        return offlinePlayer.isConnected() ? offlinePlayer.getPlayer() : OpenInv.getPlugin(OpenInv.class).loadPlayer(offlinePlayer);
    }
}
