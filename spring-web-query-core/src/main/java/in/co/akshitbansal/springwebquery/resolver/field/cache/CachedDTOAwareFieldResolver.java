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

import in.co.akshitbansal.springwebquery.resolver.field.DTOAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.ResolutionResult;
import lombok.NonNull;

import java.util.concurrent.locks.Lock;

/**
 * {@link DTOAwareFieldResolver} variant that caches DTO-path resolution
 * outcomes.
 *
 * <p>Successful resolutions are memoized by {@link CacheKey}, and a per-key
 * lock is used to avoid duplicate reflective resolution work when multiple
 * threads request the same path concurrently.</p>
 */
public class CachedDTOAwareFieldResolver extends DTOAwareFieldResolver {

	/**
	 * Shared cache storing successful and failed DTO-aware resolution attempts.
	 */
	private final DTOAwareFieldResolutionCache cache;

	/**
	 * Creates a cached DTO-aware field resolver for a single query contract.
	 *
	 * @param entityClass entity type used to validate mapped paths
	 * @param dtoClass DTO type exposed to API callers
	 * @param cache shared cache for resolution results
	 */
	public CachedDTOAwareFieldResolver(
			@NonNull Class<?> entityClass,
			@NonNull Class<?> dtoClass,
			@NonNull DTOAwareFieldResolutionCache cache
	) {
		super(entityClass, dtoClass);
		this.cache = cache;
	}

	/**
	 * Resolves the supplied DTO selector path, consulting and populating the
	 * shared cache around the superclass's reflective resolution logic.
	 *
	 * @param dtoPath selector path from the incoming request
	 *
	 * @return resolution result containing the mapped entity path and terminal
	 * DTO field
	 */
	@Override
	public ResolutionResult resolvePath(@NonNull String dtoPath) {
		CacheKey cacheKey = new CacheKey(entityClass, dtoClass, dtoPath);
		ResolutionResult result = cache.resolveFromCache(cacheKey);
		if (result != null) return result;

		Lock lock = cache.getKeyLock(cacheKey);
		lock.lock();
		try {
			result = cache.resolveFromCache(cacheKey);
			if (result != null) return result;

			try {
				result = super.resolvePath(dtoPath);
				cache.putSuccessfulResolution(cacheKey, result);
				return result;
			}
			catch (RuntimeException rEx) {
				cache.putFailedResolution(cacheKey, rEx);
				throw rEx;
			}
		}
		finally {
			lock.unlock();
		}
	}
}
