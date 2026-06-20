package com.xciel.steamturbine.network;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.xciel.steamturbine.content.nd.NetworkDiagnoserBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public class DiagnoserEditPacket extends BlockEntityConfigurationPacket<NetworkDiagnoserBlockEntity> {
    public static final StreamCodec<ByteBuf, DiagnoserEditPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, packet -> packet.pos,
        ByteBufCodecs.FLOAT, packet -> packet.maxTestSU,
        ByteBufCodecs.BOOL, packet -> packet.stressTesting,
        DiagnoserEditPacket::new
    );

    private final float maxTestSU;
    private final boolean stressTesting;

    public static DiagnoserEditPacket create(BlockPos pos, float maxTestSU, boolean stressTesting) {
        return new DiagnoserEditPacket(pos, maxTestSU, stressTesting);
    }

    public DiagnoserEditPacket(BlockPos pos, float maxTestSU, boolean stressTesting) {
        super(pos);
        this.maxTestSU = maxTestSU;
        this.stressTesting = stressTesting;
    }

    @Override
    protected void applySettings(ServerPlayer player, NetworkDiagnoserBlockEntity be) {
        if (maxTestSU != be.getMaxTestSU()) {
            be.setMaxTestSU(maxTestSU);
        }

        if (stressTesting != be.isStressTesting()) {
            be.toggleStressTesting();
        }
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return SteamTurbinePackets.CONFIGURE_DIAGNOSER;
    }
}