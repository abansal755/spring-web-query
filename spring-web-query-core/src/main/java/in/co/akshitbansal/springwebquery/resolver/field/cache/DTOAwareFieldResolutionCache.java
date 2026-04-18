/*
 * Copyright 2026-present Akshit Bansal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.co.akshitbansal.springwebquery.resolver.field.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import in.co.akshitbansal.springwebquery.resolver.field.ResolutionResult;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DTOAwareFieldResolutionCache {

	private final ConcurrentMap<CacheKey, ResolutionResult> successfulResolutions;
	private final ConcurrentMap<CacheKey, RuntimeException> failedResolutions;
	private final ConcurrentMap<CacheKey, Lock> keyLocks;

	public DTOAwareFieldResolutionCache(int failedResolutionsMaxCapacity) {
		if (failedResolutionsMaxCapacity <= 0)
			throw new IllegalArgumentException("Failed resolutions max capacity must be a positive integer");

		this.successfulResolutions = new ConcurrentHashMap<>();
		this.failedResolutions = new ConcurrentLinkedHashMap.Builder<CacheKey, RuntimeException>()
				.maximumWeightedCapacity(failedResolutionsMaxCapacity)
				.build();
		this.keyLocks = new ConcurrentHashMap<>();
	}

	@Nullable
	public ResolutionResult resolveFromCache(@NonNull CacheKey cacheKey) {
		ResolutionResult result = successfulResolutions.get(cacheKey);
		if (result != null) return result;
		RuntimeException ex = failedResolutions.get(cacheKey);
		if (ex != null) throw ex;
		return null;
	}

	public void putSuccessfulResolution(@NonNull CacheKey cacheKey, @NonNull ResolutionResult result) {
		successfulResolutions.put(cacheKey, result);
	}

	public void putFailedResolution(@NonNull CacheKey cacheKey, @NonNull RuntimeException ex) {
		failedResolutions.put(cacheKey, ex);
	}

	public Lock getLock(@NonNull CacheKey cacheKey) {
		return keyLocks.computeIfAbsent(cacheKey, ignored -> new ReentrantLock());
	}

	public void removeLock(@NonNull CacheKey cacheKey) {
		keyLocks.remove(cacheKey);
	}
}
