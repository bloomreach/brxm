package com.onehippo.cms7.crisp.core.resource.util;

import java.util.Collections;
import java.util.List;

import com.onehippo.cms7.crisp.api.resource.Resource;

public class ResourceCollectionUtils {

    private ResourceCollectionUtils() {
    }

    public static List<Resource> createSubList(List<Resource> source, long offset, long limit) {
        if (offset < 0 || offset >= source.size()) {
            throw new IllegalArgumentException("Invalid offset: " + offset + " (size = " + source.size() + ")");
        }

        if (limit == 0) {
            return Collections.emptyList();
        }

        if ((offset == 0 && limit < 0) || (offset == 0 && limit == source.size())) {
            return source;
        }

        long endIndex;

        if (limit > source.size()) {
            endIndex = source.size();
        } else {
            endIndex = Math.min(source.size(), offset + limit);
        }

        if (offset == 0) {
            return source.subList((int) offset, (int) endIndex);
        } else {
            if (limit < 0) {
                return source.subList((int) offset, source.size());
            } else {
                return source.subList((int) offset, (int) endIndex);
            }
        }
    }
}
