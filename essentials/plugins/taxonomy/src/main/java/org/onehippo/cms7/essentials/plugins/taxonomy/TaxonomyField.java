/*
 * Copyright 2018-2023 Bloomreach
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

package org.onehippo.cms7.essentials.plugins.taxonomy;

public class TaxonomyField {
    private String jcrContentType;
    private String taxonomyName;


    public String getJcrContentType() {
        return jcrContentType;
    }

    public void setJcrContentType(final String jcrContentType) {
        this.jcrContentType = jcrContentType;
    }

    public String getTaxonomyName() {
        return taxonomyName;
    }

    public void setTaxonomyName(final String taxonomyName) {
        this.taxonomyName = taxonomyName;
    }
}
