package com.tencent.wxcloudrun.config;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.MasterSlaveServersConfig;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author marsmello
 * @date 2024/11/12
 * @desc redisson配置
 */
@Configuration
public class RedissonConfig {
    @Value("${spring.redis.host}")
    private String host;

	@Value("${spring.redis.port:6379}")
	private String port;

    @Value("${spring.redis.password:}")
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
		MasterSlaveServersConfig masterSlaveServersConfig = config.useMasterSlaveServers();
		masterSlaveServersConfig.setMasterAddress(prefixAddress(host));
        if (StringUtils.isNotBlank(password)) {
			masterSlaveServersConfig.setPassword(password);
        }
        return Redisson.create(config);
    }

    private String prefixAddress(String host) {
		String address = host + ":" + port;
        if (StringUtils.isNotBlank(address) && !address.startsWith("redis://")) {
            return "redis://" + address;
        }
        return address ;
    }
}
