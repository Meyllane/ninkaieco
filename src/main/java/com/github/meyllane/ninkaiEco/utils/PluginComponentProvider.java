package com.github.meyllane.ninkaiEco.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class PluginComponentProvider {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static Component getPluginHeader() {
        return mm.deserialize("<color:white>[<gradient:#FFE259:#FFA751>NinkaiEco</gradient>] ");
    }

    public static Component getErrorComponent(RuntimeException e) {
        return getPluginHeader()
                .append(
                        Component.text(e.getMessage())
                                .color(TextColor.fromHexString("#df4b4b"))
                );
    }

    public static Component getErrorComponent(String errorMessage) {
        return getPluginHeader()
                .append(
                        Component.text(errorMessage)
                                .color(TextColor.fromHexString("#df4b4b"))
                );
    }
}
