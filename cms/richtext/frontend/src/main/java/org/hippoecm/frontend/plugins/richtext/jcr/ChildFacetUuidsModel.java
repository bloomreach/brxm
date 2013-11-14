/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.jcr;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextProcessor;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model that replaces links and images in the String of the delegate model that refer to child facetselect nodes in
 * the node model to the UUIDs referred to by these facetselects.
 *
 * For links (HTML tag 'a'), the 'href' attribute can contain the name of a child node of type 'hippo:facetselect'.
 * When this model's object will remove the 'href' attribute of those links and add a 'data-uuid' attribute with the UUID
 * referred to by the facetselect (i.e. its hippo:docbase property). Setting links with a 'data-uuid' attribute in this
 * model's object will work the other way around: the 'data-uuid' attribute will be removed, and replaced by an 'href'
 * attribute that refers to a facet child node.
 *
 * For images (HTML tag 'img'), the 'src' attribute can also contain the name of a facetselect child node, together
 * with more information about the image after the first slash. Similar to links, this model will add 'data-uuid' attribute
 * with the docbase of the facetselect. However, the 'src' attribute will be left as-is.
 *
 * All facetselect child nodes are managed by this model: new ones will be created when referred to, and unused ones
 * will be removed.
 */
public class ChildFacetUuidsModel implements IModel<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ChildFacetUuidsModel.class);

    private final IModel<String> delegate;
    private final IModel<Node> nodeModel;
    private final IRichTextLinkFactory linkFactory;

    public ChildFacetUuidsModel(IModel<String> delegate, IModel<Node> nodeModel, IRichTextLinkFactory linkFactory) {
        this.delegate = delegate;
        this.nodeModel = nodeModel;
        this.linkFactory = linkFactory;
    }

    public String getObject() {
        String text = delegate.getObject();
        if (StringUtils.isNotEmpty(text)) {
            text = replaceChildNodeNamesWithUuids(text);
        }
        return text;
    }

    private String replaceChildNodeNamesWithUuids(String text) {
        final Node node = nodeModel.getObject();

        final InternalLinkHrefToUuidDecorator linkDecorator = new InternalLinkHrefToUuidDecorator(node);
        text = RichTextProcessor.decorateLinkHrefs(text, linkDecorator);

        final ImgAddUuidDecorator imageDecorator = new ImgAddUuidDecorator(node);
        return RichTextProcessor.decorateImgSrcs(text, imageDecorator);
    }

    public void setObject(String text) {
        createMissingChildFacetNodes(text);
        removeUnusedChildFacetNodes(text);
        text = replaceUuids(text);
        delegate.setObject(text);
    }

    private void removeUnusedChildFacetNodes(final String text) {
        Set<String> linkUuids = RichTextProcessor.getInternalLinkUuids(text);
        linkFactory.cleanup(linkUuids);
    }

    private void createMissingChildFacetNodes(final String text) {
        final Set<String> uuids = RichTextProcessor.getInternalLinkUuids(text);
        final Node node = nodeModel.getObject();
        try {
            removeUuidsForExistingFacetChildNodes(node, uuids);
            RichTextFacetHelper.createFacets(node, uuids);
        } catch (RepositoryException e) {
            log.warn("Cannot create child facet nodes for links in the text of '{}'", JcrUtils.getNodePathQuietly(node), e);
        }
    }

    private static void removeUuidsForExistingFacetChildNodes(final Node node, final Set<String> uuids) throws RepositoryException {
        final NodeIterator childIterator = node.getNodes();
        while (childIterator.hasNext()) {
            final Node child = childIterator.nextNode();
            final String childDocBase = JcrUtils.getStringProperty(child, HippoNodeType.HIPPO_DOCBASE, null);
            uuids.remove(childDocBase);
        }
    }

    private String replaceUuids(String text) {
        final Node node = this.nodeModel.getObject();

        final InternalLinkRemoveHrefDecorator removeInternalLinkHrefs = new InternalLinkRemoveHrefDecorator();
        text = RichTextProcessor.decorateInternalLinkHrefs(text, removeInternalLinkHrefs);

        final InternalLinkUuidToHrefDecorator changeInternalLinkUuidsToHrefs = new InternalLinkUuidToHrefDecorator(node);
        text = RichTextProcessor.decorateInternalLinkUuids(text, changeInternalLinkUuidsToHrefs);

        final ImgRemoveUuidDecorator removeImageUuids = new ImgRemoveUuidDecorator();
        return RichTextProcessor.decorateInternalImgUuids(text, removeImageUuids);
    }

    public void detach() {
        delegate.detach();
        nodeModel.detach();
        linkFactory.detach();
    }

}
