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
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper.MappingResult;

/**
 * Validates a parsed RSQL abstract syntax tree against the active query rules
 * and collects DTO-to-entity selector mappings for later predicate
 * construction.
 *
 * <p>This visitor is the bridge between syntactic parsing and executable query
 * generation. It walks the parsed RSQL tree once and performs the validation
 * that depends on application-specific metadata rather than on parser syntax
 * alone. In particular, it is responsible for:</p>
 * <ul>
 *   <li>rejecting logical operators that are disabled for the current query</li>
 *   <li>enforcing a maximum AST depth</li>
 *   <li>mapping DTO-facing selector paths to entity paths</li>
 *   <li>validating that the terminal DTO field permits the requested
 *       comparison operator</li>
 *   <li>recording the successful selector mappings for downstream predicate
 *       conversion</li>
 * </ul>
 *
 * <p>The visitor is stateful and intended for validating a single parsed tree.
 * During traversal it accumulates selector mappings in {@link #fieldMappings}.
 * Those mappings are later consumed by the predicate converter so that a query
 * written against the DTO contract can be executed against entity attributes.</p>
 *
 * <p>Depth handling is zero-based. The root node is typically visited with
 * depth {@code 0}, and each child node is visited with its parent's depth plus
 * one. A node is rejected only when its depth is strictly greater than the
 * configured maximum depth.</p>
 */
public class ValidationRSQLVisitor implements RSQLVisitor<Void, NodeMetadata> {

	/**
	 * Whether logical {@code AND} nodes may appear in the query.
	 */
	private final boolean allowAndOperation;

	/**
	 * Whether logical {@code OR} nodes may appear in the query.
	 */
	private final boolean allowOrOperation;

	/**
	 * Maximum zero-based AST depth accepted during validation.
	 */
	private final int maxASTDepth;

	/**
	 * Mapper used to translate DTO selectors into entity paths.
	 */
	private final DTOToEntityPathMapper pathMapper;

	/**
	 * Validator used to check operator permissions on terminal DTO fields.
	 */
	private final FilterableFieldValidator filterableFieldValidator;

	/**
	 * Collected mapping of request DTO selectors to resolved entity paths.
	 */
	private final Map<String, String> fieldMappings;

	/**
	 * Creates a visitor for validating one parsed RSQL tree.
	 *
	 * <p>The supplied collaborators define the validation rules for the current
	 * query execution. The visitor itself does not parse selectors or inspect
	 * field annotations directly; instead it delegates path translation to
	 * {@code pathMapper} and field/operator permission checks to
	 * {@code filterableFieldValidator}.</p>
	 *
	 * @param allowAndOperation whether logical {@code AND} nodes are allowed
	 * @param allowOrOperation whether logical {@code OR} nodes are allowed
	 * @param maxASTDepth maximum zero-based AST depth accepted during validation
	 * @param pathMapper mapper used to resolve DTO selectors to entity paths
	 * @param filterableFieldValidator validator for terminal-field filterability
	 */
	public ValidationRSQLVisitor(
			boolean allowAndOperation,
			boolean allowOrOperation,
			int maxASTDepth,
			DTOToEntityPathMapper pathMapper,
			FilterableFieldValidator filterableFieldValidator
	) {
		this.allowAndOperation = allowAndOperation;
		this.allowOrOperation = allowOrOperation;
		this.maxASTDepth = maxASTDepth;
		this.pathMapper = pathMapper;
		this.filterableFieldValidator = filterableFieldValidator;

		this.fieldMappings = new HashMap<>();
	}

	/**
	 * Returns the DTO-to-entity selector mappings discovered so far during
	 * traversal.
	 *
	 * <p>Only successfully validated comparison selectors are added to this map.
	 * Logical nodes do not contribute entries. The returned value is an
	 * unmodifiable view keyed by the original DTO selector string, with each
	 * value containing the entity path that should be used for predicate
	 * construction.</p>
	 *
	 * <p>If the same DTO selector appears multiple times in the query, later
	 * visits replace earlier entries for the same key. In practice the mapping is
	 * deterministic for a given DTO/entity pair, so repeated selectors still
	 * resolve to the same entity path.</p>
	 *
	 * @return unmodifiable view of the collected selector mappings
	 */
	public Map<String, String> getFieldMappings() {
		return Collections.unmodifiableMap(fieldMappings);
	}

	/**
	 * Validates a logical {@code AND} node and then recursively visits its
	 * children.
	 *
	 * <p>This method first validates the current node's logical operator and
	 * depth. If validation succeeds, each child is visited with a
	 * {@link NodeMetadata} instance whose depth is one greater than the current
	 * node's depth.</p>
	 *
	 * @param node logical {@code AND} node to validate
	 * @param metadata traversal metadata for the current node
	 *
	 * @return always {@code null}
	 *
	 * @throws QueryForbiddenLogicalOperatorException if {@code AND} is disabled
	 * for the current query
	 * @throws QueryMaxASTDepthExceededException if this node exceeds the
	 * configured maximum depth
	 */
	@Override
	@Nullable
	public Void visit(@NonNull AndNode node, @NonNull NodeMetadata metadata) {
		validateNode(node, metadata);
		node.forEach(child -> child.accept(this, NodeMetadata.of(metadata.getDepth() + 1)));
		return null;
	}

	/**
	 * Validates a logical {@code OR} node and then recursively visits its
	 * children.
	 *
	 * <p>This method follows the same traversal pattern as
	 * {@link #visit(AndNode, NodeMetadata)}: it validates the current node first
	 * and then visits each child at the next depth level.</p>
	 *
	 * @param node logical {@code OR} node to validate
	 * @param metadata traversal metadata for the current node
	 *
	 * @return always {@code null}
	 *
	 * @throws QueryForbiddenLogicalOperatorException if {@code OR} is disabled
	 * for the current query
	 * @throws QueryMaxASTDepthExceededException if this node exceeds the
	 * configured maximum depth
	 */
	@Override
	@Nullable
	public Void visit(@NonNull OrNode node, @NonNull NodeMetadata metadata) {
		validateNode(node, metadata);
		node.forEach(child -> child.accept(this, NodeMetadata.of(metadata.getDepth() + 1)));
		return null;
	}

	/**
	 * Validates a comparison node, resolves its selector path, and records the
	 * resulting entity mapping for downstream predicate creation.
	 *
	 * <p>The validation flow for a comparison node is:</p>
	 * <ol>
	 *   <li>validate the current node's depth</li>
	 *   <li>extract the DTO selector and requested comparison operator</li>
	 *   <li>map the DTO selector to an entity path</li>
	 *   <li>validate that the terminal DTO field allows the requested operator</li>
	 *   <li>store the successful selector-to-entity mapping</li>
	 * </ol>
	 *
	 * <p>No predicate is created here. This visitor only validates the request
	 * and gathers the metadata needed by later query construction steps.</p>
	 *
	 * @param node comparison node to validate
	 * @param metadata traversal metadata for the current node
	 *
	 * @return always {@code null}
	 *
	 * @throws QueryMaxASTDepthExceededException if this node exceeds the
	 * configured maximum depth
	 * @throws in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException
	 * if the selector is invalid or filtering is not permitted on the terminal
	 * DTO field
	 * @throws in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException
	 * if the terminal DTO field does not allow the requested operator
	 * @throws in.co.akshitbansal.springwebquery.exception.QueryConfigurationException
	 * if the DTO selector resolves but maps to an invalid entity path
	 */
	@Override
	@Nullable
	public Void visit(@NonNull ComparisonNode node, @NonNull NodeMetadata metadata) {
		validateNode(node, metadata);
		// Extract the field name and operator from the RSQL node
		String dtoPath = node.getSelector();
		ComparisonOperator operator = node.getOperator();

		// Convert the DTO path to an entity path
		MappingResult mappingResult = pathMapper.map(dtoPath);

		// Validate the terminal field of the mapped entity path
		filterableFieldValidator.validate(mappingResult.getTerminalDTOField(), operator, dtoPath);

		// Store the mapping from DTO path to entity path for later use during query construction
		fieldMappings.put(dtoPath, mappingResult.getPath());
		return null;
	}

	/**
	 * Enforces the logical-operator and depth rules for the current node.
	 *
	 * <p>Logical-operator checks apply only to {@link AndNode} and
	 * {@link OrNode} instances. Depth validation applies to every visited node,
	 * including comparison nodes.</p>
	 *
	 * @param node node being validated
	 * @param metadata traversal metadata for the node
	 *
	 * @throws QueryForbiddenLogicalOperatorException if the node represents a
	 * disabled logical operator
	 * @throws QueryMaxASTDepthExceededException if the node depth is greater than
	 * the configured maximum
	 */
	private void validateNode(Node node, NodeMetadata metadata) {
		if ((node instanceof AndNode andNode) && !allowAndOperation)
			throw new QueryForbiddenLogicalOperatorException("Logical AND operator is not allowed", andNode.getOperator());
		if ((node instanceof OrNode orNode) && !allowOrOperation)
			throw new QueryForbiddenLogicalOperatorException("Logical OR operator is not allowed", orNode.getOperator());
		int depth = metadata.getDepth();
		if (depth > maxASTDepth) {
			throw new QueryMaxASTDepthExceededException(
					MessageFormat.format(
							"Exceeded maximum allowed depth of RSQL Abstract Syntax Tree ({0}) at node: {1}",
							maxASTDepth, node
					), maxASTDepth
			);
		}
	}
}
