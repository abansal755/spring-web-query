package in.co.akshitbansal.springwebquery.resolver.field;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.reflect.Field;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class ResolutionResult {

	private final String fieldName;
	private final Field terminalField;
}
