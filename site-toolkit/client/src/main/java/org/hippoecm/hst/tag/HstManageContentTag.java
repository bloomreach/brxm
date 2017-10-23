/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.tag;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.channelmanager.ChannelManagerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.utils.TagUtils.encloseInHTMLComment;
import static org.hippoecm.hst.utils.TagUtils.toJSONMap;

/**
 * This tag creates a manage content button in the channel manager.
 */
public class HstManageContentTag extends TagSupport {

    private static final Logger log = LoggerFactory.getLogger(HstManageContentTag.class);

    private String componentParameter;
    private String defaultPath;
    private HippoBean document;
    private String rootPath;
    private String templateQuery;

    @Override
    public int doStartTag() throws JspException {
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            final HstRequestContext requestContext = RequestContextProvider.get();

            if (requestContext == null) {
                log.warn("Cannot create a manage content button outside the hst request.");
                return EVAL_PAGE;
            }

            if (!requestContext.isCmsRequest()) {
                log.debug("Skipping manage content tag because not in cms preview.");
                return EVAL_PAGE;
            }

            String documentId = null;
            if (document != null) {
                final HippoNode documentNode = (HippoNode) document.getNode();
                try {
                    final Node editNode = documentNode.getCanonicalNode();
                    if (editNode == null) {
                        log.debug("Cannot create a manage-content link, cannot find canonical node of '{}'",
                                documentNode.getPath());
                        return EVAL_PAGE;
                    }

                    final Node handleNode = getHandleNodeIfIsAncestor(editNode);
                    if (handleNode == null) {
                        log.warn("Could not find handle node of {}", editNode.getPath());
                        return EVAL_PAGE;
                    }

                    log.debug("The node path for the manage content tag is '{}'", handleNode.getPath());
                    documentId = handleNode.getIdentifier();
                } catch (RepositoryException e) {
                    log.warn("Error while retrieving the document handle of '{}', skipping manage content tag",
                            JcrUtils.getNodePathQuietly(document.getNode()), e);
                    return EVAL_PAGE;
                }
            }

            try {
                write(documentId);
            } catch (final IOException ignore) {
                throw new JspException("Manage content tag exception: cannot write to the output writer.");
            }
            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        templateQuery = null;
        rootPath = null;
        defaultPath = null;
        componentParameter = null;
        document = null;
    }

    private void write(final String documentId) throws IOException {
        final JspWriter writer = pageContext.getOut();
        final Map<?, ?> attributeMap = getAttributeMap(documentId);
        final String comment = encloseInHTMLComment(toJSONMap(attributeMap));
        writer.print(comment);
    }

    private Map<?, ?> getAttributeMap(final String documentId) {
        final Map<String, Object> result = new LinkedHashMap<>();
        writeToMap(result, ChannelManagerConstants.HST_TYPE, "MANAGE_CONTENT_LINK");
        writeToMap(result, "uuid", documentId);
        writeToMap(result, "templateQuery", templateQuery);
        writeToMap(result, "rootPath", rootPath);
        writeToMap(result, "defaultPath", defaultPath);
        writeToMap(result, "componentParameter", componentParameter);
        return result;
    }

    private static void writeToMap(final Map<String, Object> result, final String key, final String value) {
        if (StringUtils.isNotEmpty(value)) {
            result.put(key, value);
        }
    }

    /*
     * when a currentNode is of type hippo:handle, we return this node, else we check the parent, until we are at the jcr root node.
     * When we hit the jcr root node, we return null;
     */
    private Node getHandleNodeIfIsAncestor(final Node currentNode) throws RepositoryException {
        final Node rootNode = currentNode.getSession().getRootNode();
        return getHandleNodeIfIsAncestor(currentNode, rootNode);
    }

    private Node getHandleNodeIfIsAncestor(final Node currentNode, final Node rootNode) throws RepositoryException {
        if (currentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            return currentNode;
        }
        if (currentNode.isSame(rootNode)) {
            return null;
        }
        return getHandleNodeIfIsAncestor(currentNode.getParent(), rootNode);
    }

    public void setComponentParameter(final String componentParameter) {
        this.componentParameter = componentParameter;
    }

    public void setDefaultPath(final String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public void setDocument(final HippoBean document) {
        this.document = document;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    public void setTemplateQuery(final String templateQuery) {
        this.templateQuery = templateQuery;
    }

}






