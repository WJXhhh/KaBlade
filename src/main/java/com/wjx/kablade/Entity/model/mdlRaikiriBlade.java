package com.wjx.kablade.Entity.model;// Made with Blockbench 4.3.1
// Exported for Minecraft version 1.7 - 1.12
// Paste this class into your mod and generate all required imports


import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class mdlRaikiriBlade extends ModelBase {
	private final ModelRenderer bb_main;
	private final ModelRenderer cube_r1;
	private final ModelRenderer cube_r2;

	public mdlRaikiriBlade() {
		textureWidth = 64;
		textureHeight = 64;

		bb_main = new ModelRenderer(this);
		bb_main.setRotationPoint(0.0F, 24.0F, 0.0F);
		bb_main.cubeList.add(new ModelBox(bb_main, 19, 19, -1.0F, -10.0F, 8.0F, 2, 2, 15, 0.0F, false));
		bb_main.cubeList.add(new ModelBox(bb_main, 19, 2, -1.0F, -10.0F, -24.0F, 2, 2, 15, 0.0F, false));
		bb_main.cubeList.add(new ModelBox(bb_main, 36, 36, -3.0F, -10.0F, 12.0F, 6, 2, 2, 0.0F, false));
		bb_main.cubeList.add(new ModelBox(bb_main, 20, 36, -3.0F, -10.0F, -15.0F, 6, 2, 2, 0.0F, false));
		bb_main.cubeList.add(new ModelBox(bb_main, 10, 36, 12.0F, -10.0F, -3.0F, 2, 2, 6, 0.0F, false));
		bb_main.cubeList.add(new ModelBox(bb_main, 0, 34, -14.0F, -10.0F, -3.0F, 2, 2, 6, 0.0F, false));

		cube_r1 = new ModelRenderer(this);
		cube_r1.setRotationPoint(-2.0F, 0.0F, 0.0F);
		bb_main.addChild(cube_r1);
		setRotationAngle(cube_r1, 0.0F, -1.5708F, 0.0F);
		cube_r1.cubeList.add(new ModelBox(cube_r1, 0, 0, -1.0F, -10.0F, 6.0F, 2, 2, 15, 0.0F, false));

		cube_r2 = new ModelRenderer(this);
		cube_r2.setRotationPoint(2.0F, 0.0F, 0.0F);
		bb_main.addChild(cube_r2);
		setRotationAngle(cube_r2, 0.0F, -1.5708F, 0.0F);
		cube_r2.cubeList.add(new ModelBox(cube_r2, 0, 17, -1.0F, -10.0F, -21.0F, 2, 2, 15, 0.0F, false));
	}

	@Override
	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
		bb_main.render(f5);
	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.rotateAngleX = x;
		modelRenderer.rotateAngleY = y;
		modelRenderer.rotateAngleZ = z;
	}
}