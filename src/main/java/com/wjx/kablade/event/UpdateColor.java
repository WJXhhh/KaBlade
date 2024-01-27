package com.wjx.kablade.event;

import net.minecraft.util.text.TextFormatting;

public class UpdateColor {
    private static final TextFormatting[] colour1 ;
    private static final TextFormatting[] colour2 ;
    private static final TextFormatting[] colour3 ;
    private static final TextFormatting[] colour4 ;
    private static final TextFormatting[] colour5 ;
    private static final TextFormatting[] colourred ;
    private static final TextFormatting[] colourRainBow = new TextFormatting[]{TextFormatting.RED, TextFormatting.GOLD, TextFormatting.YELLOW, TextFormatting.GREEN, TextFormatting.AQUA, TextFormatting.BLUE, TextFormatting.LIGHT_PURPLE};
    private static final TextFormatting[] coloursanic = new TextFormatting[]{TextFormatting.BLUE, TextFormatting.BLUE, TextFormatting.BLUE, TextFormatting.BLUE, TextFormatting.WHITE, TextFormatting.BLUE, TextFormatting.WHITE, TextFormatting.WHITE, TextFormatting.BLUE, TextFormatting.WHITE, TextFormatting.WHITE, TextFormatting.BLUE, TextFormatting.RED, TextFormatting.WHITE, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY};

    private static final TextFormatting[] colourpur;

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
    static {

        colour1= new TextFormatting[]{TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.YELLOW, TextFormatting.WHITE, TextFormatting.YELLOW, TextFormatting.GOLD};
        colour2 = new TextFormatting[]{TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.GOLD, TextFormatting.YELLOW, TextFormatting.WHITE, TextFormatting.YELLOW, TextFormatting.GOLD};
        colour3 = new TextFormatting[]{TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.BLUE, TextFormatting.RED, TextFormatting.WHITE, TextFormatting.GRAY};
        colour4 = new TextFormatting[]{TextFormatting.RED, TextFormatting.GOLD, TextFormatting.YELLOW, TextFormatting.GREEN, TextFormatting.AQUA, TextFormatting.BLUE, TextFormatting.LIGHT_PURPLE, TextFormatting.DARK_RED, TextFormatting.DARK_GREEN, TextFormatting.DARK_AQUA, TextFormatting.DARK_BLUE, TextFormatting.DARK_PURPLE, TextFormatting.DARK_GRAY};
        colour5 = new TextFormatting[]{TextFormatting.DARK_BLUE, TextFormatting.DARK_BLUE, TextFormatting.DARK_BLUE, TextFormatting.DARK_BLUE, TextFormatting.DARK_BLUE, TextFormatting.DARK_BLUE, TextFormatting.DARK_BLUE, TextFormatting.BLUE, TextFormatting.AQUA, TextFormatting.WHITE, TextFormatting.AQUA, TextFormatting.BLUE, TextFormatting.DARK_BLUE};
        colourred = new TextFormatting[]{ TextFormatting.DARK_RED, TextFormatting.DARK_RED, TextFormatting.DARK_RED, TextFormatting.DARK_RED, TextFormatting.DARK_RED, TextFormatting.RED, TextFormatting.WHITE, TextFormatting.RED, TextFormatting.DARK_RED};
        colourpur = new TextFormatting[]{ TextFormatting.DARK_PURPLE, TextFormatting.DARK_PURPLE, TextFormatting.DARK_PURPLE, TextFormatting.DARK_PURPLE, TextFormatting.DARK_PURPLE, TextFormatting.LIGHT_PURPLE, TextFormatting.WHITE, TextFormatting.LIGHT_PURPLE, TextFormatting.DARK_PURPLE};
    }
}
