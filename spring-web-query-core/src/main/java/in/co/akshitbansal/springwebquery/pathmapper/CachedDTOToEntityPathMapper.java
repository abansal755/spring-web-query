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

package in.co.akshitbansal.springwebquery.pathmapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.util.concurrent.Striped;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

/**
 * Cached {@link DTOToEntityPathMapper} variant that memoizes both successful
 * and failed path resolutions.
 */
public class CachedDTOToEntityPathMapper extends DTOToEntityPathMapper {

	/**
	 * Cache of previously resolved DTO-to-entity mappings.
	 */
	private final ConcurrentMap<CacheKey, MappingResult> successfulResolutions;

	/**
	 * Cache of previously encountered resolution failures.
	 */
	private final Cache<CacheKey, RuntimeException> failedResolutions;

	/**
	 * Per-key striped locks used to avoid duplicate concurrent resolutions.
	 */
	private final Striped<Lock> stripedLock;

	/**
	 * Creates a cached mapper for one entity/DTO pair.
	 */
	CachedDTOToEntityPathMapper(
			@NonNull Class<?> entityClass,
			@NonNull Class<?> dtoClass,
			@NonNull ConcurrentMap<CacheKey, MappingResult> successfulResolutions,
			@NonNull Cache<CacheKey, RuntimeException> failedResolutions,
			@NonNull Striped<Lock> stripedLock
	) {
		super(entityClass, dtoClass);
		this.successfulResolutions = successfulResolutions;
		this.failedResolutions = failedResolutions;
		this.stripedLock = stripedLock;
	}

	/**
	 * Resolves a DTO path, serving the result from cache whenever possible.
	 *
	 * @param dtoPath selector path expressed against the DTO contract
	 *
	 * @return cached or freshly resolved mapping result
	 */
	@Override
	public MappingResult map(@NonNull String dtoPath) {
		CacheKey cacheKey = CacheKey.of(entityClass, dtoClass, dtoPath);
		MappingResult result = resolveFromCache(cacheKey);
		if (result != null) return result;

		Lock lock = stripedLock.get(cacheKey);
		lock.lock();
		try {
			result = resolveFromCache(cacheKey);
			if (result != null) return result;

			try {
				result = super.map(dtoPath);
				successfulResolutions.put(cacheKey, result);
				return result;
			}
			catch (RuntimeException ex) {
				failedResolutions.put(cacheKey, ex);
				throw ex;
			}
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Returns a cached result for the supplied key, or rethrows a cached failure.
	 *
	 * @param cacheKey cache entry key
	 *
	 * @return cached mapping result, or {@code null} if no entry exists
	 */
	@Nullable
	private MappingResult resolveFromCache(CacheKey cacheKey) {
		MappingResult result = successfulResolutions.get(cacheKey);
		if (result != null) return result;
		RuntimeException ex = failedResolutions.getIfPresent(cacheKey);
		if (ex != null) throw ex;
		return null;
	}

	/**
	 * Cache key representing one entity/DTO/path resolution request.
	 */
	@RequiredArgsConstructor(staticName = "of")
	@Getter
	@EqualsAndHashCode
	@ToString
	static class CacheKey {

		/**
		 * Entity type participating in the mapping.
		 */
		@NonNull
		private final Class<?> entityClass;

		/**
		 * DTO type participating in the mapping.
		 */
		@NonNull
		private final Class<?> dtoClass;

		/**
		 * DTO selector path being resolved.
		 */
		@NonNull
		private final String dtoPath;
	}
}
