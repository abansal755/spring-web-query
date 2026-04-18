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
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.ResolutionResult;
import in.co.akshitbansal.springwebquery.resolver.field.cache.CachedDTOAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.cache.DTOAwareFieldResolutionCache;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(2)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CachedDTOAwareFieldResolverBenchmark {

	@State(Scope.Thread)
	public static class TestParams {

		@Param({
				"userId",
				"profile.primaryAddress.city",
				"accounts.portfolios.positions.lots.serialNumber",
				"accounts.portfolios.positions.security.issuer.compliance.marketRegion"
		})
		public String dtoPath;
	}

	@State(Scope.Thread)
	public static class CacheMissResolverState {

		public FieldResolver fieldResolver;

		@Setup(Level.Invocation)
		public void setup() {
			try {
				DTOAwareFieldResolutionCache cache = new DTOAwareFieldResolutionCache(1000, 128);
				this.fieldResolver = new CachedDTOAwareFieldResolver(UserEntity.class, User.class, cache);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	@State(Scope.Thread)
	public static class CacheHitResolverState {

		public FieldResolver fieldResolver;

		@Setup(Level.Trial)
		public void setup() {
			try {
				DTOAwareFieldResolutionCache cache = new DTOAwareFieldResolutionCache(1000, 128);
				this.fieldResolver = new CachedDTOAwareFieldResolver(UserEntity.class, User.class, cache);

				// populate cache
				fieldResolver.resolvePath("userId");
				fieldResolver.resolvePath("profile.primaryAddress.city");
				fieldResolver.resolvePath("accounts.portfolios.positions.lots.serialNumber");
				fieldResolver.resolvePath("accounts.portfolios.positions.security.issuer.compliance.marketRegion");
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	@Benchmark
	public ResolutionResult resolvePathCacheMissTest(CacheMissResolverState resolverState, TestParams params) {
		return resolverState.fieldResolver.resolvePath(params.dtoPath);
	}

	@Benchmark
	public ResolutionResult resolvePathCacheHitTest(CacheHitResolverState resolverState, TestParams params) {
		return resolverState.fieldResolver.resolvePath(params.dtoPath);
	}
}
