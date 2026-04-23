package in.co.akshitbansal.springwebquery.pathmapper;

import in.co.akshitbansal.springwebquery.entity.UserEntity;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.model.Name;
import in.co.akshitbansal.springwebquery.model.Phone;
import in.co.akshitbansal.springwebquery.model.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper.MappingResult;
import static org.junit.jupiter.api.Assertions.*;

class DTOToEntityPathMapperTest {

	private final DTOToEntityPathMapper mapper = new DTOToEntityPathMapper(UserEntity.class, User.class);

	@Test
	void testConstructionWithNullEntityClass() {
		assertThrows(NullPointerException.class, () -> new DTOToEntityPathMapper(null, User.class));
	}

	@Test
	void testConstructionWithNullDTOClass() {
		assertThrows(NullPointerException.class, () -> new DTOToEntityPathMapper(UserEntity.class, null));
	}

	@Test
	void testMapWithNullPath() {
		assertThrows(NullPointerException.class, () -> mapper.map(null));
	}

	@Test
	void testMapWithInvalidPath() {
		QueryFieldValidationException ex = assertThrows(QueryFieldValidationException.class, () -> mapper.map("invalid.path"));
		assertTrue(ex.getMessage().contains("Unknown field"));
	}

	@Test
	void testMapWithSameEntityAndDTOPath() {
		MappingResult result = mapper.map("email");
		assertEquals("email", result.getPath());

		// Assertions on the terminal DTO field
		Field field = result.getTerminalDTOField();
		assertEquals("email", field.getName());
		assertEquals(User.class, field.getDeclaringClass());
	}

	@Test
	void testMapWithDifferentEntityAndDTOPath() {
		MappingResult result = mapper.map("phones.number");
		assertEquals("phones.phoneNumber", result.getPath());

		// Assertions on the terminal DTO field
		Field field = result.getTerminalDTOField();
		assertEquals("number", field.getName());
		assertEquals(Phone.class, field.getDeclaringClass());
	}

	@Test
	void testMapWithAbsoluteMapsToUsage() {
		MappingResult result = mapper.map("name.firstName");
		assertEquals("firstName", result.getPath());

		// Assertions on the terminal DTO field
		Field field = result.getTerminalDTOField();
		assertEquals("firstName", field.getName());
		assertEquals(Name.class, field.getDeclaringClass());
	}
}
