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

import in.co.akshitbansal.springwebquery.jmh.entity.UserEntity;
import in.co.akshitbansal.springwebquery.jmh.model.User;
import in.co.akshitbansal.springwebquery.resolver.field.DTOAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.ResolutionResult;
import org.openjdk.jmh.annotations.*;

import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(2)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class DTOAwareFieldResolverBenchmark {

	@State(Scope.Benchmark)
	public static class TestParams {

		@Param({
				"userId",
				"profile.primaryAddress.city",
				"accounts.portfolios.positions.lots.serialNumber",
				"accounts.portfolios.positions.security.issuer.compliance.marketRegion"
		})
		public String dtoPath;
	}

	@State(Scope.Benchmark)
	public static class ResolverState {

		public FieldResolver fieldResolver;

		@Setup(Level.Trial)
		public void setup() {
			try {
				Class<DTOAwareFieldResolver> clazz = DTOAwareFieldResolver.class;
				Constructor<DTOAwareFieldResolver> constructor = clazz.getDeclaredConstructor(Class.class, Class.class);
				constructor.setAccessible(true);
				this.fieldResolver = constructor.newInstance(UserEntity.class, User.class);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	@Benchmark
	public ResolutionResult resolvePathTest(ResolverState resolverState, TestParams params) {
		return resolverState.fieldResolver.resolvePath(params.dtoPath);
	}
}
