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

package in.co.akshitbansal.springwebquery.ast;

import cz.jirutka.rsql.parser.ast.*;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenLogicalOperatorException;
import in.co.akshitbansal.springwebquery.exception.QueryMaxASTDepthExceededException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolver;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.text.MessageFormat;

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
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractValidationRSQLVisitor implements RSQLVisitor<Void, NodeMetadata> {

	/**
	 * Resolver used by subclasses to translate selectors into entity-backed
	 * paths and expose the terminal field for validation.
	 */
	@NonNull
	protected final FieldResolver fieldResolver;

	/**
	 * Validator used by subclasses to enforce {@code @RSQLFilterable}
	 * constraints on resolved terminal fields.
	 */
	@NonNull
	protected final FilterableFieldValidator filterableFieldValidator;

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
	 * Validates AND nodes and recursively visits child nodes.
	 *
	 * @param node AND node to validate
	 * @param metadata traversal metadata including current depth
	 *
	 * @return {@code null} (visitor contract)
	 */
	@Override
	@Nullable
	public Void visit(@NonNull AndNode node, @NonNull NodeMetadata metadata) {
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
	public Void visit(@NonNull OrNode node, @NonNull NodeMetadata metadata) {
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
	public Void visit(@NonNull ComparisonNode node, @NonNull NodeMetadata metadata) {
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
	 * @throws QueryForbiddenLogicalOperatorException if a logical AND/OR operator is disallowed
	 * @throws QueryMaxASTDepthExceededException if the current node depth exceeds the configured maximum
	 */
	protected void validateNode(Node node, NodeMetadata metadata) {
		if ((node instanceof AndNode andNode) && !andNodeAllowed)
			throw new QueryForbiddenLogicalOperatorException("Logical AND operator is not allowed", andNode.getOperator());
		if ((node instanceof OrNode orNode) && !orNodeAllowed)
			throw new QueryForbiddenLogicalOperatorException("Logical OR operator is not allowed", orNode.getOperator());
		int depth = metadata.getDepth();
		if (depth > maxDepth) {
			throw new QueryMaxASTDepthExceededException(MessageFormat.format(
					"Exceeded maximum allowed depth of RSQL Abstract Syntax Tree ({0}) at node: {1}",
					maxDepth, node
			), maxDepth);
		}
	}
}
