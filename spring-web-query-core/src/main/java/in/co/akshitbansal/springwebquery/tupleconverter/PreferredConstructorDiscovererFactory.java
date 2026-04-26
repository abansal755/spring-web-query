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

import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static in.co.akshitbansal.springwebquery.tupleconverter.CachedPreferredConstructorDiscoverer.CacheKey;

public class PreferredConstructorDiscovererFactory {

	@Nullable
	private final ConcurrentMap<CacheKey, Constructor<?>> constructorCache;

	public PreferredConstructorDiscovererFactory(boolean useCache) {
		if (useCache) constructorCache = new ConcurrentHashMap<>();
		else constructorCache = null;
	}

	public <T> PreferredConstructorDiscoverer<T> newDiscoverer(@NonNull Class<T> clazz) {
		if (constructorCache != null)
			return new CachedPreferredConstructorDiscoverer<>(clazz, constructorCache);
		return new PreferredConstructorDiscoverer<>(clazz);
	}
}
