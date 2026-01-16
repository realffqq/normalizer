package club.polarite.normalizer.mixin;

import club.polarite.normalizer.config.ConfigManager;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.SharedConstants;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * Fixes sprints not being cancelled properly (like in 1.21.4 & <=1.14.1)
 */
@Mixin(LocalPlayer.class)
public abstract class SprintResetMixin extends AbstractClientPlayer {
    public SprintResetMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Shadow
    public abstract boolean isMovingSlowly();

    @Unique
    private boolean shouldStopSprinting() {
        return this.isMovingSlowly() || this.isPassenger() && !this.isRidingCamel() || this.isUsingItem() && !this.isPassenger() && !this.isUnderWater();
    }

    @Unique
    private boolean isRidingCamel() {
        return this.getVehicle() != null && this.getVehicle().getType() == EntityType.CAMEL;
    }

    @Unique
    String version = SharedConstants.getCurrentVersion().name(); // .getName() in 1.21.4-5, automatically gonna be changed at build-time by gradle 👍️

    @Inject(
            method = "aiStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/ClientInput;tick()V",
                    shift = At.Shift.AFTER
            )
    )
    private void sprintReset(CallbackInfo ci) {
        boolean restoreSprintCancel = ConfigManager.getConfig().restoreSprintCancel;
        if (this.shouldStopSprinting() && ConfigManager.isWhitelisted && restoreSprintCancel && !Objects.equals(version, "1.21.4")) {
            this.setSprinting(false);
        }
    }
}