package com.wjx.kablade;

public class Lib {


    public static int rgbToMetrication(int r,int g,int b){
        String i = Integer.toHexString(r);
        String j = Integer.toHexString(g);
        String k = Integer.toHexString(b);
        i = i +j +k;
        return Integer.parseInt(i,16);
    }

    public static final String NBT_SLOW_TIME = "enchantment_slow_time";
    public static final String NBT_SLOW_LEVEL = "enchantment_slow_level";
    public static final String NBT_ORIGIN_MOVEMENT = "origin_movement";





}
