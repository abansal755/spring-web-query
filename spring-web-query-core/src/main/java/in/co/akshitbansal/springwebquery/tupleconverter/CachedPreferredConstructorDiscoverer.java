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

package in.co.akshitbansal.springwebquery.tupleconverter;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import lombok.*;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Cached {@link PreferredConstructorDiscoverer} variant that memoizes
 * constructor matches by target DTO type and tuple shape.
 *
 * @param <T> target DTO type
 */
public class CachedPreferredConstructorDiscoverer<T> extends PreferredConstructorDiscoverer<T> {

	/**
	 * Shared cache of constructors previously matched to DTO/tuple shapes.
	 */
	private final ConcurrentMap<CacheKey, Constructor<?>> constructorCache;

	/**
	 * Creates a cached discoverer for one target DTO type.
	 *
	 * @param clazz DTO type whose constructors will be inspected
	 * @param constructorCache shared cache used across discoverer instances
	 */
	CachedPreferredConstructorDiscoverer(@NonNull Class<T> clazz, @NonNull ConcurrentMap<CacheKey, Constructor<?>> constructorCache) {
		super(clazz);
		this.constructorCache = constructorCache;
	}

	/**
	 * Finds a constructor for the supplied tuple, serving the result from the
	 * shared cache whenever the tuple shape has already been seen.
	 *
	 * @param tuple tuple whose values will be passed to the constructor
	 *
	 * @return cached or freshly discovered matching constructor
	 */
	@Override
	public Constructor<T> discover(@NonNull Tuple tuple) {
		// noinspection unchecked
		return (Constructor<T>) constructorCache.computeIfAbsent(
				newCacheKey(tuple),
				ignored -> super.discover(tuple)
		);
	}

	/**
	 * Builds the cache key for one constructor lookup request.
	 *
	 * <p>The key is defined by the target DTO type and the ordered Java types of
	 * the tuple elements. Tuple values and aliases are not considered.</p>
	 *
	 * @param tuple tuple whose positional shape identifies the constructor lookup
	 *
	 * @return cache key for the requested tuple conversion
	 */
	private CacheKey newCacheKey(Tuple tuple) {
		List<Class<?>> parameterTypes = new ArrayList<>();
		for(TupleElement<?> tupleElement: tuple.getElements())
			parameterTypes.add(wrap(tupleElement.getJavaType()));
		return CacheKey.of(clazz, parameterTypes);
	}

	/**
	 * Cache key representing one DTO constructor lookup by tuple shape.
	 */
	@Getter
	@EqualsAndHashCode
	@ToString
	static class CacheKey {

		/**
		 * DTO type whose constructor is being resolved.
		 */
		private final Class<?> clazz;

		/**
		 * Positional tuple element types used to match a constructor.
		 */
		private final List<Class<?>> parameterTypes;

		private CacheKey(@NonNull Class<?> clazz, @NonNull List<Class<?>> parameterTypes) {
			this.clazz = clazz;
			this.parameterTypes = List.copyOf(parameterTypes);
		}

		public static CacheKey of(@NonNull Class<?> clazz, @NonNull List<Class<?>> parameterTypes) {
			return new CacheKey(clazz, parameterTypes);
		}
	}
}
