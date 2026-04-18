package in.co.akshitbansal.springwebquery.jmh.benchmark;

import in.co.akshitbansal.springwebquery.jmh.entity.UserEntity;
import in.co.akshitbansal.springwebquery.jmh.model.User;
import in.co.akshitbansal.springwebquery.resolver.field.DTOAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolver;
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
				Class<DTOAwareFieldResolver> clazz =  DTOAwareFieldResolver.class;
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
	public String resolvePathTest(ResolverState resolverState, TestParams params) {
		return resolverState.fieldResolver.resolvePath(params.dtoPath);
	}
}
