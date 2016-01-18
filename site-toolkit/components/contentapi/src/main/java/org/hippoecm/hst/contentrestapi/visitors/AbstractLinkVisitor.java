/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.contentrestapi.visitors;

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.hippoecm.repository.util.NodeIterable;

import static org.hippoecm.hst.contentrestapi.ContentRestApiResource.NAMESPACE_PREFIX;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DOCBASE;

abstract class AbstractLinkVisitor extends AbstractNodeVisitor {

    public AbstractLinkVisitor(VisitorFactory factory) {
        super(factory);
    }

    @Override
    public void visit(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        final Map<String, Object> descendantsOutput = new TreeMap<>();

        try {
            final String docbase = node.getProperty(HIPPO_DOCBASE).getString();
            if (StringUtils.isBlank(docbase) || docbase.equals("cafebabe-cafe-babe-cafe-babecafebabe")) {
                return;
            }
            // TODO link rewriting - use generic HST methods to construct URL
            descendantsOutput.put(NAMESPACE_PREFIX + ":url",
                    "http://localhost:8080/site/api/documents/" + docbase);
        } catch (RepositoryException e) {
            // log warning
        }

        destination.put(node.getName(), descendantsOutput);
        visitChildren(context, node, descendantsOutput);
    }

    protected void visitChildren(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        visit(context, new NodeIterable(node.getNodes()).iterator(), destination);
    }
}