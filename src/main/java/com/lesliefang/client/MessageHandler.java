package com.lesliefang.client;

import com.lesliefang.packet.HeartbeatPacket;
import com.lesliefang.packet.Message;
import com.lesliefang.packet.MessagePacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class MessageHandler extends SimpleChannelInboundHandler<MessagePacket> {
    private static final int RECONNECT_DELAY = 5;
    private Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private MessageEventListener messageEventListener;
    private Bootstrap bootstrap;

    public MessageHandler(Bootstrap bootstrap, MessageEventListener messageEventListener) {
        this.bootstrap = bootstrap;
        this.messageEventListener = messageEventListener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessagePacket msg) throws Exception {
        if (messageEventListener != null) {
            Message message = msg.getMessage();
            logger.debug("receive message {} {} ", message.getType(), message.getContent());
            messageEventListener.onReceiveMessage(message);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                HeartbeatPacket heartbeatPacket = new HeartbeatPacket();
                ctx.channel().writeAndFlush(heartbeatPacket);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().eventLoop().isShutdown() && !ctx.channel().eventLoop().isShuttingDown()) {
            logger.info("channelInactive {}", ctx.channel().remoteAddress());
            ctx.channel().eventLoop().schedule(new Runnable() {
                @Override
                public void run() {
                    InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    connect(socketAddress.getHostName(), socketAddress.getPort());
                }
            }, 5, TimeUnit.SECONDS);
        } else {
            logger.info("channelInactive {} and eventLoop shutdown", ctx.channel().remoteAddress());
        }
    }

    private void connect(String host, int port) {
        logger.info("try to connect {}:{}", host, port);
        ChannelFuture f = bootstrap.connect(host, port);
        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.info("connected to {}:{}", host, port);
            } else {
                EventLoopGroup group = bootstrap.config().group();
                if (!group.isShutdown() && !group.isShuttingDown()) {
                    logger.info("failed connect to {}:{}, schedule reconnect after {}s", host, port, RECONNECT_DELAY);
                    future.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            connect(host, port);
                        }
                    }, RECONNECT_DELAY, TimeUnit.SECONDS);
                } else {
                    logger.info("failed connect to {}:{} eventLoop shutdown", host, port);
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.channel().close();
    }
}
