package xyz.wagyourtail.randombedspawns;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class EmptyNetworkHandler extends ServerPlayNetworkHandler {
    public EmptyNetworkHandler(MinecraftServer server, ServerPlayerEntity player) {
        super(server, new ClientConnection(NetworkSide.CLIENTBOUND), player);
    }

    @Override
    public void sendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> listener) {
        // NO-OP
    }

}
