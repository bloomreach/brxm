/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.query.builder;

class OrderByConstruct {

    private String fieldName;
    private boolean ascending = true;
    private boolean caseSensitive = true;

    public OrderByConstruct(String fieldName, boolean ascending) {
        this.fieldName = fieldName;
        this.ascending = ascending;
    }

    public String fieldName() {
        return fieldName;
    }

    public OrderByConstruct ascending(final boolean ascending) {
        this.ascending = ascending;
        return this;
    }

    public boolean ascending() {
        return ascending;
    }

    public OrderByConstruct caseSensitive(final boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }

    public boolean caseSensitive() {
        return caseSensitive;
    }
}
