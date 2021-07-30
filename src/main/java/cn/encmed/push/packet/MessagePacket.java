package cn.encmed.push.packet;

public class MessagePacket extends Packet {
    private Message message;

    @Override
    public byte getCmd() {
        return Command.MESSAGE;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
