package com.anantaya.smartcgolem.client.mixin;

import com.anantaya.smartcgolem.client.StretchRenderAccess;
import com.anantaya.smartcgolem.client.StretchKeyframes;
import net.minecraft.client.model.animal.golem.CopperGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperGolemModel.class)
public abstract class CopperGolemModelMixin {
    @Shadow @Final private ModelPart body;
    @Shadow @Final private ModelPart leftLeg;
    @Shadow @Final private ModelPart rightLeg;

    // After vanilla setupAnim: preserve upper body rotation and vanilla
    // chest interaction; affect only body translation and leg transforms.
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void scg$applyStretch(CopperGolemRenderState state, CallbackInfo ci) {
        float p = ((StretchRenderAccess) state).scg$getStretch();
        if (p <= 0f) return;
        StretchKeyframes.Pose pose = StretchKeyframes.sample(p);
        // Bedrock Y points upwards, Java ModelPart Y points downwards.
        this.body.y -= pose.bodyY();
        this.body.yScale *= pose.bodyScale();
        this.leftLeg.y -= pose.legY();
        this.rightLeg.y -= pose.legY();
        this.leftLeg.yScale *= pose.legScale();
        this.rightLeg.yScale *= pose.legScale();
        // Do not touch arms or head: all vanilla animations still run.
    }
}
