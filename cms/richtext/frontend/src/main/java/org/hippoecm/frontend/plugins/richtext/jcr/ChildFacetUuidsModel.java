/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextProcessor;
import org.hippoecm.repository.api.NodeNameCodec;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model that replaces links and images in the String of the delegate model that refer to child facetselect nodes in the
 * node model to the UUIDs referred to by these facetselects.
 * <p/>
 * For links (HTML tag 'a'), the 'href' attribute can contain the name of a child node of type 'hippo:facetselect'. When
 * this model's object will remove the 'href' attribute of those links and add a 'data-uuid' attribute with the UUID
 * referred to by the facetselect (i.e. its hippo:docbase property). Setting links with a 'data-uuid' attribute in this
 * model's object will work the other way around: the 'data-uuid' attribute will be removed, and replaced by an 'href'
 * attribute that refers to a facet child node.
 * <p/>
 * For images (HTML tag 'img'), the 'src' attribute can also contain the name of a facetselect child node, together with
 * more information about the image after the first slash. Similar to links, this model will add 'data-uuid' and
 * 'data-type' attributes.
 * <p/>
 * All facetselect child nodes are managed by this model: new ones will be created when referred to, and unused ones
 * will be removed.
 */
public class ChildFacetUuidsModel implements IModel<String> {

    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(ChildFacetUuidsModel.class);

    private static final String TAG_A = "a";
    private static final String TAG_IMG = "img";

    private static final String ATTRIBUTE_HREF = "href";
    private static final String ATTRIBUTE_SRC = "src";
    private static final String ATTRIBUTE_DATA_UUID = "data-uuid";
    private static final String ATTRIBUTE_DATA_TYPE = "data-type";
    private static final String ATTRIBUTE_DATA_FACETSELECT = "data-facetselect";

    private static final String IMAGE_DOCUMENT = "{_document}";
    private static final String IMAGE_SEPARATOR = "/";

    private final IModel<String> delegate;
    private final IModel<Node> nodeModel;
    private final IRichTextLinkFactory linkFactory;
    private final IImageURLProvider imageLinkProvider;
    private final HtmlCleaner cleaner;

    public ChildFacetUuidsModel(IModel<String> delegate, IModel<Node> nodeModel, IRichTextLinkFactory linkFactory, IImageURLProvider imageLinkProvider) {
        this.delegate = delegate;
        this.nodeModel = nodeModel;
        this.linkFactory = linkFactory;
        this.imageLinkProvider = imageLinkProvider;

        cleaner = new HtmlCleaner();
        final CleanerProperties properties = cleaner.getProperties();
        properties.setOmitXmlDeclaration(true);
        properties.setOmitHtmlEnvelope(true);
        properties.setUseEmptyElementTags(true);
    }

    public String getObject() {
        return convertForView(delegate.getObject());
    }

    private String convertForView(String text) {
        if (text == null) {
            return null;
        }

        final TagNode html = cleaner.clean(text);
        html.traverse(new TagNodeVisitor() {
            @Override
            public boolean visit(TagNode parentNode, HtmlNode htmlNode) {
                try {
                    if (htmlNode instanceof TagNode) {
                        final TagNode tag = (TagNode) htmlNode;
                        if (StringUtils.equalsIgnoreCase(TAG_A, tag.getName())) {
                            convertLinkForView(tag);
                        } else if (StringUtils.equalsIgnoreCase(TAG_IMG, tag.getName())) {
                            convertImageForView(tag);
                        }
                    }
                } catch (RepositoryException | RichTextException e) {
                    log.info(e.getMessage());
                }
                return true;
            }
        });
        return new SimpleHtmlSerializer(cleaner.getProperties()).getAsString(html);
    }

    private void convertLinkForView(TagNode tag) throws RepositoryException, RichTextException {
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

    private void convertImageForView(TagNode tag) throws RepositoryException {
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

    public void setObject(String text) {
        delegate.setObject(convertForStorage(text));
    }

    private String convertForStorage(String text) {
        final Set<String> uuids = new HashSet<>();
        if (text == null) {
            linkFactory.cleanup(uuids);
            return null;
        }

        final TagNode html = cleaner.clean(text);

        html.traverse(new TagNodeVisitor() {
            @Override
            public boolean visit(TagNode parentNode, HtmlNode htmlNode) {
                if (htmlNode instanceof TagNode) {
                    final TagNode tagNode = (TagNode) htmlNode;
                    final String uuid = tagNode.getAttributes().get(ATTRIBUTE_DATA_UUID);
                    if (StringUtils.isNotEmpty(uuid)) {
                        uuids.add(uuid);
                    }
                }
                return true;
            }
        });
        linkFactory.cleanup(uuids);

        html.traverse(new TagNodeVisitor() {
            @Override
            public boolean visit(TagNode parentNode, HtmlNode htmlNode) {
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
                    log.info(e.getMessage());
                }
                return true;
            }
        });
        return new SimpleHtmlSerializer(cleaner.getProperties()).getAsString(html);
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

        final Node node = nodeModel.getObject();
        String name = RichTextFacetHelper.getChildFacetNameOrNull(node, uuid);
        if (name == null) {
            name = RichTextFacetHelper.createFacet(node, uuid);
        }

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
        attributes.remove(ATTRIBUTE_DATA_FACETSELECT); // link picker passes wrong value so we drop it

        if (uuid == null) {
            return;
        }

        Node node = nodeModel.getObject();
        String name = RichTextFacetHelper.getChildFacetNameOrNull(node, uuid);
        if (name == null) {
            name = RichTextFacetHelper.createFacet(node, uuid);
        }
        if (name == null) {
            return;
        }
        src = name;
        if (type != null) {
            src += IMAGE_SEPARATOR + IMAGE_DOCUMENT + IMAGE_SEPARATOR + type;
        }
        attributes.put(ATTRIBUTE_SRC, src);
    }

    public void detach() {
        delegate.detach();
        nodeModel.detach();
        linkFactory.detach();
    }

}
