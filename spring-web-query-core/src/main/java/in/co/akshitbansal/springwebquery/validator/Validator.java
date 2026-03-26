package in.co.akshitbansal.springwebquery.validator;

import lombok.NonNull;

public interface Validator<T> {

    void validate(@NonNull T object);
}
