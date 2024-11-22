package com.tencent.core.feign;

import com.tencent.common.dto.ApiResponse;
import com.tencent.common.dto.CounterRequest;

public interface CounterFeign {

	ApiResponse get();

	ApiResponse create(CounterRequest request);
}
