package com.example.mycommunity.actuator;

import com.example.mycommunity.util.CommunityUtil;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

///对数据库进行监控，暴露一个端点，可以通过端点来查看数据库的连接情况
@Component
@Endpoint(id="database")
public class DatabaseEndpoint {

    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(DatabaseEndpoint.class);

    @Autowired
    private DataSource dataSource;

    @ReadOperation
    public String checkConnection() {
        try (
                Connection conn = dataSource.getConnection();
        ) {
            return CommunityUtil.getJSONString(0, "获取连接成功!");
        } catch (SQLException e) {
            logger.error("获取连接失败:" + e.getMessage());
            return CommunityUtil.getJSONString(1, "获取连接失败!");
        }
    }
}
