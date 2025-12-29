package com.factory.generators.utils;

import org.bukkit.ChatColor;

public class Utils {

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String formatTime(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public static String formatMaterial(String material) {
        String name = material.toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
}
