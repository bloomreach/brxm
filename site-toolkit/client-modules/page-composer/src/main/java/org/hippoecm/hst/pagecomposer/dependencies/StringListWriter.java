/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.pagecomposer.dependencies;

import java.util.List;

/**
 * Adds dependencies as a String into the provided {@link List}. Filters out duplicates.
 *
 * @version $Id$
 */
public class StringListWriter extends StringWriter {

    private List<String> dependencies;

    public StringListWriter(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String parse(String path) {
        return path;
    }

    @Override
    protected void write(String src) {
        if (!dependencies.contains(src)) {
            dependencies.add(src);
        }
    }
}
