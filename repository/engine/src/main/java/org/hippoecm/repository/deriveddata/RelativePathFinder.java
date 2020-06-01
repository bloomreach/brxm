/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.repository.deriveddata;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelativePathFinder {

    private static final Logger log = LoggerFactory.getLogger(RelativePathFinder.class);
    private final String nodePath;
    private final String propertyPath;

    public RelativePathFinder(final String nodePath, final String propertyPath) {
        Validate.notNull(nodePath);
        Validate.notNull(propertyPath);
        log.debug("nodePath:{}", nodePath);
        log.debug("propertyPath:{}", propertyPath);
        this.nodePath = nodePath.replaceAll("\\[[1-3]\\]", "");;
        this.propertyPath = propertyPath.replaceAll("\\[[1-3]\\]", "");
        Validate.isTrue(this.propertyPath.startsWith(this.nodePath), "property path:%s does not start with property path:%s ", this.propertyPath, this.nodePath);
    }

    public String getRelativePath() {
        final String propertyPathWithoutSameNameSibblingsCounter = propertyPath.replaceAll("\\[[1-3]\\]","");
        final String relativePath = propertyPathWithoutSameNameSibblingsCounter.substring(nodePath.length()+1);
        Validate.isTrue(!relativePath.startsWith("/"));
        return relativePath;
    }
}
