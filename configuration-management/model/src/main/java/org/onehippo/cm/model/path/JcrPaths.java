package org.onehippo.cm.model.path;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.google.common.collect.ImmutableList;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.joining;

/**
 * A factory class for constructing new {@link JcrPath} and {@link JcrPathSegment} instances
 */
public class JcrPaths {

    /**
     * A constant value representing the path of the JCR root node.
     */
    public static final JcrPath ROOT = new JcrPathImpl(ImmutableList.of(), false, true);

    /**
     * A constant value representing the name of the JCR root node, which typically does not appear explicitly in a {@link org.onehippo.cm.model.impl.path.JcrPath}.
     */
    public static final JcrPathSegment ROOT_NAME = new JcrPathSegmentImpl("", 0);

    /**
     * Static factory for JcrPath instances.
     * @see java.nio.file.Paths#get(String, String...)
     */
    public static JcrPath getPath(String path, final String... more) {

        // do this check now to check for null or pure whitespace
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("Path string must not be blank!");
        }

        // "/node/" => "/node"; "///" => ""
        if (path.endsWith("/") && !path.equals("/")) {
            path = StringUtils.stripEnd(path, "/");
        }

        // collapse "more" into a single path, so that this will work: get("/", "  ", "/parent/child", "/grandchild/");
        if (ArrayUtils.isNotEmpty(more)) {
            path += Arrays.stream(more).map(StringUtils::stripToEmpty).map(s -> StringUtils.stripEnd(s, "/"))
                    .filter(StringUtils::isNotBlank).collect(joining("/", "/", ""));
        }

        // root is special
        if (path.equals("/")) {
            return ROOT;
        }

        // do this check now to detect weirdness like "////"
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("Path string must contain non-empty segments!");
        }

        final boolean relative = !path.startsWith("/");
        final String[] segments = StringUtils.strip(path, "/").split("/");

        final ImmutableList<JcrPathSegment> names =
                Arrays.stream(segments).map(JcrPaths::getSegment).collect(ImmutableList.toImmutableList());

        return new JcrPathImpl(names, relative);
    }

    /**
     * Static factory for JcrPathSegment instances; parses the index from the input String.
     */
    public static JcrPathSegment getSegment(final String fullName) {
        if (fullName == null) {
            throw new IllegalArgumentException("Name must not be null!");
        }

        if (fullName.equals("") || fullName.equals("/")) {
            return ROOT_NAME;
        }
        else {
            return new JcrPathSegmentImpl(fullName);
        }
    }

    /**
     * Static factory for JcrPathSegment instances with separate index param.
     */
    public static JcrPathSegment getSegment(final String name, final int index) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null!");
        }

        if (name.equals("") || name.equals("/")) {
            return ROOT_NAME;
        }
        else {
            return new JcrPathSegmentImpl(name, index);
        }
    }

    /**
     * Static factory for JcrPathSegment instances from JCR node. Sets an explicit index iff the node has SNS.
     */
    public static JcrPathSegment getSegment(final Node node) throws RepositoryException {
        if (node == null) {
            throw new IllegalArgumentException("Node must not be null!");
        }

        if (node.getDepth() == 0) {
            return ROOT_NAME;
        } else {
            int index = node.getIndex();
            if (index == 1 && node.getParent().getNodes(node.getName()).getSize() == 1) {
                index = 0;
            }
            return new JcrPathSegmentImpl(node.getName(), index);
        }
    }
}
