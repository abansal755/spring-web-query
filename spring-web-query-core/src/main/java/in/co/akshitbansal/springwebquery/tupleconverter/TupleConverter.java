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

import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import jakarta.persistence.Tuple;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;

/**
 * Converts JPA {@link Tuple} results into DTO instances by invoking a matching
 * constructor discovered for the target DTO type.
 *
 * <p>Constructor selection is delegated to the supplied
 * {@link PreferredConstructorDiscoverer}. Once a matching constructor has been
 * found for the first tuple shape seen by this converter, that constructor is
 * cached on the converter instance for subsequent conversions.</p>
 *
 * @param <T> target DTO type
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class TupleConverter<T> implements Converter<Tuple, T> {

	/**
	 * Discoverer used to locate a constructor compatible with incoming tuples.
	 */
	@NonNull
	private final PreferredConstructorDiscoverer<T> discoverer;

	/**
	 * Lazily discovered constructor cached for repeated conversions.
	 */
	@Nullable
	private volatile Constructor<T> cachedConstructor;

	/**
	 * Converts one tuple into the configured DTO type.
	 *
	 * @param tuple tuple to convert
	 *
	 * @return instantiated DTO
	 *
	 * @throws QueryConfigurationException if no suitable constructor can be
	 * found or invocation fails
	 */
	@Override
	public T convert(@NonNull Tuple tuple) {
		try {
			// synchronization with double-checking
			if (cachedConstructor == null) {
				synchronized (this) {
					if (cachedConstructor == null)
						cachedConstructor = discoverer.discover(tuple);
				}
			}
			// cachedConstructor will never be null here, so it is safe to invoke newInstance
			// noinspection DataFlowIssue
			return cachedConstructor.newInstance(tuple.toArray());
		}
		catch (Exception ex) {
			throw new QueryConfigurationException(
					MessageFormat.format(
							"Failed to convert tuple to {0}: {1}",
							discoverer.getTargetClass().getName(), ex.getMessage()
					), ex
			);
		}
	}
}
