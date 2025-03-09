package com.github.meyllane.ninkaiEco.utils;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.enums.ErrorMessage;
import com.github.meyllane.ninkaiEco.error.NinkaiEcoError;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.entity.Player;

public class ErrorHandler {
    private static final BukkitAudiences adventure = NinkaiEco.getPlugin(NinkaiEco.class).adventure();

    public static <T, U extends Player> void wrap(ThrowingFunction<T, U> function, T t, U u) {
        try {
            function.apply(t, u);
        } catch (NinkaiEcoError e) {
            adventure.player(u).sendMessage(
                    PluginComponentProvider.getErrorComponent(e)
            );
        } catch (Exception e) {
            adventure.player(u).sendMessage(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.UNKNOWN_ERROR.message)
            );
            e.printStackTrace();
        }
    }
}
