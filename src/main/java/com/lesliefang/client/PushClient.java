package com.lesliefang.client;

import com.lesliefang.packet.MessageCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class PushClient {
    private Logger logger = LoggerFactory.getLogger(PushClient.class);
    private Bootstrap b = new Bootstrap();
    private EventLoopGroup group = new NioEventLoopGroup();

    public PushClient() {
        this(null);
    }

    public PushClient(MessageEventListener messageEventListener) {
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(50000, 1, 2));
                        ch.pipeline().addLast(new MessageCodec());
                        ch.pipeline().addLast(new IdleStateHandler(0, 10, 0, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new MessageHandler(b, messageEventListener));
                    }
                });
    }

    public void connect(String host, int port) {
        logger.info("try to connect {}:{}", host, port);
        b.connect(host, port).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    logger.info("connected to {}:{}", host, port);
                } else {
                    // 连接不成功，5秒后重新连接
                    logger.info("failed connect to {}:{}, schedule reconnect after {}s", host, port, 5);
                    future.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            connect(host, port);
                        }
                    }, 5, TimeUnit.SECONDS);
                }
            }
        });
    }

    public void stop() {
        group.shutdownGracefully();
    }

    public static void main(String[] args) {
        PushClient pushClient = new PushClient();
        pushClient.connect("127.0.0.1", 5000);
    }
}
