package xyz.wagyourtail.randombedspawns;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class FakeServerPlayer extends ServerPlayerEntity {
    public FakeServerPlayer(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile);
    }

    @Override
    public void unsetRemoved() {
        super.unsetRemoved();
    }

}
