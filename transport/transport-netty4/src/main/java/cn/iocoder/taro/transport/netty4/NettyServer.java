package cn.iocoder.taro.transport.netty4;

import cn.iocoder.taro.rpc.core.common.TaroConstants;
import cn.iocoder.taro.rpc.core.transport.Channel;
import cn.iocoder.taro.rpc.core.transport.Codec;
import cn.iocoder.taro.rpc.core.transport.exception.TransportException;
import cn.iocoder.taro.rpc.core.transport.exchange.ExchangeHandler;
import cn.iocoder.taro.rpc.core.transport.exchange.Request;
import cn.iocoder.taro.rpc.core.transport.support.AbstractServer;
import cn.iocoder.taro.transport.netty4.codec.NettyDecoder;
import cn.iocoder.taro.transport.netty4.codec.NettyEncoder;
import cn.iocoder.taro.transport.netty4.heartbeat.ServerHeartbeatHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class NettyServer extends AbstractServer {

    private ServerBootstrap bootstrap;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerNettyChannelManager channelManager;

    public NettyServer(int port, ExchangeHandler exchangeHandler) {
        super(port, exchangeHandler);
    }

    public NettyServer(int port, ExchangeHandler exchangeHandler, Codec codec) {
        super(port, exchangeHandler, codec);
    }

    @Override
    public void open() {
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
        this.channelManager = new ServerNettyChannelManager();

        ServerBootstrap bootstrap = new ServerBootstrap();

        try {
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast("decoder", new NettyDecoder(codec))
                                    .addLast("encoder", new NettyEncoder(codec))
                                    .addLast("manager", channelManager)
                                    .addLast("idleState", new IdleStateHandler(0, 0, TaroConstants.TRANSPORT_SERVER_IDLE, TimeUnit.MILLISECONDS))
                                    .addLast("heartbeat", new ServerHeartbeatHandler())
                                    .addLast("handler", new NettyChannelHandler(messageHandler));
                        }
                    });

            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

            //绑定端口, 同步等待成功;
            ChannelFuture future = bootstrap.bind(port).sync(); // TODO close 添加
            //等待服务端监听端口关闭
//            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

        }
    }

    @Override
    public Collection<Channel> getChannels() {
        return channelManager.getChannels();
    }

    @Override
    public Channel getChannel(InetSocketAddress address) {
        return channelManager.getChannel(address);
    }

    @Override
    public void close() {
        // 尝试发送关闭消息
        for (Channel channel : getChannels()) {
            try {
                channel.oneway(Request.DATA_EVENT_READ_ONLY, 10000); // TODO 芋艿，超时
            } catch (TransportException e) {
                e.printStackTrace();
            }
        }

        //优雅关闭 线程组
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

//    public static void main(String[] args) {
//        Server server = new NettyServer(8080, new ExchangeHandler() {
//            @Override
//            public Response reply(Request request) {
//                System.out.println("接收到消息：" + request);
//                if (request.isOneway()) {
//                    System.out.println("oneway 消息，不进行响应");
//                    return null;
//                }
//
//                Response response = new Response(request.getId());
//                response.setEvent(false);
//                response.setStatus(Response.STATUS_SUCCESS);
//                if (request.getData().equals("\"hello\"")) {
//                    response.setData("world");
//                } else {
//                    response.setData("unknown");
//                }
//
//                return response;
//            }
//        });
////        try {
////            Thread.sleep(10 * 1000L);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
////        System.out.println("开始关闭...");
////        server.close();
////        System.out.println("启动完成");
//    }

}