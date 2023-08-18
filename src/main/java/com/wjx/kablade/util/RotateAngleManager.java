package com.wjx.kablade.util;

public class RotateAngleManager {
    public float angle = 0f;
    public float getAngle(){
        return this.angle;
    }

    public void rotate(float angleIn){
        if(angleIn >= 360){
            angle += (angleIn%360);
            angle = checkAndEditAngle(angle);
        }
        else if (angleIn <= -360){
            angle += (angleIn%360);
            angle = checkAndEditAngle(angle);
        }
        else {
            angle += angleIn;
            angle = checkAndEditAngle(angle);
        }
    }

    public static float checkAndEditAngle(float angle){
        float a = angle;
        if (a >= 360){
            a %= 360;
        }
        if (a < 0){
            a = 360 - a;
        }
        return a;
    }
}
