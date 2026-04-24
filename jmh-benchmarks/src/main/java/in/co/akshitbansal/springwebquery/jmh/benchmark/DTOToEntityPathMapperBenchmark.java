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
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapperFactory;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper.MappingResult;

@BenchmarkMode(Mode.AverageTime)
@Fork(2)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class DTOToEntityPathMapperBenchmark {

	@State(Scope.Thread)
	public static class TestParams {

		@Param({
				"userId",
				"profile.primaryAddress.city",
				"accounts.portfolios.positions.lots.serialNumber",
				"accounts.portfolios.positions.security.issuer.compliance.marketRegion"
		})
		public String dtoPath;

		public DTOToEntityPathMapper pathMapper;

		@Setup(Level.Trial)
		public void setup() {
			DTOToEntityPathMapperFactory factory = new DTOToEntityPathMapperFactory();
			pathMapper = factory.newMapper(UserEntity.class, User.class);
		}
	}

	@Benchmark
	public MappingResult resolvePathTest(TestParams params) {
		return params.pathMapper.map(params.dtoPath);
	}
}
