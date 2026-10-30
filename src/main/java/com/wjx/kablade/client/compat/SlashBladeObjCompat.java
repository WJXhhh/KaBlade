package com.wjx.kablade.client.compat;

import java.lang.reflect.Field;
import java.util.List;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject;

/** Allows the rendering paths to run when older SlashBlade versions lack the faces field. */
public final class SlashBladeObjCompat {
    private static final Field GROUP_FACES = findGroupFacesField();

    private SlashBladeObjCompat() {}

    @SuppressWarnings("unchecked")
    public static List<Face> faces(GroupObject group) {
        if (group == null || GROUP_FACES == null) return List.of();
        try {
            Object faces = GROUP_FACES.get(group);
            return faces instanceof List<?> ? (List<Face>) faces : List.of();
        } catch (IllegalAccessException e) {
            return List.of();
        }
    }

    private static Field findGroupFacesField() {
        try {
            return GroupObject.class.getField("faces");
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
