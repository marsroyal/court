package com.tencent.app.controller;

import com.tencent.core.feign.CounterFeign;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import com.tencent.common.dto.ApiResponse;
import com.tencent.common.dto.CounterRequest;
import com.tencent.common.entity.Counter;
import com.tencent.core.service.CounterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * counter控制器
 */
@RestController
@Slf4j
public class CounterController implements CounterFeign {

	@Value("${spring.profiles.active}")
	private String envName;

	@Resource
	RedissonClient redissonClient;

	@Resource
	CounterService counterService;

	/**
	 * 获取当前计数
	 *
	 * @return API response json
	 */
	@Override
	@GetMapping(value = "/api/count")
	public ApiResponse get() {
		log.info("/api/count get request, env=[{}]", envName);
		RBucket<Integer> bucket = redissonClient.getBucket("count");
		if (bucket.isExists()) {
			return ApiResponse.ok(bucket.get());
		}
		Optional<Counter> counter = counterService.getCounter(1);
		Integer count = 0;
		if (counter.isPresent()) {
			count = counter.get().getCount();
		}
		bucket.set(count);
		bucket.expire(10, TimeUnit.SECONDS);
		return ApiResponse.ok(count);
	}


	/**
	 * 更新计数，自增或者清零
	 *
	 * @param request {@link CounterRequest}
	 * @return API response json
	 */
	@Override
	@PostMapping(value = "/api/count")
	public ApiResponse create(@RequestBody CounterRequest request) {
		log.info("/api/count post request, action: {}", request.getAction());
		Optional<Counter> curCounter = counterService.getCounter(1);
		if (request.getAction().equals("inc")) {
			Integer count = 1;
			if (curCounter.isPresent()) {
				count += curCounter.get().getCount();
			}
			Counter counter = new Counter();
			counter.setId(1);
			counter.setCount(count);
			counterService.upsertCount(counter);
			return ApiResponse.ok(count);
		} else if (request.getAction().equals("clear")) {
			if (!curCounter.isPresent()) {
				return ApiResponse.ok(0);
			}
			counterService.clearCount(1);
			return ApiResponse.ok(0);
		} else {
			return ApiResponse.error("参数action错误");
		}
	}

}
