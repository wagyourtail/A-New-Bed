package xyz.wagyourtail.randombedspawns.mixin;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.wagyourtail.randombedspawns.RandomBedSpawns;
import xyz.wagyourtail.randombedspawns.ServerLevelAccessor;

@Mixin(World.class)
public class WorldMixin {

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("RETURN"))
    private void onUpdateBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && this instanceof ServerLevelAccessor) {
            if (state.getBlock() instanceof BedBlock && state.get(BedBlock.PART) == BedPart.FOOT) {
                RandomBedSpawns.LOGGER.debug("[RandomBedSpawns] Bed placed at " + pos.toShortString());
                ((ServerLevelAccessor) this).addBed(pos);
            } else {
                if (((ServerLevelAccessor) this).removeBed(pos)) {
                    RandomBedSpawns.LOGGER.debug("[RandomBedSpawns] Bed removed at " + pos.toShortString());
                }
            }
        }
    }
}
