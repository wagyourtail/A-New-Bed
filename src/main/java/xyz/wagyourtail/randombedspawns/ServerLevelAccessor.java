package xyz.wagyourtail.randombedspawns;

import net.minecraft.util.math.BlockPos;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ServerLevelAccessor {

    Set<BlockPos> getBeds();

    boolean addBed(BlockPos pos);

    boolean removeBed(BlockPos pos);

    void loadNextVillage(BlockPos pos, int range);

}
