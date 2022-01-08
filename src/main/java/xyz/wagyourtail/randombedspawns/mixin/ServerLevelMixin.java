package xyz.wagyourtail.randombedspawns.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.randombedspawns.EmptyNetworkHandler;
import xyz.wagyourtail.randombedspawns.RandomBedSpawns;
import xyz.wagyourtail.randombedspawns.ServerLevelAccessor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ServerWorld.class)
public abstract class ServerLevelMixin implements ServerLevelAccessor {
    @Unique
    private final Set<BlockPos> bedPositions = ConcurrentHashMap.newKeySet();

    @Unique
    private Path bedFile;

    @Shadow
    public abstract BlockPos locateStructure(StructureFeature<?> feature, BlockPos pos, int radius, boolean skipExistingChunks);

    @Shadow @Final private ServerChunkManager chunkManager;

    @Shadow public abstract @NotNull MinecraftServer getServer();

    @Shadow public abstract void removePlayer(ServerPlayerEntity player, Entity.RemovalReason reason);

    @Shadow protected abstract void addPlayer(ServerPlayerEntity player);

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<World> worldKey, DimensionType dimensionType, WorldGenerationProgressListener worldGenerationProgressListener, ChunkGenerator chunkGenerator, boolean debugWorld, long seed, List spawners, boolean shouldTickTime, CallbackInfo ci) {
        bedFile = session.getWorldDirectory(worldKey).resolve("beds.dat");
        if (Files.exists(bedFile)) {
            try {
                ByteBuffer buff = ByteBuffer.wrap(Files.readAllBytes(bedFile));
                while (buff.remaining() >= Integer.BYTES * 3) {
                    int x = buff.getInt();
                    int y = buff.getInt();
                    int z = buff.getInt();
                    addBed(new BlockPos(x, y, z));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;save(Z)V"))
    public void onWorldSave(ProgressListener progressListener, boolean flush, boolean savingDisabled, CallbackInfo ci) {
        try {
            Files.createDirectories(bedFile.getParent());
            ByteBuffer buff = ByteBuffer.allocate(Integer.BYTES * 3 * bedPositions.size());
            for (BlockPos pos : bedPositions) {
                buff.putInt(pos.getX());
                buff.putInt(pos.getY());
                buff.putInt(pos.getZ());
            }
            Files.write(bedFile, buff.array(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<BlockPos> getBeds() {
        return bedPositions;
    }

    @Override
    public boolean addBed(BlockPos pos) {
        return bedPositions.add(pos);
    }

    @Override
    public boolean removeBed(BlockPos pos) {
        return bedPositions.remove(pos);
    }

    @Override
    public AtomicBoolean loadVillagesInRange(BlockPos pos, int range) {
        AtomicBoolean interrupted = new AtomicBoolean(false);
        CompletableFuture.runAsync(() -> {
            BlockPos village;
            ServerPlayerEntity watcher = new ServerPlayerEntity(this.getServer(), (ServerWorld) (Object) this, new GameProfile(UUID.randomUUID(), "RBS"));
            watcher.interactionManager.changeGameMode(GameMode.SPECTATOR);
            new EmptyNetworkHandler(this.getServer(), watcher);
            addPlayer(watcher);
            // TODO: figure out how to get features to load without a watcher, this is scuffed
            try {
                while (true) {
                    village = this.locateStructure(StructureFeature.VILLAGE, pos, range, true);
                    if (village == null)
                        break;
                    if (pos.getSquaredDistance(village) > range * range)
                        break;
                    RandomBedSpawns.LOGGER.debug("loading village at " + village.toShortString());
                    watcher.setPosition(village.getX(), village.getY(), village.getZ());
                    chunkManager.updatePosition(watcher);
                    if (interrupted.get()) break;
                }
            } finally {
                watcher.remove(Entity.RemovalReason.DISCARDED);
                removePlayer(watcher, Entity.RemovalReason.DISCARDED);
            }
        });
        return interrupted;
    }

}
