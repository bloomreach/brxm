/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms.channelmanager.content.document.util;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;

/**
 * Represents the path to field value in a document. It consists of a hierarchy of segments that are unique per level.
 * Segments can (optionally) have a numbered suffix in square brackets to distinguish 'same name siblings'.
 */
public class FieldPath {

    public static final String SEPARATOR = "/";
    private static final Pattern NUMBERED_SUFFIX = Pattern.compile("^\\[\\d\\]$");

    private final String firstSegment;
    private final String remainingSegments;

    /**
     * Creates a field path from a string
     * @param fieldPath zero or more segments separated by slashes
     */
    public FieldPath(final String fieldPath) {
        if (fieldPath == null) {
            firstSegment = null;
            remainingSegments = null;
        } else {
            firstSegment = nullWhenEmpty(StringUtils.substringBefore(fieldPath, SEPARATOR));
            remainingSegments = nullWhenEmpty(StringUtils.substringAfter(fieldPath, SEPARATOR));
        }
    }

    private static String nullWhenEmpty(final String s) {
        return StringUtils.isEmpty(s) ? null : s;
    }

    /**
     * @return whether this field path consists of zero segments.
     */
    public boolean isEmpty() {
        return StringUtils.isEmpty(firstSegment);
    }

    /**
     * @param segment the segment to compare to
     * @return whether this field path only consists of exactly the given segment.
     */
    public boolean is(final String segment) {
        return firstSegment != null && firstSegment.equals(segment) && StringUtils.isEmpty(remainingSegments);
    }

    /**
     * @param segment the segment to compare the first segment to. A numbered suffix in the first segment is ignored
     *                when comparing it to the given segment. For example, the field path "foo[2]/bar" starts with
     *                "foo[2]", but is also starts with "foo". It does not start with "fo", though.
     * @return whether the first segment of this field path equals the given segment.
     */
    public boolean startsWith(final String segment) {
        return StringUtils.isNotEmpty(segment) && matchesFirstSegment(segment);
    }

    private boolean matchesFirstSegment(final String segment) {
        if (firstSegment.equals(segment)) {
            return true;
        }
        final String suffix = StringUtils.substringAfter(firstSegment, segment);
        if (StringUtils.isEmpty(suffix)) {
            return false;
        }
        return NUMBERED_SUFFIX.matcher(suffix).matches();
    }

    /**
     * @return the first segment of this field path, or null if this field path is empty.
     */
    public String getFirstSegment() {
        return firstSegment;
    }

    /**
     * @return the field path consisting of all segments except the first one.
     */
    public FieldPath getRemainingSegments() {
        return new FieldPath(remainingSegments);
    }

    /**
     * @return the first segment of this field path without a numbered suffix.
     */
    public String getFirstSegmentName() {
        if (!firstSegment.endsWith("]")) {
            return firstSegment;
        }

        return StringUtils.substringBeforeLast(firstSegment, "[");
    }

    /**
     * @return the JCR index of the first segment of this field path, defaults to 1.
     */
    public int getFirstSegmentIndex() {
        if (!firstSegment.endsWith("]")) {
            return 1;
        }

        final String indexAsString = substringBeforeLast(substringAfterLast(firstSegment, "["), "]");
        return Integer.parseInt(indexAsString);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof FieldPath) {
            FieldPath other = (FieldPath) o;
            return StringUtils.equals(firstSegment, other.firstSegment) &&
                    StringUtils.equals(remainingSegments, other.remainingSegments);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = firstSegment != null ? firstSegment.hashCode() : 0;
        result += 17 * (remainingSegments != null ? remainingSegments.hashCode() : 1);
        return result;
    }

    @Override
    public String toString() {
        if (firstSegment == null) {
            return StringUtils.EMPTY;
        }
        if (StringUtils.isEmpty(remainingSegments)) {
            return firstSegment;
        }
        return firstSegment + SEPARATOR + remainingSegments;
    }
}
