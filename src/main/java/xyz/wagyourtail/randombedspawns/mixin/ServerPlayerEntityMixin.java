package xyz.wagyourtail.randombedspawns.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.wagyourtail.randombedspawns.RandomBedSpawns;
import xyz.wagyourtail.randombedspawns.ServerLevelAccessor;

import java.util.List;
import java.util.Random;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    @Shadow public abstract ServerWorld getWorld();

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Inject(method = "wakeUp", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerPlayerEntity;networkHandler:Lnet/minecraft/server/network/ServerPlayNetworkHandler;", ordinal = 0))
    private void onWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        if (!updateSleepingPlayers || this.getWorld().isDay()) {
            RandomBedSpawns.LOGGER.info("naturally waking up player");
            // get a random bed
            List<BlockPos> l = List.copyOf(((ServerLevelAccessor) getWorld()).getBeds());
            int i = new Random().nextInt(l.size());
            BlockPos pos = l.get(i);

            // teleport player to bed
            Vec3d vec3dx = (Vec3d) BedBlock.findWakeUpPosition(this.getType(), this.world, pos, this.getYaw()).orElseGet(() -> {
                BlockPos blockPos2 = pos.up();
                return new Vec3d((double)blockPos2.getX() + 0.5, (double)blockPos2.getY() + 0.1, (double)blockPos2.getZ() + 0.5);
            });
            Vec3d vec3d2 = Vec3d.ofBottomCenter(pos).subtract(vec3dx).normalize();
            float f = (float) MathHelper.wrapDegrees(MathHelper.atan2(vec3d2.z, vec3d2.x) * 180.0F / (float)Math.PI - 90.0);
            this.setPosition(vec3dx.x, vec3dx.y, vec3dx.z);
            this.setYaw(f);
            this.setPitch(0.0F);
        } else {
            RandomBedSpawns.LOGGER.info("player woke up early");
        }
    }

    @Inject(method = "trySleep", at = @At("RETURN"))
    private void onSleep(BlockPos pos, CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> cir) {
        cir.getReturnValue().ifRight(e -> {
            RandomBedSpawns.LOGGER.info("sleeping player");
            // load villages around player
        });
    }

    @Unique
    private int villageBedTickCount = 0;

    @Inject(method = "playerTick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.isSleeping()) {
            if (++villageBedTickCount % 5 == 0) {
                ((ServerLevelAccessor) this.getWorld()).loadNextVillage(this.getBlockPos(), 5000);
            }
        }
    }
}
