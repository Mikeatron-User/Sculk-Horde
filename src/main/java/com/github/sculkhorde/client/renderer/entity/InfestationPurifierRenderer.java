package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.InfestationPurifierModel;
import com.github.sculkhorde.client.model.enitity.SculkSporeSpewerModel;
import com.github.sculkhorde.common.entity.InfestationPurifierEntity;
import com.github.sculkhorde.common.entity.SculkSporeSpewerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

public class InfestationPurifierRenderer extends GeoEntityRenderer<InfestationPurifierEntity> {

    public InfestationPurifierRenderer(EntityRenderDispatcher renderManager)
    {
        super(renderManager, new InfestationPurifierModel());
        this.shadowStrength = 0.7F; //change 0.7 to the desired shadow size.
    }

    @Override
    public RenderType getRenderType(InfestationPurifierEntity animatable, float partialTicks, PoseStack stack, @Nullable MultiBufferSource renderTypeBuffer, @Nullable VertexConsumer vertexBuilder, int packedLightIn, ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }


}