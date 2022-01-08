package xyz.wagyourtail.randombedspawns.mixin;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.wagyourtail.randombedspawns.RandomBedSpawns;
import xyz.wagyourtail.randombedspawns.ServerLevelAccessor;

@Mixin(ChunkRegion.class)
public class ChunkRegionMixin {

    @Shadow @Final private ServerWorld world;

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("RETURN"))
    private void onUpdateBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            if (state.getBlock() instanceof BedBlock && state.get(BedBlock.PART) == BedPart.FOOT) {
                RandomBedSpawns.LOGGER.debug("[RandomBedSpawns] Bed placed at " + pos.toShortString());
//                throw new RuntimeException();
                ((ServerLevelAccessor) world).addBed(pos);
            } else {
                if (((ServerLevelAccessor) world).removeBed(pos)) {
                    RandomBedSpawns.LOGGER.debug("[RandomBedSpawns] Bed removed at " + pos.toShortString());
                }
            }
        }
    }
}
