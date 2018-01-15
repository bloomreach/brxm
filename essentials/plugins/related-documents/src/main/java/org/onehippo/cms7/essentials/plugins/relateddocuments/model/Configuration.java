/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.relateddocuments.model;

import java.util.List;

public class Configuration {
    private List<Field> fields;

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(final List<Field> fields) {
        this.fields = fields;
    }

    public static class Field {
        private String jcrContentType;
        private String searchPath;
        private int nrOfSuggestions;

        public String getJcrContentType() {
            return jcrContentType;
        }

        public void setJcrContentType(final String jcrContentType) {
            this.jcrContentType = jcrContentType;
        }

        public String getSearchPath() {
            return searchPath;
        }

        public void setSearchPath(final String searchPath) {
            this.searchPath = searchPath;
        }

        public int getNrOfSuggestions() {
            return nrOfSuggestions;
        }

        public void setNrOfSuggestions(final int nrOfSuggestions) {
            this.nrOfSuggestions = nrOfSuggestions;
        }
    }
}
