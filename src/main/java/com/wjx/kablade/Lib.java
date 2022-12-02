package com.wjx.kablade;

public class Lib {
    public static int rgbToMetrication(int r,int g,int b){
        String i = Integer.toHexString(r);
        String j = Integer.toHexString(g);
        String k = Integer.toHexString(b);
        i = i +j +k;
        return Integer.parseInt(i,16);
    }
}
