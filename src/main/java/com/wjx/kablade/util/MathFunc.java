package com.wjx.kablade.util;

public class MathFunc {
    public static float amplifierCalc(float amp,float factor){
        if(Math.abs(amp)<=0.5){
            amp= (float) (Math.abs(amp) +0.5);
        }
        float z =(float)Math.log ((Math.abs(amp)+0.5)*4)*factor;
        return z;
    }
}
