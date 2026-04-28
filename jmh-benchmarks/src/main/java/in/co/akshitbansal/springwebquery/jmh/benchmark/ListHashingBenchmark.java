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

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(2)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ListHashingBenchmark {

	@State(Scope.Thread)
	public static class TestParams {

		@Param({"0", "1", "2", "5", "10", "50", "100"})
		public int size;

		public List<Class<?>> classes;

		@Setup(Level.Trial)
		public void setup() {
			classes = new ArrayList<>();
			for (int idx = 0; idx < size; idx++)
				classes.add(CLASSES[idx % CLASSES.length]);
		}

		private static final Class<?>[] CLASSES = new Class<?>[] {
				String.class,
				Integer.class,
				Long.class,
				Double.class,
				Float.class,
				Boolean.class,
				Byte.class,
				Short.class,
				Character.class,
				Object.class,
				int.class,
				long.class,
				double.class,
				float.class,
				boolean.class,
				byte.class,
				short.class,
				char.class
		};
	}

	@Benchmark
	public int hashListTest(TestParams params) {
		return params.classes.hashCode();
	}
}
