/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.jcr;

import javax.jcr.Session;

import org.onehippo.cms7.services.search.commons.query.QueryImpl;
import org.onehippo.cms7.services.search.jcr.query.JcrQueryBuilder;
import org.onehippo.cms7.services.search.jcr.query.JcrQueryVisitor;

public class TestUtils {
    public static String getQueryAsString(final QueryImpl query, final Session session) {
        final JcrQueryBuilder builder = new JcrQueryBuilder(session);
        JcrQueryVisitor visitor = new JcrQueryVisitor(builder, session);
        query.accept(visitor);
        return builder.getQueryString();
    }
}
