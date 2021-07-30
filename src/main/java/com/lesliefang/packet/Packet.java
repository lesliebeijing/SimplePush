package com.lesliefang.packet;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * cmd(1字节)length(2字节)data(json数据)
 */
public abstract class Packet {
    @JSONField(serialize = false)
    public abstract byte getCmd();
}
