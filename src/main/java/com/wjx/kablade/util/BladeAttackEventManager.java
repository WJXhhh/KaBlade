package com.wjx.kablade.util;

import com.google.common.collect.Lists;

import java.util.ArrayList;

public class BladeAttackEventManager {
    public static ArrayList<BladeAttackEvent> events = Lists.newArrayList();

    public static void addEvent(BladeAttackEvent event){
        events.add(event);
    }
}
