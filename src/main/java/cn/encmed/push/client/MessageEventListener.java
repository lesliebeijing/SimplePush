package cn.encmed.push.client;

import cn.encmed.push.packet.Message;

public interface MessageEventListener {
    void onReceiveMessage(Message message);
}
