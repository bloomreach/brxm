/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.image.JcrImage;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.dragdrop.DragSettings;
import org.hippoecm.frontend.plugins.yui.dragdrop.ImageNodeDragBehavior;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageContainer extends Panel {

    private static final Logger log = LoggerFactory.getLogger(ImageContainer.class);

    private final JcrResourceStream stream;

    public ImageContainer(final String wicketId, final JcrNodeModel model, final IPluginContext pluginContext,
                          final IPluginConfig pluginConfig) {

        this(wicketId, model, pluginContext, pluginConfig, -1);
    }

    public ImageContainer(final String wicketId, final JcrNodeModel model, final IPluginContext pluginContext,
                          final IPluginConfig pluginConfig, final int maxAxisLength) {

        super(wicketId, model);

        stream = new JcrResourceStream(model);
        int width = getDimension(model, HippoGalleryNodeType.IMAGE_WIDTH, pluginConfig);
        int height = getDimension(model, HippoGalleryNodeType.IMAGE_HEIGHT, pluginConfig);

        if (maxAxisLength > -1) {
            if (width > height && width > maxAxisLength) {
                final double ratio = maxAxisLength / width;
                height = (int) Math.round(height * ratio);
                width = maxAxisLength;
            } else if (height > width && height > maxAxisLength) {
                final double ratio = maxAxisLength / height;
                width = (int) Math.round(width * ratio);
                height = maxAxisLength;
            } else if(width > maxAxisLength) {
                width = maxAxisLength;
                height = maxAxisLength;
            }
        }

        final Image img = new JcrImage("image", stream, width, height);
        img.add(new ImageNodeDragBehavior(new DragSettings(YuiPluginHelper.getConfig(pluginConfig)), model));
        add(img);
    }

    private int getDimension(final JcrNodeModel model, final String propertyName, final IPluginConfig config) {
        try {
            return (int)model.getNode().getProperty(propertyName).getLong();
        } catch (final ValueFormatException ignored) {
            log.debug("Ignoring illegal long value of image property {}: {}", propertyName, ignored.getMessage());
        } catch (final PathNotFoundException ignored) {
            log.debug("Ignoring missing image property {}: {}", propertyName, ignored.getMessage());
        } catch (final RepositoryException ignored) {
            log.debug("Ignoring error while reading image property {}: {}", propertyName, ignored.getMessage());
        }
        return config.getAsInteger("gallery.thumbnail.size", 0);
    }

    @Override
    public void onDetach() {
        stream.detach();
        super.onDetach();
    }
}
