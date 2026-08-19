package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.client.FinLineFix;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Same fin line fix as the small model, see TropicalFishSmallModelMixin.
 */
@Mixin(TropicalFishLargeModel.class)
public class TropicalFishLargeModelMixin {

    @Inject(method = "createBodyLayer", at = @At("HEAD"), cancellable = true)
    private static void keepFinsFlat(CubeDeformation g,
            CallbackInfoReturnable<LayerDefinition> cir) {
        if (!FinLineFix.shouldFix(g)) {
            return;
        }
        CubeDeformation flatX = FinLineFix.withoutX(g);
        CubeDeformation flatZ = FinLineFix.withoutZ(g);

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-1.0F, -3.0F, -3.0F, 2.0F, 6.0F, 6.0F, g),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(21, 16)
                .addBox(0.0F, -3.0F, 0.0F, 0.0F, 6.0F, 5.0F, flatX),
                PartPose.offset(0.0F, 19.0F, 3.0F));
        root.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(2, 16)
                .addBox(-2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F, flatZ),
                PartPose.offsetAndRotation(-1.0F, 20.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(2, 12)
                .addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F, flatZ),
                PartPose.offsetAndRotation(1.0F, 20.0F, 0.0F, 0.0F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("top_fin", CubeListBuilder.create().texOffs(20, 11)
                .addBox(0.0F, -4.0F, 0.0F, 0.0F, 4.0F, 6.0F, flatX),
                PartPose.offset(0.0F, 16.0F, -3.0F));
        root.addOrReplaceChild("bottom_fin", CubeListBuilder.create().texOffs(20, 21)
                .addBox(0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 6.0F, flatX),
                PartPose.offset(0.0F, 22.0F, -3.0F));
        cir.setReturnValue(LayerDefinition.create(mesh, 32, 32));
    }
}
