package in.co.akshitbansal.springwebquery.pathmapper;

import in.co.akshitbansal.springwebquery.entity.UserEntity;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DTOToEntityPathMapperFactoryTest {

	@Test
	void testUncachedMapper() {
		DTOToEntityPathMapperFactory factory = new DTOToEntityPathMapperFactory();
		DTOToEntityPathMapper mapper = factory.newMapper(UserEntity.class, User.class);
		assertSame(DTOToEntityPathMapper.class, mapper.getClass());
	}

	@Test
	void testCachedMapper() {
		DTOToEntityPathMapperFactory factory = new DTOToEntityPathMapperFactory(10, 2);
		DTOToEntityPathMapper mapper = factory.newMapper(UserEntity.class, User.class);
		assertSame(CachedDTOToEntityPathMapper.class, mapper.getClass());
	}

	@Test
	void testNegativeFailedResolutionsMaxCapacity() {
		QueryConfigurationException ex = assertThrows(
				QueryConfigurationException.class,
				() -> new DTOToEntityPathMapperFactory(-1, 2)
		);
		Throwable cause = ex.getCause();
		assertInstanceOf(IllegalArgumentException.class, cause);
		assertTrue(cause.getMessage().contains("must not be negative"));
	}

	@Test
	void testNonPositiveLockStripeCount() {
		QueryConfigurationException ex = assertThrows(
				QueryConfigurationException.class,
				() -> new DTOToEntityPathMapperFactory(10, 0)
		);
		Throwable cause = ex.getCause();
		assertInstanceOf(IllegalArgumentException.class, cause);
		assertTrue(cause.getMessage().contains("must be positive"));
	}
}
