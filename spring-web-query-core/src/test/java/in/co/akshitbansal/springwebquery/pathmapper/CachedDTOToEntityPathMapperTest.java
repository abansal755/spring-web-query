package in.co.akshitbansal.springwebquery.pathmapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.Striped;
import in.co.akshitbansal.springwebquery.entity.UserEntity;
import in.co.akshitbansal.springwebquery.model.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import static in.co.akshitbansal.springwebquery.pathmapper.CachedDTOToEntityPathMapper.CacheKey;
import static in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper.MappingResult;
import static org.junit.jupiter.api.Assertions.*;

class CachedDTOToEntityPathMapperTest {

	@Test
	void testConstructionWithNullEntityClass() {
		var successfulResolutions = new ConcurrentHashMap<CacheKey, MappingResult>();
		var failedResolutions = newFailedResolutions();
		var stripedLock = newStripedLock();
		assertThrows(NullPointerException.class, () -> new CachedDTOToEntityPathMapper(
				null, User.class, successfulResolutions, failedResolutions, stripedLock
		));
	}

	@Test
	void testConstructionWithNullDTOClass() {
		var successfulResolutions = new ConcurrentHashMap<CacheKey, MappingResult>();
		var failedResolutions = newFailedResolutions();
		var stripedLock = newStripedLock();
		assertThrows(NullPointerException.class, () -> new CachedDTOToEntityPathMapper(
				UserEntity.class, null, successfulResolutions, failedResolutions, stripedLock
		));
	}

	@Test
	void testConstructionWithNullSuccessfulResolutions() {
		var failedResolutions = newFailedResolutions();
		var stripedLock = newStripedLock();
		assertThrows(NullPointerException.class, () -> new CachedDTOToEntityPathMapper(
				UserEntity.class, User.class, null, failedResolutions, stripedLock
		));
	}

	@Test
	void testConstructionWithNullFailedResolutions() {
		var successfulResolutions = new ConcurrentHashMap<CacheKey, MappingResult>();
		var stripedLock = newStripedLock();
		assertThrows(NullPointerException.class, () -> new CachedDTOToEntityPathMapper(
				UserEntity.class, User.class, successfulResolutions, null, stripedLock
		));
	}

	@Test
	void testConstructionWithNullStripedLock() {
		var successfulResolutions = new ConcurrentHashMap<CacheKey, MappingResult>();
		var failedResolutions = newFailedResolutions();
		assertThrows(NullPointerException.class, () -> new CachedDTOToEntityPathMapper(
				UserEntity.class, User.class, successfulResolutions, failedResolutions, null
		));
	}

	@Test
	void testShouldReturnSameInstanceForRepeatedSuccessfulLookup() {
		// Constructing the mapper
		ConcurrentMap<CacheKey, MappingResult> successfulResolutions = new ConcurrentHashMap<>();
		Cache<CacheKey, RuntimeException> failedResolutions = newFailedResolutions();
		Striped<Lock> stripedLock = newStripedLock();
		CachedDTOToEntityPathMapper mapper = new CachedDTOToEntityPathMapper(
				UserEntity.class, User.class,
				successfulResolutions, failedResolutions, stripedLock
		);

		// Asserting that the cache is empty
		assertEquals(0, successfulResolutions.size());

		// Loading mapper's cache with one result
		MappingResult result = mapper.map("email");
		// Asserting that the cache has one entry
		assertEquals(1, successfulResolutions.size());
		// Assertion on entity path
		assertEquals("email", result.getPath());
		// Assertions on terminal DTO field
		Field field = result.getTerminalDTOField();
		assertEquals("email", field.getName());
		assertEquals(User.class, field.getDeclaringClass());

		// Calling the mapper again with the same path
		MappingResult result2 = mapper.map("email");
		// Asserting that the cache has one entry
		assertEquals(1, successfulResolutions.size());
		// The result objects should be the same (== not just equals)
		assertSame(result, result2);
	}

	@Test
	void testShouldReturnSameInstanceForRepeatedFailedLookup() {
		// Constructing the mapper
		ConcurrentMap<CacheKey, MappingResult> successfulResolutions = new ConcurrentHashMap<>();
		Cache<CacheKey, RuntimeException> failedResolutions = newFailedResolutions();
		Striped<Lock> stripedLock = newStripedLock();
		CachedDTOToEntityPathMapper mapper = new CachedDTOToEntityPathMapper(
				UserEntity.class, User.class,
				successfulResolutions, failedResolutions, stripedLock
		);

		// Asserting that the cache is empty
		assertEquals(0, failedResolutions.estimatedSize());

		// Loading mapper's cache with one result
		RuntimeException ex = assertThrows(RuntimeException.class, () -> mapper.map("hello"));
		// Asserting that the cache has one entry
		assertEquals(1, failedResolutions.estimatedSize());

		// Calling the mapper again with the same path
		RuntimeException ex2 = assertThrows(RuntimeException.class, () -> mapper.map("hello"));
		// Asserting that the cache has one entry
		assertEquals(1, failedResolutions.estimatedSize());
		// The result objects should be the same (== not just equals)
		assertSame(ex, ex2);
	}

	private Cache<CacheKey, RuntimeException> newFailedResolutions() {
		return Caffeine
				.newBuilder()
				.maximumSize(10)
				.build();
	}

	private Striped<Lock> newStripedLock() {
		return Striped.lock(2);
	}
}
