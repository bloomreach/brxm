/*
 * Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.freemarker.jcr;

public class RepositorySource {

    private final String template;
    private final  String absJcrPath;
    private final long placeHolderLastModified;
    private final boolean found;

    public static final RepositorySource notFound(final String absJcrPath) {
        return new RepositorySource(absJcrPath);
    }

    public static final RepositorySource found(final String absJcrPath, final String template) {
        return new RepositorySource(absJcrPath, template);
    }

    private RepositorySource(final String absJcrPath){
        this.absJcrPath = absJcrPath;
        template = null;
        found = false;
        placeHolderLastModified = System.currentTimeMillis();
    }

    private RepositorySource(final String absJcrPath, final String template){
        this.absJcrPath = absJcrPath;
        this.template = template;
        found = true;
        this.placeHolderLastModified = System.currentTimeMillis();
        
    }

    public String getAbsJcrPath() {
        return absJcrPath;
    }

    public String getTemplate() {
        return template;
    }

    public long getPlaceHolderLastModified() {
        return placeHolderLastModified;
    }

    public boolean isFound() {
        return found;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RepositorySource that = (RepositorySource) o;

        if (found != that.found) {
            return false;
        }
        if (placeHolderLastModified != that.placeHolderLastModified) {
            return false;
        }
        if (absJcrPath != null ? !absJcrPath.equals(that.absJcrPath) : that.absJcrPath != null) {
            return false;
        }
        if (template != null ? !template.equals(that.template) : that.template != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = template != null ? template.hashCode() : 0;
        result = 31 * result + (absJcrPath != null ? absJcrPath.hashCode() : 0);
        result = 31 * result + (int) (placeHolderLastModified ^ (placeHolderLastModified >>> 32));
        result = 31 * result + (found ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RepositorySource{" +
                " absJcrPath='" + absJcrPath + '\'' +
                ", placeHolderLastModified=" + placeHolderLastModified +
                ", found=" + found +
                '}';
    }
}
