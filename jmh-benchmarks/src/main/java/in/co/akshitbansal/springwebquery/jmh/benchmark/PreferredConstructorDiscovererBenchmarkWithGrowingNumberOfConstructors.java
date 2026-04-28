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

package in.co.akshitbansal.springwebquery.jmh.benchmark;

import in.co.akshitbansal.springwebquery.tupleconverter.PreferredConstructorDiscoverer;
import in.co.akshitbansal.springwebquery.tupleconverter.PreferredConstructorDiscovererFactory;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodCall;
import org.hibernate.sql.results.internal.TupleElementImpl;
import org.hibernate.sql.results.internal.TupleImpl;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.openjdk.jmh.annotations.*;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.AverageTime)
@Fork(2)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class PreferredConstructorDiscovererBenchmarkWithGrowingNumberOfConstructors {

	@State(Scope.Thread)
	public static class TestParams {

		@Param({"1", "2", "3", "4", "5"})
		public int numConstructors;

		public int numConstructorParams = 5;

		public PreferredConstructorDiscovererFactory factory = new PreferredConstructorDiscovererFactory(false);

		public PreferredConstructorDiscoverer<?> discoverer;

		public Tuple tuple;

		@Setup(Level.Trial)
		public void setup() throws NoSuchMethodException {
			// Building a class at runtime with 'numConstructors' number of constructors
			// Where each constructor has 'numConstructorParams' String parameters except one Integer parameter at varying positions
			DynamicType.Builder<?> builder = new ByteBuddy()
					.subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
					.name("in.co.akshitbansal.springwebquery.jmh.benchmark.GrowingNumberOfConstructorsTestClass" + numConstructors);

			for (int idx = 0; idx < numConstructors; idx++) {
				int finalIdx = idx;
				List<Class<?>> constructorParams = IntStream
						.range(0, numConstructorParams)
						.mapToObj(idx2 -> {
							if (finalIdx == idx2) return (Class<?>) Integer.class;
							return (Class<?>) String.class;
						})
						.toList();
				builder = builder
						.defineConstructor(Visibility.PUBLIC)
						.withParameters(constructorParams)
						.intercept(MethodCall.invoke(Object.class.getConstructor()));
			}

			Class<?> clazz;
			try(DynamicType.Unloaded<?> unloaded = builder.make()) {
				clazz = unloaded
						.load(getClass().getClassLoader())
						.getLoaded();
			}
			// Creating a discoverer for the class
			this.discoverer = factory.newDiscoverer(clazz);

			// Creating a tuple with 100 String values except one Integer value at the start
			String[] aliases = IntStream
					.range(0, numConstructorParams)
					.mapToObj(idx -> "param" + idx)
					.toArray(String[]::new);
			TupleElement<?>[] elements = IntStream
					.range(0, numConstructorParams)
					.mapToObj(idx -> {
						Class<?> type;
						if (idx == 0) type = Integer.class;
						else type = String.class;
						return new TupleElementImpl<>(type, aliases[idx]);
					})
					.toArray(TupleElement[]::new);
			TupleMetadata metadata = new TupleMetadata(elements, aliases);
			Object[] values = IntStream
					.range(0, numConstructorParams)
					.mapToObj(idx -> {
						if (idx == 0) return 1;
						return "value" + idx;
					})
				.toArray(Object[]::new);
			this.tuple = new TupleImpl(metadata, values);
		}
	}

	@Benchmark
	public Constructor<?> testDiscoverWithGrowingNumberOfConstructors(TestParams params) {
		return params.discoverer.discover(params.tuple);
	}
}
