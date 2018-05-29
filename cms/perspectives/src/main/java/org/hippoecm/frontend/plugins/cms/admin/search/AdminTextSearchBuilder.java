/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.search;

import org.hippoecm.frontend.plugins.standards.search.GeneralSearchBuilder;

public class AdminTextSearchBuilder extends GeneralSearchBuilder {

    public AdminTextSearchBuilder() {
        super();
    }

    @Override
    protected void appendIncludedPrimaryNodeTypeFilter(final StringBuilder sb) {
        final String[] scopes = getScope();
        if (scopes.length > 0) {
            sb.append("/jcr:root");
            sb.append(scopes[0]);
        }

        // for 1 primary type, use element() to retrieve also subtypes
        final String[] primaryTypes = getIncludePrimaryTypes();
        if (primaryTypes != null && primaryTypes.length == 1) {
            sb.append("//element(*,");
            sb.append(primaryTypes[0]);
            sb.append(")");
            return;
        }

        super.appendIncludedPrimaryNodeTypeFilter(sb);
    }

    @Override
    protected void appendExtraWhereClauses(final StringBuilder queryStringBuilder) {
        queryStringBuilder.append("[(not(@hipposys:system) or @hipposys:system='false')]");
    }
}
