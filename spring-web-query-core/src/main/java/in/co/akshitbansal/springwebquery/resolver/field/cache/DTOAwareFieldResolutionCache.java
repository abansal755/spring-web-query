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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import in.co.akshitbansal.springwebquery.resolver.field.ResolutionResult;
import in.co.akshitbansal.springwebquery.validator.KeyLockPoolSizeValidator;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Shared cache for DTO-aware field-resolution outcomes.
 *
 * <p>Successful resolutions are retained without eviction, while failed
 * resolutions are stored in a bounded LRU map to avoid unbounded memory growth
 * from repeated invalid selector lookups.</p>
 */
public class DTOAwareFieldResolutionCache {

	/**
	 * Cache of successfully resolved DTO selectors keyed by query contract.
	 */
	private final ConcurrentMap<CacheKey, ResolutionResult> successfulResolutions;

	/**
	 * Bounded cache of failed resolution attempts keyed by query contract.
	 */
	private final Cache<CacheKey, RuntimeException> failedResolutions;

	/**
	 * Striped lock pool used to serialize cache population for hash-bucket
	 * groups of cache keys.
	 */
	private final Lock[] keyLockPool;

	/**
	 * Creates the shared cache used by cached DTO-aware field resolvers.
	 *
	 * @param failedResolutionsMaxCapacity maximum number of failed resolutions to
	 * retain
	 * @param keyLockPoolSize number of striped locks used to coordinate cache
	 * population for selector keys
	 */
	public DTOAwareFieldResolutionCache(int failedResolutionsMaxCapacity, int keyLockPoolSize) {
		// Validate failed resolutions max capacity
		if (failedResolutionsMaxCapacity <= 0)
			throw new IllegalArgumentException("Failed resolutions max capacity must be a positive integer");
		// Validate key lock pool size
		KeyLockPoolSizeValidator keyLockPoolSizeValidator = new KeyLockPoolSizeValidator();
		keyLockPoolSizeValidator.validate(keyLockPoolSize);

		this.successfulResolutions = new ConcurrentHashMap<>();
		this.failedResolutions = Caffeine
				.newBuilder()
				.maximumSize(failedResolutionsMaxCapacity)
				.build();

		this.keyLockPool = new Lock[keyLockPoolSize];
		for (int idx = 0; idx < keyLockPool.length; idx++)
			keyLockPool[idx] = new ReentrantLock();
	}

	/**
	 * Returns a cached successful resolution result when present or rethrows a
	 * cached failure for the same key.
	 *
	 * @param cacheKey composite key identifying the query contract and DTO path
	 *
	 * @return cached successful resolution, or {@code null} when the key is not
	 * present
	 */
	@Nullable
	public ResolutionResult resolveFromCache(@NonNull CacheKey cacheKey) {
		ResolutionResult result = successfulResolutions.get(cacheKey);
		if (result != null) return result;
		RuntimeException ex = failedResolutions.getIfPresent(cacheKey);
		if (ex != null) throw ex;
		return null;
	}

	/**
	 * Stores a successful DTO-aware path-resolution result.
	 *
	 * @param cacheKey composite key identifying the query contract and DTO path
	 * @param result successful resolution result to cache
	 */
	public void putSuccessfulResolution(@NonNull CacheKey cacheKey, @NonNull ResolutionResult result) {
		successfulResolutions.put(cacheKey, result);
	}

	/**
	 * Stores a failed DTO-aware path-resolution attempt.
	 *
	 * @param cacheKey composite key identifying the query contract and DTO path
	 * @param ex exception raised while resolving the path
	 */
	public void putFailedResolution(@NonNull CacheKey cacheKey, @NonNull RuntimeException ex) {
		failedResolutions.put(cacheKey, ex);
	}

	/**
	 * Returns the striped lock associated with the supplied cache key.
	 *
	 * <p>The returned lock is selected by hashing the key into the configured
	 * lock pool so callers can coordinate cache population without allocating a
	 * dedicated lock per selector.</p>
	 *
	 * @param cacheKey composite key identifying the query contract and DTO path
	 *
	 * @return lock guarding cache population for the key's stripe
	 */
	public Lock getKeyLock(@NonNull CacheKey cacheKey) {
		int idx = cacheKey.hashCode() & (keyLockPool.length - 1);
		return keyLockPool[idx];
	}
}
