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
package org.hippoecm.frontend.plugins.standards.search;

import org.hippoecm.frontend.plugins.cms.admin.search.AdminTextSearchBuilder;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

public class AdminTextSearchBuilderTest {

    @Test
    public void testPathFilter() {
        String scopePath = "/hippo:configuration/hippo:users";

        AdminTextSearchBuilder builder = new AdminTextSearchBuilder();
        builder.setText("x");
        builder.setScope(new String[]{scopePath});
        final StringBuilder queryStringBuilder = builder.getQueryStringBuilder();
        final String xpathQuery = queryStringBuilder.toString();

        assertThat(xpathQuery, startsWith("/jcr:root" + scopePath));
    }

    @Test
    public void testSystemTypeFilter() {
        AdminTextSearchBuilder builder = new AdminTextSearchBuilder();
        builder.setText("x");
        final StringBuilder queryStringBuilder = builder.getQueryStringBuilder();
        final String xpathQuery = queryStringBuilder.toString();

        assertThat(xpathQuery, containsString("[(not(@hipposys:system) or @hipposys:system='false')]"));
    }

    @Test
    public void testOnePrimaryTypeIncludeSubtype() {
        AdminTextSearchBuilder builder = new AdminTextSearchBuilder();
        builder.setText("x");
        builder.setIncludePrimaryTypes(new String[]{HippoNodeType.NT_USER});
        final StringBuilder queryStringBuilder = builder.getQueryStringBuilder();
        final String xpathQuery = queryStringBuilder.toString();

        assertThat(xpathQuery, containsString("element(*," + HippoNodeType.NT_USER + ")"));
    }
}
