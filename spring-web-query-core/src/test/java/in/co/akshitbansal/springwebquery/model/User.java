package in.co.akshitbansal.springwebquery.model;

import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterableEquality;
import in.co.akshitbansal.springwebquery.customoperator.IsLongGreaterThanFiveOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import lombok.Data;

import java.util.List;

@Data
public class User {

	@RSQLFilterable(customOperators = IsLongGreaterThanFiveOperator.class)
	@RSQLFilterableEquality
	private Long id;

	// Explicitly not annotated with @RSQLFilterable
	private String email;

	private Name name;
	private Phone[] phones;
	private List<Address> addresses;
}
