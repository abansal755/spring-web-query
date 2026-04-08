package in.co.akshitbansal.springwebquery.ast;

import cz.jirutka.rsql.parser.ast.*;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.resolver.FieldResolver;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import org.jspecify.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Map;

/**
 * Base RSQL AST visitor that enforces structural validation rules while
 * delegating selector-specific checks to subclasses.
 *
 * <p>This base class is responsible for:</p>
 * <ul>
 *     <li>Disallowing logical AND/OR nodes based on configuration.</li>
 *     <li>Ensuring the AST does not exceed the configured maximum depth.</li>
 *     <li>Providing access to a resolver for selector-to-entity path translation.</li>
 *     <li>Providing access to a reusable validator for filterable terminal fields.</li>
 * </ul>
 *
 * <p>Concrete subclasses validate individual {@link ComparisonNode} selectors
 * against either entity fields or DTO fields and invoke the shared
 * {@link #filterableFieldValidator} as needed.</p>
 */
public abstract class AbstractValidationRSQLVisitor implements RSQLVisitor<Void, NodeMetadata> {

	/**
	 * Resolver used by subclasses to translate selectors into entity-backed
	 * paths and expose the terminal field for validation.
	 */
	protected final FieldResolver fieldResolver;

	/**
	 * Validator used by subclasses to enforce {@code @RSQLFilterable}
	 * constraints on resolved terminal fields.
	 */
	protected final Validator<FilterableFieldValidator.FilterableField> filterableFieldValidator;

	/**
	 * Whether logical AND operator is allowed in the query.
	 */
	private final boolean andNodeAllowed;

	/**
	 * Whether logical OR operator is allowed in the query.
	 */
	private final boolean orNodeAllowed;

	/**
	 * Maximum allowed depth for the RSQL AST during validation.
	 */
	private final int maxDepth;

	/**
	 * Creates a validation visitor with the supplied structural limits and
	 * custom operator registry.
	 *
	 * @param fieldResolver resolver used for selector-path resolution in concrete visitors
	 * @param customOperators registered custom operators keyed by implementation class
	 * @param andNodeAllowed whether logical AND nodes are allowed
	 * @param orNodeAllowed whether logical OR nodes are allowed
	 * @param maxDepth maximum allowed AST depth
	 */
	protected AbstractValidationRSQLVisitor(
			FieldResolver fieldResolver,
			Map<Class<?>, RSQLCustomOperator<?>> customOperators,
			boolean andNodeAllowed,
			boolean orNodeAllowed,
			int maxDepth
	) {
		this.fieldResolver = fieldResolver;
		this.filterableFieldValidator = new FilterableFieldValidator(customOperators);
		this.andNodeAllowed = andNodeAllowed;
		this.orNodeAllowed = orNodeAllowed;
		this.maxDepth = maxDepth;
	}

	/**
	 * Validates AND nodes and recursively visits child nodes.
	 *
	 * @param node AND node to validate
	 * @param metadata traversal metadata including current depth
	 *
	 * @return {@code null} (visitor contract)
	 */
	@Override
	@Nullable
	public Void visit(AndNode node, NodeMetadata metadata) {
		validateNode(node, metadata);
		node.forEach(child -> child.accept(this, NodeMetadata.of(metadata.getDepth() + 1)));
		return null;
	}

	/**
	 * Validates OR nodes and recursively visits child nodes.
	 *
	 * @param node OR node to validate
	 * @param metadata traversal metadata including current depth
	 *
	 * @return {@code null} (visitor contract)
	 */
	@Override
	@Nullable
	public Void visit(OrNode node, NodeMetadata metadata) {
		validateNode(node, metadata);
		node.forEach(child -> child.accept(this, NodeMetadata.of(metadata.getDepth() + 1)));
		return null;
	}

	/**
	 * Validates comparison nodes and delegates field/operator validation.
	 *
	 * @param node comparison node to validate
	 * @param metadata traversal metadata including current depth
	 *
	 * @return {@code null} (visitor contract)
	 */
	@Override
	@Nullable
	public Void visit(ComparisonNode node, NodeMetadata metadata) {
		validateNode(node, metadata);
		validateComparisonNode(node);
		return null;
	}

	/**
	 * Validates a comparison node for field/operator correctness.
	 *
	 * <p>Concrete subclasses implement this hook to resolve selectors against
	 * their active contract type and then invoke {@link #filterableFieldValidator}
	 * on the resolved terminal field.</p>
	 *
	 * @param node comparison node to validate
	 */
	protected abstract void validateComparisonNode(ComparisonNode node);

	/**
	 * Validates logical operator usage and depth constraints for the given node.
	 *
	 * @param node node to validate
	 * @param metadata node metadata including current depth
	 *
	 * @throws QueryValidationException if an operator is disallowed or the depth exceeds the limit
	 */
	protected void validateNode(Node node, NodeMetadata metadata) {
		if ((node instanceof AndNode) && !andNodeAllowed)
			throw new QueryValidationException("Logical AND operator is not allowed");
		if ((node instanceof OrNode) && !orNodeAllowed)
			throw new QueryValidationException("Logical OR operator is not allowed");
		int depth = metadata.getDepth();
		if (depth > maxDepth) {
			throw new QueryValidationException(MessageFormat.format(
					"Exceeded maximum allowed depth of RSQL Abstract Syntax Tree ({0}) at node: {1}",
					maxDepth, node
			));
		}
	}
}
