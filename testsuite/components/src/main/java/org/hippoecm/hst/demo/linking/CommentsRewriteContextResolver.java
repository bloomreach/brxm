/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.linking;

import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.linking.RewriteContext;
import org.hippoecm.hst.core.linking.RewriteContextException;
import org.hippoecm.hst.core.linking.RewriteContextResolver;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommentsRewriteContextResolver implements RewriteContextResolver {

    private static final Logger log = LoggerFactory.getLogger(CommentsRewriteContextResolver.class);

    @Override
    public RewriteContext resolve(final Node node, final Mount mount, final HstRequestContext context,
                          final boolean canonical, final boolean navigationStateful) {
        try {
            if (!node.isNodeType(HippoNodeType.NT_HANDLE)) {
                return new RewriteContext(node.getPath(), mount, canonical, navigationStateful);
            }

            final Node document = JcrUtils.getNodeIfExists(node, node.getName());
            if (document == null) {
                return new RewriteContext(node.getPath(), mount, canonical, navigationStateful);
            }

            if (!document.isNodeType("demosite:commentdocument")) {
                return new RewriteContext(node.getPath(), mount, canonical, navigationStateful);
            }

            final Node commentLink = JcrUtils.getNodeIfExists(document, "demosite:commentlink");

            if (commentLink == null) {
                final String msg = String.format("Found a comment document '%s' without a commentlink node. " +
                                "Cannot create a link for it.", document.getPath());
                log.info(msg);
                throw new RewriteContextException(msg);
            }

            final String docBase = JcrUtils.getStringProperty(commentLink, HippoNodeType.HIPPO_DOCBASE, null);
            try {
                UUID.fromString(docBase);
            } catch (IllegalArgumentException e){
                final String msg = String.format("Found a comment document '%s' with incorrect docbase in commentlink node. " +
                                "Cannot create a link for it.", document.getPath());
                log.info(msg);
                throw new RewriteContextException(msg);
            }

            try {
                final Node linkedHandle = node.getSession().getNodeByIdentifier(docBase);
                if (linkedHandle.isNodeType(HippoNodeType.NT_HANDLE)) {
                    String linkedPathInfo = linkedHandle.getPath();
                    log.info("For '{}' we found the referred document '{}'. Create a link for the referred document.",
                            node.getPath(), linkedPathInfo);
                    return new RewriteContext(linkedPathInfo, mount, canonical, navigationStateful);
                } else {
                    final String msg = String.format("Found a comment document '%s' with docbase '%s' that does" +
                            "not point to a document (handle).", document.getPath(), docBase);
                    log.info(msg);
                    throw new RewriteContextException(msg);
                }
            } catch (ItemNotFoundException e) {
                final String msg = String.format("Found a comment document '%s' with docbase '%s' that does" +
                        "not point to an existing node.", document.getPath(), docBase);
                log.info(msg);
                throw new RewriteContextException(msg);
            }

        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }
}
