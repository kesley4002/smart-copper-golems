package com.anantaya.smartcgolem.client.mixin;

import com.anantaya.smartcgolem.client.ClientStretchAnimations;
import com.anantaya.smartcgolem.client.StretchRenderAccess;
import net.minecraft.client.renderer.entity.CopperGolemRenderer;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperGolemRenderer.class)
public class CopperGolemRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void scg$extract(CopperGolem entity, CopperGolemRenderState state, float tickProgress, CallbackInfo ci) {
        ((StretchRenderAccess) state).scg$setStretch(ClientStretchAnimations.progress(entity.getId(), tickProgress));
    }
}
