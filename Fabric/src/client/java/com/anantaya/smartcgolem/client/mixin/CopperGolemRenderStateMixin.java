package com.anantaya.smartcgolem.client.mixin;

import com.anantaya.smartcgolem.client.StretchRenderAccess;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CopperGolemRenderState.class)
public class CopperGolemRenderStateMixin implements StretchRenderAccess {
    @Unique private float scg$progress;
    @Override public void scg$setStretch(float value) { scg$progress = value; }
    @Override public float scg$getStretch() { return scg$progress; }
}
