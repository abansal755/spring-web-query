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

package in.co.akshitbansal.springwebquery.repository;

import in.co.akshitbansal.springwebquery.util.PreferredConstructorDiscoveryUtil;
import jakarta.persistence.Tuple;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;

@RequiredArgsConstructor
public class TupleConverter<T> implements Converter<Tuple, T> {

	@NonNull
	private final Class<T> targetType;

	@Override
	public T convert(@NonNull Tuple tuple) {
		try {
			Constructor<?> constructor = PreferredConstructorDiscoveryUtil.discover(targetType, tuple);
			return (T) constructor.newInstance(tuple.toArray());
		}
		catch (Exception ex) {
			throw new RuntimeException(MessageFormat.format(
					"Failed to convert tuple to {0}: {1}",
					targetType.getName(), ex.getMessage()
			), ex);
		}
	}
}
