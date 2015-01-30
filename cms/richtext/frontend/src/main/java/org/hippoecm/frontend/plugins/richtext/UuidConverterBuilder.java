/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext;

import java.util.HashSet;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.plugins.richtext.jcr.RichTextFacetHelper;
import org.hippoecm.repository.api.NodeNameCodec;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UuidConverterBuilder implements IDetachable {

    public static final Logger log = LoggerFactory.getLogger(UuidConverterBuilder.class);

    private static final String TAG_A = "a";
    private static final String TAG_IMG = "img";

    private static final String ATTRIBUTE_HREF = "href";
    private static final String ATTRIBUTE_SRC = "src";
    private static final String ATTRIBUTE_DATA_UUID = "data-uuid";
    private static final String ATTRIBUTE_DATA_TYPE = "data-type";

    private static final String IMAGE_DOCUMENT = "{_document}";
    private static final String IMAGE_SEPARATOR = "/";

    private final IModel<Node> nodeModel;
    private final IRichTextLinkFactory linkFactory;
    private final IImageURLProvider imageLinkProvider;

    public UuidConverterBuilder(IModel<Node> nodeModel, IRichTextLinkFactory linkFactory, IImageURLProvider imageLinkProvider) {
        this.nodeModel = nodeModel;
        this.linkFactory = linkFactory;
        this.imageLinkProvider = imageLinkProvider;
    }

    public TagNodeVisitor createRetrievalConverter() {
        return (parentNode, htmlNode) -> {
            try {
                if (htmlNode instanceof TagNode) {
                    final TagNode tag = (TagNode) htmlNode;
                    if (StringUtils.equalsIgnoreCase(TAG_A, tag.getName())) {
                        convertLinkForRetrieval(tag);
                    } else if (StringUtils.equalsIgnoreCase(TAG_IMG, tag.getName())) {
                        convertImageForRetrieval(tag);
                    }
                }
            } catch (RepositoryException | RichTextException e) {
                log.info(e.getMessage(), e);
            }
            return true;
        };
    }

    private void convertLinkForRetrieval(TagNode tag) throws RepositoryException, RichTextException {
        final Map<String, String> attributes = tag.getAttributes();
        final String href = attributes.get(ATTRIBUTE_HREF);

        if (StringUtils.isEmpty(href)) {
            return;
        }
        if (RichTextProcessor.isExternalLink(href)) {
            return;
        }

        final Node node = nodeModel.getObject();
        final String name = NodeNameCodec.encode(href, true);
        final String uuid = RichTextFacetHelper.getChildDocBaseOrNull(node, name);

        if (uuid != null) {
            attributes.put(ATTRIBUTE_HREF, RichTextProcessor.INTERNAL_LINK_DEFAULT_HREF);
            attributes.put(ATTRIBUTE_DATA_UUID, uuid);
        }
    }

    private void convertImageForRetrieval(TagNode tag) throws RepositoryException {
        final Map<String, String> attributes = tag.getAttributes();
        final String src = attributes.get(ATTRIBUTE_SRC);

        if (StringUtils.isEmpty(src)) {
            return;
        }
        if (RichTextProcessor.isExternalLink(src)) {
            return;
        }

        String name = null;
        String type = null;

        final String[] parts = src.split(IMAGE_SEPARATOR);
        if (parts.length >= 1) {
            name = parts[0];
        }
        if (parts.length >= 3) {
            type = parts[2];
        }

        final Node node = nodeModel.getObject();
        final String uuid = RichTextFacetHelper.getChildDocBaseOrNull(node, name);

        String url;
        try {
            url = imageLinkProvider.getURL(src);
        } catch (RichTextException ex) {
            url = RequestCycle.get().urlFor(RichTextProcessor.BROKEN_IMAGE, null).toString();
        }

        if (uuid != null) {
            attributes.put(ATTRIBUTE_SRC, url);
            attributes.put(ATTRIBUTE_DATA_UUID, uuid);
            if (type != null) {
                attributes.put(ATTRIBUTE_DATA_TYPE, type);
            }
        }
    }

    public TagNodeVisitor createStorageConverter() {
        return (parentNode, htmlNode) -> {
            if (parentNode == null) {
                linkFactory.cleanup(new HashSet<>());
            }
            try {
                if (htmlNode instanceof TagNode) {
                    final TagNode tagNode = (TagNode) htmlNode;
                    if (StringUtils.equalsIgnoreCase(TAG_A, tagNode.getName())) {
                        convertLinkForStorage(tagNode);
                    } else if (StringUtils.equalsIgnoreCase(TAG_IMG, tagNode.getName())) {
                        convertImageForStorage(tagNode);
                    }
                }
            } catch (RepositoryException | RichTextException e) {
                log.info(e.getMessage(), e);
            }
            return true;
        };
    }

    private void convertLinkForStorage(TagNode tag) throws RepositoryException, RichTextException {
        final Map<String, String> attributes = tag.getAttributes();
        final String href = attributes.get(ATTRIBUTE_HREF);

        if (StringUtils.isEmpty(href)) {
            return;
        }

        final String uuid = attributes.remove(ATTRIBUTE_DATA_UUID);

        if (RichTextProcessor.isExternalLink(href)) {
            return;
        }

        if (uuid == null) {
            return;
        }

        final String name = findOrCreateFacetNode(uuid);
        if (name != null) {
            attributes.put(ATTRIBUTE_HREF, name);
        } else {
            attributes.remove(ATTRIBUTE_HREF);
        }
    }

    private void convertImageForStorage(TagNode tag) throws RepositoryException, RichTextException {
        final Map<String, String> attributes = tag.getAttributes();
        String src = attributes.get(ATTRIBUTE_SRC);

        if (StringUtils.isEmpty(src)) {
            return;
        }

        final String uuid = attributes.remove(ATTRIBUTE_DATA_UUID);
        final String type = attributes.remove(ATTRIBUTE_DATA_TYPE);

        if (uuid == null) {
            return;
        }

        final String name = findOrCreateFacetNode(uuid);
        if (name == null) {
            return;
        }
        src = name;
        if (type != null) {
            src += IMAGE_SEPARATOR + IMAGE_DOCUMENT + IMAGE_SEPARATOR + type;
        }
        attributes.put(ATTRIBUTE_SRC, src);
    }

    private String findOrCreateFacetNode(String uuid) throws RepositoryException {
        final Node node = nodeModel.getObject();
        String name = RichTextFacetHelper.getChildFacetNameOrNull(node, uuid);
        if (name == null) {
            name = RichTextFacetHelper.createFacet(node, uuid);
        }
        return name;
    }

    public void detach() {
        nodeModel.detach();
        linkFactory.detach();
    }

}
