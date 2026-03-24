package in.co.akshitbansal.springwebquery;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Carries metadata while traversing the RSQL AST.
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class NodeMetadata {

    /**
     * Current depth in the AST traversal.
     */
    private final int depth;

    /**
     * Creates metadata for the given depth.
     *
     * @param depth current depth in the AST
     * @return metadata instance
     */
    public static NodeMetadata of(int depth) {
        return new NodeMetadata(depth);
    }
}
