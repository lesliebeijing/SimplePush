package com.lesliefang.server;

import com.lesliefang.packet.Message;
import com.lesliefang.packet.MessageCodec;
import com.lesliefang.packet.MessagePacket;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushServer {
    private Logger logger = LoggerFactory.getLogger(PushServer.class);
    private NioEventLoopGroup boss;
    private NioEventLoopGroup worker;
    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public void run(int port) {
        boss = new NioEventLoopGroup();
        worker = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(50000, 1, 2));
                            ch.pipeline().addLast(new MessageCodec());
                            ch.pipeline().addLast(new IdleStateHandler(30, 0, 0));
                            ch.pipeline().addLast(new HeartBeatHandler());
                        }
                    });

            ChannelFuture f = serverBootstrap.bind(port).sync();

            logger.info("push server start at " + port);

            f.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    logger.info("push server close");
                    boss.shutdownGracefully();
                    worker.shutdownGracefully();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送消息到所有已连接客户端
     */
    public void publishToAll(Message message) {
        MessagePacket messagePacket = new MessagePacket();
        messagePacket.setMessage(message);
        channelGroup.writeAndFlush(messagePacket);
    }

    public void stop() {
        boss.shutdownGracefully();
        worker.shutdownGracefully();
    }

    public static void main(String[] args) {
        new PushServer().run(5000);
    }
}
