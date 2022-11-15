package com.wjx.kablade.event;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.util.text.TextFormatting;

public class UpdateColor {
    private static final TextFormatting[] colour1 ;
    private static final ChatFormatting[] colour2 ;
    private static final ChatFormatting[] colour3 ;
    private static final ChatFormatting[] colour4 ;
    private static final ChatFormatting[] colour5 ;
    private static final ChatFormatting[] colourred ;
    private static final ChatFormatting[] colourRainBow = new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE};
    private static final ChatFormatting[] coloursanic = new ChatFormatting[]{ChatFormatting.BLUE, ChatFormatting.BLUE, ChatFormatting.BLUE, ChatFormatting.BLUE, ChatFormatting.WHITE, ChatFormatting.BLUE, ChatFormatting.WHITE, ChatFormatting.WHITE, ChatFormatting.BLUE, ChatFormatting.WHITE, ChatFormatting.WHITE, ChatFormatting.BLUE, ChatFormatting.RED, ChatFormatting.WHITE, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY};

    private static final ChatFormatting[] colourpur;

    public static String makeColour1(String input) {
        return formatting(input, colour1, 80.0D);
    }
    public static String makeColour2(String input) {
        return formatting(input, colour2, 80.0D);
    }
    public static String makeColour3(String input) {
        return formatting(input, colour3, 80.0D);
    }
    public static String makeColour4(String input) {
        return formatting(input, colour4, 80.0D);
    }
    public static String makeColour5(String input) {
        return formatting(input, colour5, 80.0D);
    }
    public static String makeColourRed(String input) {
        return formatting(input, colourred, 80.0D);
    }
    public static String makeColourPur(String input) {
        return formatting(input, colourpur, 80.0D);
    }
    public static String makeColourSanic(String input) {
        return formatting(input, coloursanic, 50.0D);
    }
    public static String makeColourRainbow(String input) {
        return formatting(input, colourRainBow, 80.0D);
    }
    public static String formatting(String input, TextFormatting[] colours, double delay) {
        StringBuilder sb = new StringBuilder(input.length() * 3);
        if (delay <= 0.0D) {
            delay = 0.001D;
        }

        int offset = (int)Math.floor((double)(System.currentTimeMillis() & 16383L) / delay) % colours.length;

        for(int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            int col = (i + colours.length - offset) % colours.length;
            sb.append(colours[col].toString());
            sb.append(c);
        }

        return sb.toString();
    }
    public static String formatting(String input, ChatFormatting[] colours, double delay) {
        StringBuilder sb = new StringBuilder(input.length() * 3);
        if (delay <= 0.0D) {
            delay = 0.001D;
        }

        int offset = (int)Math.floor((double)(System.currentTimeMillis() & 16383L) / delay) % colours.length;

        for(int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            int col = (i + colours.length - offset) % colours.length;
            sb.append(colours[col].toString());
            sb.append(c);
        }

        return sb.toString();
    }
    static {

        colour1= new TextFormatting[]{TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.YELLOW, TextFormatting.WHITE, TextFormatting.YELLOW, TextFormatting.GOLD};
        colour2 = new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.GOLD, ChatFormatting.GOLD, ChatFormatting.GOLD, ChatFormatting.GOLD, ChatFormatting.GOLD, ChatFormatting.GOLD, ChatFormatting.GOLD, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.WHITE, ChatFormatting.YELLOW, ChatFormatting.GOLD};
        colour3 = new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.BLUE, ChatFormatting.RED, ChatFormatting.WHITE, ChatFormatting.GRAY};
        colour4 = new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_RED, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_AQUA, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_GRAY};
        colour5 = new ChatFormatting[]{ChatFormatting.DARK_BLUE, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_BLUE, ChatFormatting.BLUE, ChatFormatting.AQUA, ChatFormatting.WHITE, ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.DARK_BLUE};
        colourred = new ChatFormatting[]{ ChatFormatting.DARK_RED, ChatFormatting.DARK_RED, ChatFormatting.DARK_RED, ChatFormatting.DARK_RED, ChatFormatting.DARK_RED, ChatFormatting.RED, ChatFormatting.WHITE, ChatFormatting.RED, ChatFormatting.DARK_RED};
        colourpur = new ChatFormatting[]{ ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_PURPLE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.WHITE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_PURPLE};
    }
}
