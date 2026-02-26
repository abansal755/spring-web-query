package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.*;
import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;

public class DtoValidationRSQLVisitor implements RSQLVisitor<Void, Void> {

    private final Class<?> entityClass;
    private final Class<?> dtoClass;
    private final Map<String, String> fieldMappings;

    private final AnnotationUtil annotationUtil;

    public DtoValidationRSQLVisitor(Class<?> entityClass, Class<?> dtoClass, AnnotationUtil annotationUtil) {
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
        this.fieldMappings = new HashMap<>();
        this.annotationUtil = annotationUtil;
    }

    public Map<String, String> getFieldMappings() {
        return Map.copyOf(fieldMappings);
    }

    @Override
    public Void visit(AndNode node, Void param) {
        node.forEach(child -> child.accept(this));
        return null;
    }

    @Override
    public Void visit(OrNode node, Void param) {
        node.forEach(child -> child.accept(this));
        return null;
    }

    @Override
    public Void visit(ComparisonNode node, Void param) {
        validate(node);
        return null;
    }

    private void validate(ComparisonNode node) {
        // Extract the field name and operator from the RSQL node
        String dtoPath = node.getSelector();
        ComparisonOperator operator = node.getOperator();

        // Resolve the field path in the DTO class
        List<Field> dtoFields;
        try {
            dtoFields = ReflectionUtil.resolveFieldPath(dtoClass, dtoPath);
        }
        catch (Exception ex) {
            throw new QueryValidationException(MessageFormat.format(
                    "Unknown field ''{0}''", dtoPath
            ), ex);
        }
        // Validate the last field in the path for filterability and allowed operators
        validateField(dtoFields.getLast(), operator, dtoPath);


        // Construct the corresponding entity field path using the @MapsTo annotation if present
        List<String> entityPathSegments = new ArrayList<>();
        for(Field dtoField : dtoFields) {
            MapsTo mapsToAnnotation = dtoField.getAnnotation(MapsTo.class);
            if(mapsToAnnotation == null) entityPathSegments.add(dtoField.getName());
            else {
                if(mapsToAnnotation.absolute()) entityPathSegments.clear();
                entityPathSegments.add(mapsToAnnotation.field());
            }
        }
        String entityPath = String.join(".", entityPathSegments);
        // Validate that the constructed entity field path is resolvable in the entity class
        try {
            ReflectionUtil.resolveField(entityClass, entityPath);
        }
        catch (Exception x) {
            throw new QueryConfigurationException(MessageFormat.format(
                    "Unable to resolve entity field path ''{0}'' mapped from DTO path ''{1}''", entityPath, dtoPath
            ));
        }

        // Store the mapping from DTO path to entity path for later use during query construction
        fieldMappings.put(dtoPath, entityPath);
    }

    private void validateField(Field field, ComparisonOperator operator, String fieldPath) {
        // Retrieve the RsqlFilterable annotation on the field (if present)
        RsqlFilterable filterable = field.getAnnotation(RsqlFilterable.class);
        // Throw exception if the field is not annotated as filterable
        if(filterable == null) throw new QueryValidationException(MessageFormat.format(
                "Filtering not allowed on field ''{0}''", fieldPath
        ));

        // Throw exception if the provided operator is not in the allowed set
        Set<ComparisonOperator> allowedOperators = annotationUtil.getAllowedOperators(filterable);
        if(!allowedOperators.contains(operator)) throw new QueryValidationException(MessageFormat.format(
                "Operator ''{0}'' not allowed on field ''{1}''", operator, fieldPath
        ));
    }
}
