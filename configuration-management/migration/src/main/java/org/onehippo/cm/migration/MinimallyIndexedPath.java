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
package org.onehippo.cm.migration;

public class MinimallyIndexedPath implements Comparable<MinimallyIndexedPath> {

    private final String path;

    public MinimallyIndexedPath(final String name) {
        this.path = name.replaceAll("\\[1]", "");
    }

    public String getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof MinimallyIndexedPath) {
            return path.equals(((MinimallyIndexedPath) other).getPath());
        }
        return false;
    }

    @Override
    public int compareTo(final MinimallyIndexedPath other) {
        return path.compareTo(other.getPath());
    }

}
