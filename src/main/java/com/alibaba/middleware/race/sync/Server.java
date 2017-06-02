package com.alibaba.middleware.race.sync;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 服务器类，负责push消息到client Created by wanshao on 2017/5/25.
 */
public class Server {

    // 保存channel
    private static Map<String, Channel> map = new ConcurrentHashMap<String, Channel>();
    // 接收评测程序的三个参数
    private static String schema;
    private static Map tableNamePkMap;

    public static Map<String, Channel> getMap() {
        return map;
    }

    public static void setMap(Map<String, Channel> map) {
        Server.map = map;
    }

    public static void main(String[] args) throws InterruptedException {
        initProperties();

        printInput(args);

        schema = args[0];
        JSONObject jsonObject = JSONObject.parseObject(args[1]);
        tableNamePkMap = JSONObject.parseObject(jsonObject.toJSONString());

        Logger logger = LoggerFactory.getLogger(Client.class);
        logger.info("schema:" + schema);
        // 打印下输入内容
        for (Object object : tableNamePkMap.entrySet()) {
            Entry<String, Long> entry = (Entry<String, Long>) object;
            logger.info("tableName:" + entry.getKey());
            logger.info("PrimaryKey:" + entry.getValue());

        }
        Server server = new Server();
        logger.info("com.alibaba.middleware.race.sync.Server is running....");

        server.startServer(5527);
    }

    /**
     * 打印赛题输入 赛题输入格式： middleware {"student":"1,2,3","teacher":"4,5,6"}
     * 上面表示，查询的schema为middleware，查询的表为student中主键为1，2，3的记录和teacher表中主键为4，5，6的记录
     */
    private static void printInput(String[] args) {
        // 第一个参数是Schema Name
        System.out.println("Schema:" + args[0]);

        for (int i = 1; i < args.length; i++) {
            String tableAndPkStr = args[i];

            // 去掉花括号
            tableAndPkStr = tableAndPkStr.substring(1, tableAndPkStr.length() - 1);
            // 获取键值对
            String[] keyValuePair = tableAndPkStr.replaceAll("[\\[\\]]", "").split("=");
            // 第一个
            String key = keyValuePair[0];
            String[] valueArray = keyValuePair[1].split(",");
            for (String s : valueArray) {
                System.out.println("Key:" + key + ",Value:" + s);

            }
        }
    }

    /**
     * 初始化系统属性
     */
    private static void initProperties() {
        System.setProperty("middleware.test.home", Constants.TESTER_HOME);
        System.setProperty("middleware.teamcode", Constants.TEAMCODE);
        System.setProperty("app.logging.level", Constants.LOG_LEVEL);
    }


    private void startServer(int port) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        // 注册handler
                        ch.pipeline().addLast(new ServerDemoInHandler());
                        // ch.pipeline().addLast(new ServerDemoOutHandler());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
