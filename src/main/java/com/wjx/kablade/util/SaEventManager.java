package com.wjx.kablade.util;

public class SaEventManager {
    public static void addSaEvent(SaEvent e){
        SaEventLister.events.add(e);
    }
}
