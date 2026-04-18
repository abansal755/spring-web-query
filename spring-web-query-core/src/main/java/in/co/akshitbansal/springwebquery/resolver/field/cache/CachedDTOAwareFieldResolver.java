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
import in.co.akshitbansal.springwebquery.resolver.field.DTOAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.ResolutionResult;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class CachedDTOAwareFieldResolver extends DTOAwareFieldResolver {

	@Nullable
	private static volatile ConcurrentMap<CacheKey, ResolutionResult> successfulResolutions;

	@Nullable
	private static volatile ConcurrentMap<CacheKey, RuntimeException> failedResolutions;

	@Nullable
	private static volatile ConcurrentMap<CacheKey, Lock> keyLocks;

	private static volatile boolean initialized = false;

	public static synchronized void initCache(int failedResolutionsMaxCapacity) {
		if (initialized) {
			log.warn(
					"Cache has already been initialized earlier. Skipping initialization with max capacity for failed resolutions: {}",
					failedResolutionsMaxCapacity
			);
			return;
		}
		if (failedResolutionsMaxCapacity <= 0)
			throw new IllegalArgumentException("Failed resolutions max capacity must be a positive integer");

		successfulResolutions = new ConcurrentHashMap<>();
		failedResolutions = new ConcurrentLinkedHashMap.Builder<CacheKey, RuntimeException>()
				.maximumWeightedCapacity(failedResolutionsMaxCapacity)
				.build();
		keyLocks = new ConcurrentHashMap<>();
		initialized = true;
	}

	public CachedDTOAwareFieldResolver(@NonNull Class<?> entityClass, @NonNull Class<?> dtoClass) {
		super(entityClass, dtoClass);
	}

	@Override
	@SuppressWarnings({"DataFlowIssue", "NullAway"})
	public ResolutionResult resolvePath(@NonNull String dtoPath) {
		if (!initialized)
			throw new IllegalStateException("Cache not initialized");

		CacheKey cacheKey = new CacheKey(entityClass, dtoClass, dtoPath);
		ResolutionResult result = resolveFromCache(cacheKey);
		if (result != null) return result;

		Lock lock = keyLocks.computeIfAbsent(cacheKey, ignored -> new ReentrantLock());
		lock.lock();
		try {
			result = resolveFromCache(cacheKey);
			if (result != null) return result;

			try {
				result = super.resolvePath(dtoPath);
				successfulResolutions.put(cacheKey, result);
				return result;
			}
			catch (RuntimeException rEx) {
				failedResolutions.put(cacheKey, rEx);
				throw rEx;
			}
		}
		finally {
			lock.unlock();
			keyLocks.remove(cacheKey, lock);
		}
	}

	@Nullable
	@SuppressWarnings({"DataFlowIssue", "NullAway"})
	private static ResolutionResult resolveFromCache(CacheKey cacheKey) {
		ResolutionResult result = successfulResolutions.get(cacheKey);
		if (result != null) return result;
		RuntimeException ex = failedResolutions.get(cacheKey);
		if (ex != null) throw ex;
		return null;
	}
}
