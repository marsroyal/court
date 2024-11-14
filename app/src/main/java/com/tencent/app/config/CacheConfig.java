package com.tencent.app.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.github.benmanes.caffeine.cache.CaffeineSpec;

import com.tencent.common.constants.CacheConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.cache.CacheProperties.Redis;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.stream.Collectors;

/**
 * @author wanghaomin001
 * @date 2024/3/29
 * @desc 缓存配置
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties({CacheProperties.class})
@EnableRedisRepositories
public class CacheConfig {
	private String redisHost;
	private String redisPassword;
	private ObjectMapper objectMapper;
	private CacheProperties cacheProperties;

	@Value("${cache.caffeine.spec.short:maximumSize=1000,expireAfterWrite=60s}")
	private String shortTimeCaffeineSpec;

	@Value("${cache.caffeine.spec.long:maximumSize=500,expireAfterWrite=15m}")
	private String longTimeCaffeineSpec;

	/**
	 * CacheConfig constructor .
	 *
	 * @param redisHost    .
	 * @param redisPassword .
	 * @param objectMapper  .
	 */
	@Autowired
	public CacheConfig(@Value("${spring.redis.host}") String redisHost,
                       @Value("${spring.redis.password:}") String redisPassword,
                       CacheProperties cacheProperties,
                       ObjectMapper objectMapper) {
		this.redisHost = redisHost;
		this.redisPassword = redisPassword;
		this.objectMapper = objectMapper;
		this.cacheProperties = cacheProperties;
	}

	/**
	 * Caffeine缓存.
	 * 短存续时间内存缓存
	 */
	@Bean(CacheConstants.SHORT_TIME_MEMORY_CACHE_MANAGER)
	public CacheManager shortTimeMemoryCacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.setCaffeineSpec(CaffeineSpec.parse(shortTimeCaffeineSpec));
		return cacheManager;
	}

	/**
	 * Caffeine缓存.
	 * 长存续时间内存缓存
	 */
	@Bean(CacheConstants.LONG_TIME_MEMORY_CACHE_MANAGER)
	public CacheManager longTimeMemoryCacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.setCaffeineSpec(CaffeineSpec.parse(longTimeCaffeineSpec));
		return cacheManager;
	}



	/**
	 * redis 缓存.
	 *
	 * @param connectionFactory , redisSerializer .
	 * @return org.springframework.cache.CacheManager.
	 * @author Hardaway.
	 * @version 1.0.
	 * @date 15:45 2019/12/12.
	 */
	@Primary
	@Bean(CacheConstants.REDIS_CACHE_MANAGER)
	public CacheManager createCacheManager(RedisConnectionFactory connectionFactory,
										   RedisSerializer redisSerializer) {
		Redis redis = cacheProperties.getRedis();
		SerializationPair serializationPair = SerializationPair.fromSerializer(redisSerializer);
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(redis.getTimeToLive())
			.prefixKeysWith(redis.getKeyPrefix())
			.serializeValuesWith(serializationPair)
			.serializeKeysWith(serializationPair)
			.disableCachingNullValues();
		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(config)
			.initialCacheNames(cacheProperties.getCacheNames()
				.stream().collect(Collectors.toSet()))
			.transactionAware()
			.build();
	}


	@Bean
	public RedisSerializer redisSerializer() {
		return new GenericJackson2JsonRedisSerializer(initObjectMapper());
	}

	/**
	 * create stringRedisTemplate .
	 *
	 * @param redisConnectionFactory .
	 * @param redisSerializer        .
	 */
	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory,
												   RedisSerializer redisSerializer) {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(redisConnectionFactory);
		template.setKeySerializer(redisSerializer);
		template.setValueSerializer(redisSerializer);
		return template;
	}

	private static ObjectMapper initObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jdk8Module());
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.registerModule(new GuavaModule());
		objectMapper.registerModule(new KotlinModule());
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		objectMapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		return objectMapper;
	}
}

