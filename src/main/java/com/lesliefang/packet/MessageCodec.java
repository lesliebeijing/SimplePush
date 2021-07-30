package com.lesliefang.packet;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageCodec extends ByteToMessageCodec<Packet> {
    private Logger logger = LoggerFactory.getLogger(MessageCodec.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) throws Exception {
        out.writeByte(msg.getCmd());
        byte[] data = JSON.toJSONBytes(msg);
        out.writeShort(data.length);
        out.writeBytes(data);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        byte cmd = in.readByte();
        int length = in.readUnsignedShort();
        byte[] data = new byte[length];
        ByteBuf dataBuf = in.readBytes(data);
        dataBuf.release();

        Packet packet = null;

        switch (cmd) {
            case Command.HEAT_BEAT:
                packet = JSON.parseObject(data, HeartbeatPacket.class);
                break;
            case Command.MESSAGE:
                packet = JSON.parseObject(data, MessagePacket.class);
                break;
            default:
                logger.error("unknown cmd {}", cmd);
                break;
        }

        if (packet != null) {
            out.add(packet);
        }
    }
}
