package com.wjx.kablade.util;

public class MathFunc {
    public static float amplifierCalc(float amp,float factor){
        return (float)Math.log ((Math.abs(amp))*factor)*5;
    }
}
