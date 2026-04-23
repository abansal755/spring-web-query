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

public class CachedDTOToEntityPathMapper extends DTOToEntityPathMapper {

	private final ConcurrentMap<CacheKey, MappingResult> successfulResolutions;
	private final Cache<CacheKey, RuntimeException> failedResolutions;
	private final Striped<Lock> stripedLock;

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

	@Nullable
	private MappingResult resolveFromCache(CacheKey cacheKey) {
		MappingResult result = successfulResolutions.get(cacheKey);
		if (result != null) return result;
		RuntimeException ex = failedResolutions.getIfPresent(cacheKey);
		if (ex != null) throw ex;
		return null;
	}

	@RequiredArgsConstructor(staticName = "of")
	@Getter
	@EqualsAndHashCode
	@ToString
	static class CacheKey {

		@NonNull
		private final Class<?> entityClass;

		@NonNull
		private final Class<?> dtoClass;

		@NonNull
		private final String dtoPath;
	}
}
