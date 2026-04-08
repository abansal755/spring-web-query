package in.co.akshitbansal.springwebquery.validator;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldMappingsValidatorTest {

	@Test
	void validate_rejectsDuplicateAlias() {
		FieldMappingsValidator validator = new FieldMappingsValidator();
		assertThrows(
				QueryConfigurationException.class, () -> validator.validate(List.of(
						mapping("name", "a"),
						mapping("name", "b")
				))
		);
	}

	@Test
	void validate_rejectsDuplicateTargetField() {
		FieldMappingsValidator validator = new FieldMappingsValidator();
		assertThrows(
				QueryConfigurationException.class, () -> validator.validate(List.of(
						mapping("a", "field"),
						mapping("b", "field")
				))
		);
	}

	private static FieldMapping mapping(String name, String field) {
		return new FieldMapping() {

			@Override
			public String name() {
				return name;
			}

			@Override
			public String field() {
				return field;
			}

			@Override
			public boolean allowOriginalFieldName() {
				return false;
			}

			@Override
			public Class<? extends java.lang.annotation.Annotation> annotationType() {
				return FieldMapping.class;
			}
		};
	}
}
