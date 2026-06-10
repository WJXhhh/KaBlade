package com.wjx.kablade.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code RegistryObject<Item>} field whose item should be rendered with SlashBlade's
 * {@link mods.flammpfeil.slashblade.client.renderer.model.BladeModel} instead of a plain item model.
 *
 * <p>Processed client-side in {@code KbladeClientEvents#onModifyBakingResult}, which reflects over
 * the declaring registry class, finds the annotated fields, and wraps each item's baked inventory
 * model. Must be {@link RetentionPolicy#RUNTIME} so the client handler can read it via reflection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CustomBladeModel {
}
