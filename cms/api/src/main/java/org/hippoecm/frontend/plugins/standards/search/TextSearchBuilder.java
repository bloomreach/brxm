/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.joining;

public class TextSearchBuilder extends GeneralSearchBuilder implements IClusterable {

    static final Logger log = LoggerFactory.getLogger(TextSearchBuilder.class);

    public TextSearchBuilder() {
        super();
    }

    public TextSearchBuilder(final String queryName) {
        super(queryName);
    }

    @Override
    protected void appendScope(final StringBuilder sb) {
        final Session session = UserSession.get().getJcrSession();

        final String scopes = Arrays.stream(getScope())
                .map(scope -> {
                    try {
                        final Node content = (Node) session.getItem(scope);
                        if (content.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                            return String.format("hippo:paths = '%s'", content.getIdentifier());
                        } else {
                            log.info("Skipping non-referenceable node at path {}", scope);
                        }
                    } catch (final PathNotFoundException e) {
                        log.warn("Search path not found: " + scope);
                    } catch (final RepositoryException e) {
                        throw new IllegalStateException("An error occurred while constructing the text search where-clause part", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(joining(" or "));

        if (StringUtils.isNotBlank(scopes)) {
            if (!sb.toString().endsWith("[")) {
                sb.append(" and ");
            }
            sb.append("(");
            sb.append(scopes);
            sb.append(")");
        }
    }

    /**
     * Translates the included primary type(s) to a filter for a JCR xpath query. Adds an xpath condition with
     * configured document types or a clause that queries {@literal hippo:document} and all its subtypes if no document
     * type filter is configured
     */
    @Override
    protected void appendIncludedPrimaryNodeTypeFilter(final StringBuilder sb) {
        final String[] includePrimaryTypes = getIncludePrimaryTypes();
        if (includePrimaryTypes == null || includePrimaryTypes.length == 0) {
            sb.append("//element(*, " + HippoNodeType.NT_DOCUMENT + ")");
        } else {
            super.appendIncludedPrimaryNodeTypeFilter(sb);
        }
    }
}
