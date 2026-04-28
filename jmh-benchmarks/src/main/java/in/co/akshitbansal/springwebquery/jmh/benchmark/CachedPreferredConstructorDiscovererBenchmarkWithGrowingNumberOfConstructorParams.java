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
public class CachedPreferredConstructorDiscovererBenchmarkWithGrowingNumberOfConstructorParams {

	@State(Scope.Thread)
	public static class CacheMissTestParams {

		@Param({"1", "2", "3", "4", "5"})
		public int size;

		public Class<?> clazz;

		public PreferredConstructorDiscoverer<?> discoverer;

		public Tuple tuple;

		@Setup(Level.Trial)
		public void setupTrial() throws NoSuchMethodException {
			// Building a class at runtime with a single constructor having 'size' number of String parameters
			List<Class<String>> constructorParams = IntStream
					.range(0, size)
					.mapToObj(idx -> String.class)
					.toList();

			DynamicType.Builder<?> builder = new ByteBuddy()
					.subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
					.name("in.co.akshitbansal.springwebquery.jmh.benchmark.CachedGrowingNumberOfConstructorParamsTestClass" + size)
					.defineConstructor(Visibility.PUBLIC)
					.withParameters(constructorParams)
					.intercept(MethodCall.invoke(Object.class.getConstructor()));

			try(DynamicType.Unloaded<?> unloaded = builder.make()) {
				this.clazz = unloaded
						.load(getClass().getClassLoader())
						.getLoaded();
			}

			// Creating a tuple with 'size' number of String values
			String[] aliases = IntStream
					.range(0, size)
					.mapToObj(idx -> "param" + idx)
					.toArray(String[]::new);
			// noinspection unchecked
			TupleElement<String>[] elements = IntStream
					.range(0, size)
					.mapToObj(idx -> new TupleElementImpl<>(String.class, aliases[idx]))
					.toArray(TupleElement[]::new);
			TupleMetadata metadata = new TupleMetadata(elements, aliases);
			String[] values = IntStream
					.range(0, size)
					.mapToObj(idx -> "value" + idx)
					.toArray(String[]::new);
			this.tuple = new TupleImpl(metadata, values);
		}

		@Setup(Level.Invocation)
		public void setupInvocation() {
			PreferredConstructorDiscovererFactory factory = new PreferredConstructorDiscovererFactory(true);
			this.discoverer = factory.newDiscoverer(clazz);
		}
	}

	@Benchmark
	public Constructor<?> testCacheMissDiscoverWithGrowingNumberOfConstructorParams(CacheMissTestParams params) {
		return params.discoverer.discover(params.tuple);
	}

	@State(Scope.Thread)
	public static class CacheHitTestParams {

		@Param({"1", "2", "3", "4", "5"})
		public int size;

		public PreferredConstructorDiscovererFactory factory = new PreferredConstructorDiscovererFactory(true);

		public PreferredConstructorDiscoverer<?> discoverer;

		public Tuple tuple;

		@Setup(Level.Trial)
		public void setup() throws NoSuchMethodException {
			// Building a class at runtime with a single constructor having 'size' number of String parameters
			List<Class<String>> constructorParams = IntStream
					.range(0, size)
					.mapToObj(idx -> String.class)
					.toList();

			DynamicType.Builder<?> builder = new ByteBuddy()
					.subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
					.name("in.co.akshitbansal.springwebquery.jmh.benchmark.CachedGrowingNumberOfConstructorParamsTestClass" + size)
					.defineConstructor(Visibility.PUBLIC)
					.withParameters(constructorParams)
					.intercept(MethodCall.invoke(Object.class.getConstructor()));

			Class<?> clazz;
			try(DynamicType.Unloaded<?> unloaded = builder.make()) {
				clazz = unloaded
						.load(getClass().getClassLoader())
						.getLoaded();
			}
			// Creating a discoverer for the class
			this.discoverer = factory.newDiscoverer(clazz);

			// Creating a tuple with 'size' number of String values
			String[] aliases = IntStream
					.range(0, size)
					.mapToObj(idx -> "param" + idx)
					.toArray(String[]::new);
			// noinspection unchecked
			TupleElement<String>[] elements = IntStream
					.range(0, size)
					.mapToObj(idx -> new TupleElementImpl<>(String.class, aliases[idx]))
					.toArray(TupleElement[]::new);
			TupleMetadata metadata = new TupleMetadata(elements, aliases);
			String[] values = IntStream
					.range(0, size)
					.mapToObj(idx -> "value" + idx)
					.toArray(String[]::new);
			this.tuple = new TupleImpl(metadata, values);

			// Loading cache
			discoverer.discover(tuple);
		}
	}

	@Benchmark
	public Constructor<?> testCacheHitDiscoverWithGrowingNumberOfConstructorParams(CacheHitTestParams params) {
		return params.discoverer.discover(params.tuple);
	}
}
