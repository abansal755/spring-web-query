package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.*;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

/**
 * Base RSQL AST visitor that enforces logical operator and depth constraints.
 *
 * <p>Subclasses are responsible for validating {@link ComparisonNode} details,
 * while this base class handles:</p>
 * <ul>
 *     <li>Disallowing AND/OR nodes based on configuration.</li>
 *     <li>Ensuring the AST does not exceed the configured maximum depth.</li>
 * </ul>
 */
public abstract class ValidationRSQLVisitor implements RSQLVisitor<Void, NodeMetadata> {

    protected final Validator<FilterableFieldValidator.Field> filterableFieldValidator;

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

    public ValidationRSQLVisitor(
            Map<Class<?>, RSQLCustomOperator<?>> customOperators,
            boolean andNodeAllowed,
            boolean orNodeAllowed,
            int maxDepth
    ) {
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
     * @return {@code null} (visitor contract)
     */
    @Override
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
     * @return {@code null} (visitor contract)
     */
    @Override
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
     * @return {@code null} (visitor contract)
     */
    @Override
    public Void visit(ComparisonNode node, NodeMetadata metadata) {
        validateNode(node, metadata);
        validateComparisonNode(node);
        return null;
    }

    /**
     * Validates a comparison node for field/operator correctness.
     *
     * @param node comparison node to validate
     */
    abstract protected void validateComparisonNode(ComparisonNode node);

    /**
     * Validates logical operator usage and depth constraints for the given node.
     *
     * @param node node to validate
     * @param metadata node metadata including current depth
     * @throws QueryValidationException if an operator is disallowed or the depth exceeds the limit
     */
    protected void validateNode(Node node, NodeMetadata metadata) {
        if((node instanceof AndNode) && !andNodeAllowed)
            throw new QueryValidationException("Logical AND operator is not allowed");
        if((node instanceof OrNode) && !orNodeAllowed)
            throw new QueryValidationException("Logical OR operator is not allowed");
        int depth = metadata.getDepth();
        if(depth > maxDepth) {
            throw new QueryValidationException(MessageFormat.format(
                    "Exceeded maximum allowed depth of RSQL Abstract Syntax Tree ({0}) at node: {1}",
                    maxDepth, node
            ));
        }
    }
}
