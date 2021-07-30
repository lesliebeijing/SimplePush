package cn.encmed.push.packet;

public class HeartbeatPacket extends Packet {
    @Override
    public byte getCmd() {
        return Command.HEAT_BEAT;
    }
}
