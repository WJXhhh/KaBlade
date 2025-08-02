package com.wjx.kablade.util;

import com.wjx.kablade.config.ModConfig;

public class GraphicQuality {
    public static float sFloat(float f1,float f2){
        if(ModConfig.GeneralConf.Ultra_Effect){
            return f2;
        }
        return f1;
    }
    public static int sInt(int i1,int i2){
        if(ModConfig.GeneralConf.Ultra_Effect){
            return i2;
        }
        return i1;
    }
}
