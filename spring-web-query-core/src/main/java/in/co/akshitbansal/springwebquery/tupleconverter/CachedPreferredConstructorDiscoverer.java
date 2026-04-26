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

public class CachedPreferredConstructorDiscoverer<T> extends PreferredConstructorDiscoverer<T> {

	private final ConcurrentMap<CacheKey, Constructor<?>> constructorCache;

	CachedPreferredConstructorDiscoverer(@NonNull Class<T> clazz, @NonNull ConcurrentMap<CacheKey, Constructor<?>> constructorCache) {
		super(clazz);
		this.constructorCache = constructorCache;
	}

	@Override
	public Constructor<T> discover(@NonNull Tuple tuple) {
		// noinspection unchecked
		return (Constructor<T>) constructorCache.computeIfAbsent(
				newCacheKey(tuple),
				ignored -> super.discover(tuple)
		);
	}

	private CacheKey newCacheKey(Tuple tuple) {
		List<Class<?>> parameterTypes = new ArrayList<>();
		for(TupleElement<?> tupleElement: tuple.getElements())
			parameterTypes.add(tupleElement.getJavaType());
		return CacheKey.of(clazz, parameterTypes);
	}

	@RequiredArgsConstructor(staticName = "of")
	@Getter
	@EqualsAndHashCode
	@ToString
	static class CacheKey {

		private final Class<?> clazz;
		private final List<Class<?>> parameterTypes;
	}
}
