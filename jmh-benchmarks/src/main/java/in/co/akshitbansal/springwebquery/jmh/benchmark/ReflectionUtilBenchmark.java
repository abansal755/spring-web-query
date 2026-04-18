package in.co.akshitbansal.springwebquery.jmh.benchmark;

import in.co.akshitbansal.springwebquery.jmh.model.User;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;
import org.openjdk.jmh.annotations.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(2)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ReflectionUtilBenchmark {

	@State(Scope.Benchmark)
	public static class TestParams {

		@Param({
				"userId",
				"profile.primaryAddress.city",
				"accounts.portfolios.positions.lots.serialNumber",
				"accounts.portfolios.positions.security.issuer.compliance.marketRegion"
		})
		public String fieldPath;

		public Class<?> clazz = User.class;
	}

	@Benchmark
	public List<Field> resolveFieldTest(TestParams params) {
		return ReflectionUtil.resolveFieldPath(params.clazz, params.fieldPath);
	}
}
