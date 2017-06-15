/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.crisp.core.resource.util;

import java.util.Collections;
import java.util.List;

import org.onehippo.cms7.crisp.api.resource.Resource;

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
