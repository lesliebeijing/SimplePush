package com.lesliefang.client;

import com.lesliefang.packet.Message;

public interface MessageEventListener {
    void onReceiveMessage(Message message);
}
