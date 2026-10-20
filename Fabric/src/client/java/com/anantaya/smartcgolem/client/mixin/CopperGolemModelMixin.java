package com.anantaya.smartcgolem.client.mixin;

import com.anantaya.smartcgolem.client.StretchRenderAccess;
import com.anantaya.smartcgolem.client.StretchKeyframes;
import net.minecraft.client.model.animal.golem.CopperGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperGolemModel.class)
public abstract class CopperGolemModelMixin {
    // CopperGolemModel 26.2 does not declare leftLeg/rightLeg fields.
    // Resolve the actual baked child bones through the public Model.root().
    // This avoids @Shadow mismatches that abort Minecraft resource reload.
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void scg$applyStretch(CopperGolemRenderState state, CallbackInfo ci) {
        float p = ((StretchRenderAccess) state).scg$getStretch();
        if (p <= 0f) return;
        StretchKeyframes.Pose pose = StretchKeyframes.sample(p);
        ModelPart root = ((CopperGolemModel)(Object)this).root();
        ModelPart body = root.getChild("body");
        ModelPart leftLeg = root.getChild("left_leg");
        ModelPart rightLeg = root.getChild("right_leg");

        // Java's model coordinates grow downward; Bedrock's grow upward.
        body.y -= pose.bodyY();
        body.yScale *= pose.bodyScale();
        leftLeg.y -= pose.legY();
        rightLeg.y -= pose.legY();
        leftLeg.yScale *= pose.legScale();
        rightLeg.yScale *= pose.legScale();
        // Preserve all vanilla arm/head animations.
    }
}
