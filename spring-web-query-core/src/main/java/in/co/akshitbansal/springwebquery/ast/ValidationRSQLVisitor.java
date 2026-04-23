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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper.MappingResult;

public class ValidationRSQLVisitor implements RSQLVisitor<Void, NodeMetadata> {

	private final boolean andNodeAllowed;
	private final boolean orNodeAllowed;
	private final int maxDepth;

	private final DTOToEntityPathMapper pathMapper;
	private final FilterableFieldValidator filterableFieldValidator;

	private final Map<String, String> fieldMappings;

	public ValidationRSQLVisitor(
			boolean andNodeAllowed,
			boolean orNodeAllowed,
			int maxDepth,
			DTOToEntityPathMapper pathMapper,
			FilterableFieldValidator filterableFieldValidator
	) {
		this.andNodeAllowed = andNodeAllowed;
		this.orNodeAllowed = orNodeAllowed;
		this.maxDepth = maxDepth;
		this.pathMapper = pathMapper;
		this.filterableFieldValidator = filterableFieldValidator;

		this.fieldMappings = new HashMap<>();
	}

	public Map<String, String> getFieldMappings() {
		return Collections.unmodifiableMap(fieldMappings);
	}

	@Override
	public Void visit(@NonNull AndNode node, @NonNull NodeMetadata metadata) {
		validateNode(node, metadata);
		node.forEach(child -> child.accept(this, NodeMetadata.of(metadata.getDepth() + 1)));
		return null;
	}

	@Override
	public Void visit(@NonNull OrNode node, @NonNull NodeMetadata metadata) {
		validateNode(node, metadata);
		node.forEach(child -> child.accept(this, NodeMetadata.of(metadata.getDepth() + 1)));
		return null;
	}

	@Override
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

	private void validateNode(Node node, NodeMetadata metadata) {
		if ((node instanceof AndNode andNode) && !andNodeAllowed)
			throw new QueryForbiddenLogicalOperatorException("Logical AND operator is not allowed", andNode.getOperator());
		if ((node instanceof OrNode orNode) && !orNodeAllowed)
			throw new QueryForbiddenLogicalOperatorException("Logical OR operator is not allowed", orNode.getOperator());
		int depth = metadata.getDepth();
		if (depth > maxDepth) {
			throw new QueryMaxASTDepthExceededException(
					MessageFormat.format(
							"Exceeded maximum allowed depth of RSQL Abstract Syntax Tree ({0}) at node: {1}",
							maxDepth, node
					), maxDepth
			);
		}
	}
}
