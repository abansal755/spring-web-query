package in.co.akshitbansal.springwebquery.util;

import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import lombok.NonNull;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.text.MessageFormat;

public class AnnotationUtil {

    public static WebQuery resolveWebQueryFromParameter(@NonNull MethodParameter parameter) {
        // Retrieve the controller method
        Method controllerMethod = parameter.getMethod();
        // Ensure that the method is not null (should not happen for valid controller parameters)
        if(controllerMethod == null) throw new QueryConfigurationException(MessageFormat.format(
                "Unable to resolve controller method for parameter {0}", parameter
        ));
        // Retrieve the @WebQuery annotation from the controller method to access query configuration
        WebQuery webQueryAnnotation = controllerMethod.getAnnotation(WebQuery.class);
        // Ensure that the method is annotated with @WebQuery to access query configuration
        if(webQueryAnnotation == null)
            throw new QueryConfigurationException(MessageFormat.format(
                    "Controller method {0} must be annotated with @WebQuery to use @RsqlSpec parameters",
                    controllerMethod
            ));
        return webQueryAnnotation;
    }
}
