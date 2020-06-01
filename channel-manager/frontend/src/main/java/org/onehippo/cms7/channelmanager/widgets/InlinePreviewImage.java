/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.widgets;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a preview version of an imageset. The model contains the UUID of the handle of the image set to render.
 * The name of the image set variant to show can be set at runtime. If the named variant cannot be found, the
 * primary item of the image set is used instead. The width and height of the preview image are read from its meta-data.
 * If no meta-data is available (e.g. with older imageset types) no width and height are set.
 */
class InlinePreviewImage extends WebComponent {

    private static final long serialVersionUID = 1L;
    private static final String BASE_PATH_BINARIES = "binaries";

    private final Logger log = LoggerFactory.getLogger(InlinePreviewImage.class);

    private String variantName;

    InlinePreviewImage(String id, IModel<String> uuidModel, String variantName) {
        super(id, uuidModel);
        this.variantName = variantName;

        setOutputMarkupId(true);
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        checkComponentTag(tag, "img");

        super.onComponentTag(tag);

        Node previewImage = getPreviewImage();

        String src = getUrl(previewImage);

        if (src != null) {
            tag.put("src", src);

            String width = getDimension(previewImage, HippoGalleryNodeType.IMAGE_WIDTH);
            if (width != null) {
                tag.put("width", width);
            }

            String height = getDimension(previewImage, HippoGalleryNodeType.IMAGE_HEIGHT);
            if (height != null) {
                tag.put("height", height);
            }

            setVisible(true);
        } else {
            setVisible(false);
        }

    }

    public boolean isValid() {
        return getPreviewImage() != null;
    }

    private Node getPreviewImage() {
        final String uuid = getDefaultModelObjectAsString();

        if (StringUtils.isNotEmpty(uuid)) {
            final javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            try {
                Node handle = session.getNodeByIdentifier(uuid);
                if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                    Node imageSet = handle.getNode(handle.getName());
                    Node variant = getVariant(imageSet);
                    if (variant != null) {
                        if (variant.isNodeType(HippoNodeType.NT_RESOURCE)) {
                            return variant;
                        } else {
                            log.error("Preview image variant '{}' is not of type '{}'",
                                    variant.getPath(), HippoNodeType.NT_RESOURCE);
                        }
                    }
                } else {
                    log.error("Cannot retrieve preview image from '{}': node is not a handle", handle.getPath());
                }
            } catch (RepositoryException e) {
                log.warn("Cannot retrieve preview image with UUID '" + uuid + "'", e);
            }
        }

        return null;
    }

    private Node getVariant(Node imageSet) throws RepositoryException {
        if (StringUtils.isNotEmpty(variantName)) {
            if (imageSet.hasNode(variantName)) {
                return imageSet.getNode(variantName);
            }
            log.debug("Image set '{}' does not contain variant '{}', using primary item instead", imageSet.getPath(), variantName);
        }

        Item primary = JcrHelper.getPrimaryItem(imageSet);

        if (primary.isNode()) {
            return (Node) primary;
        }

        if (StringUtils.isNotEmpty(variantName)) {
            log.error("Cannot retrieve preview image '{}': variant '{}' does not exist and primary item of image set is not a node",
                    imageSet.getPath(), variantName);
        } else {
            log.error("Cannot retrieve preview image '{}': primary item of image set is not a node", imageSet.getPath());
        }

        return null;
    }

    private String getUrl(Node image) {
        if (image != null) {
            try {
                return BASE_PATH_BINARIES + image.getPath();
            } catch (RepositoryException e) {
                log.warn("Cannot retrieve path of preview image", e);
            }
        }
        return null;
    }

    private String getDimension(Node image, String propertyName) {
        if (image != null) {
            try {
                return image.getProperty(propertyName).getString();
            } catch (PathNotFoundException ignored) {
                log.debug("Ignoring missing image property {}: {}", propertyName, ignored.getMessage());
            } catch (RepositoryException ignored) {
                log.debug("Ignoring error while reading image property {}: {}", propertyName, ignored.getMessage());
            }
        }
        return null;
    }

}
