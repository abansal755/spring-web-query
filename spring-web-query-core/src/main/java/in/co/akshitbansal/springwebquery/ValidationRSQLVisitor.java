package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.*;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterables;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * Registered custom operators keyed by their implementation class.
     */
    private final Map<Class<?>, RSQLCustomOperator<?>> customOperators;

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
            Set<? extends RSQLCustomOperator<?>> customOperators,
            boolean andNodeAllowed,
            boolean orNodeAllowed,
            int maxDepth
    ) {
        this.customOperators = Collections.unmodifiableMap(customOperators
                .stream()
                .collect(Collectors.toMap(
                        RSQLCustomOperator::getClass,
                        operator -> operator,
                        // Might happen in case multiple instances of an operator are registered
                        // In that case, we can just keep one of them since they should be functionally equivalent
                        (existing, duplicate) -> existing,
                        HashMap::new
                )));
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

    /**
     * Validates that a field is marked as filterable and that the requested
     * operator is permitted by its {@link RSQLFilterable} declaration(s).
     *
     * @param field field being targeted by the request selector
     * @param operator comparison operator requested in the query
     * @param fieldPath original selector path from the request
     * @throws QueryFieldValidationException if the field is not filterable
     * @throws QueryForbiddenOperatorException if the operator is not allowed for the field
     */
    protected void validateFilterableField(@NonNull Field field, ComparisonOperator operator, String fieldPath) {
        // Retrieve the RSQLFilterable annotations on the field (if present)
        Set<RSQLFilterable> filterables = collectFilterables(field);
        // Throw exception if the field is not annotated as filterable
        if(filterables.isEmpty()) throw new QueryFieldValidationException(MessageFormat.format(
                "Filtering not allowed on field ''{0}''", fieldPath
        ), fieldPath);

        // Throw exception if the provided operator is not in the allowed set
        Set<ComparisonOperator> allowedOperators = getAllowedOperators(filterables);
        if(!allowedOperators.contains(operator)) {
            throw new QueryForbiddenOperatorException(
                    MessageFormat.format("Operator ''{0}'' not allowed on field ''{1}''", operator, fieldPath),
                    fieldPath,
                    operator,
                    allowedOperators
            );
        }
    }

    /**
     * Aggregates all allowed operators from one or more {@link RSQLFilterable}
     * declarations attached to the same field.
     *
     * @param filterables repeatable filterability declarations
     * @return deduplicated set of allowed comparison operators
     * @throws QueryConfigurationException if a referenced custom operator is not registered
     */
    private Set<ComparisonOperator> getAllowedOperators(@NonNull Set<RSQLFilterable> filterables) {
        // Collect the set of allowed operators for this field from the annotations
        // Stream of default operators defined in the annotation
        Stream<ComparisonOperator> defaultOperators = filterables
                .stream()
                .flatMap(filterable -> Arrays.stream(filterable.value()))
                .map(RSQLDefaultOperator::getOperator);
        // Stream of custom operators defined in the annotation
        // Note: The annotation references classes, which are looked up in the customOperators map
        Stream<ComparisonOperator> customOperators = filterables
                .stream()
                .flatMap(filterable -> Arrays.stream(filterable.customOperators()))
                .map(this::getCustomOperator)
                .map(RSQLCustomOperator::getComparisonOperator);
        return Stream
                .concat(defaultOperators, customOperators)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Retrieves the custom operator instance for the given operator class.
     *
     * @param clazz the custom operator class to look up
     * @return the registered custom operator instance
     * @throws QueryConfigurationException if the custom operator class is not registered
     */
    private RSQLCustomOperator<?> getCustomOperator(@NonNull Class<?> clazz) {
        RSQLCustomOperator<?> operator = customOperators.get(clazz);
        if(operator == null) throw new QueryConfigurationException(MessageFormat.format(
                "Custom operator ''{0}'' referenced in @RSQLFilterable is not registered", clazz.getSimpleName()
        ));
        return operator;
    }

    /**
     * Collects all {@link RSQLFilterable} declarations present on a field,
     * including repeatable and composed annotations.
     *
     * @param field field whose annotations are to be inspected
     * @return collected filterability declarations
     */
    private Set<RSQLFilterable> collectFilterables(Field field) {
        return collectFilterables(field.getAnnotations());
    }

    /**
     * Recursively collects {@link RSQLFilterable} declarations from a set of
     * annotations, supporting both direct and meta-annotation usage.
     *
     * @param annotations annotations to inspect
     * @return collected filterability declarations
     */
    private Set<RSQLFilterable> collectFilterables(Annotation[] annotations) {
        Set<RSQLFilterable> filterables = new HashSet<>();
        for(Annotation annotation : annotations) {
            Class<? extends Annotation> type = annotation.annotationType();
            if(annotation instanceof RSQLFilterable rsqlFilterable)
                filterables.add(rsqlFilterable);
            else if(annotation instanceof RSQLFilterables rsqlFilterables)
                filterables.addAll(Arrays.asList(rsqlFilterables.value()));
            else if(type.getName().startsWith("in.co.akshitbansal.springwebquery.annotation"))
                filterables.addAll(collectFilterables(type.getAnnotations()));
        }
        return filterables;
    }
}
