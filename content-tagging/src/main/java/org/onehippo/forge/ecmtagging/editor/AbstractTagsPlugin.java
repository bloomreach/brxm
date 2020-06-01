/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.ecmtagging.editor;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for tags plugin and tagsuggest plugin
 */
public abstract class AbstractTagsPlugin extends RenderPlugin<Node> {

    private final Logger log = LoggerFactory.getLogger(AbstractTagsPlugin.class);

    public AbstractTagsPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    /**
     * Get the model for the caption, a.k.a. title.
     *
     * Inspired on org.hippoecm.frontend.editor.plugins.field.AbstractFieldPlugin#getCaptionModel(),
     * unfortunately this class does not extend from AbstractFieldPlugin. Also, it's become quite different, since the
     * concept of a field does not apply to tags/tagsuggest.
     */
    protected IModel<String> getCaptionModel(final String defaultCaptionKey, final String defaultCaption) {

        final String captionKey = getPluginConfig().getString("captionKey", defaultCaptionKey);
        final String caption = getPluginConfig().getString("caption", defaultCaption);

        // implicitly from translator service (this class implements IStringResourceProvider)
        log.debug("Getting field caption from translator by captionKey '{}' and default caption '{}'", captionKey, caption);
        return new StringResourceModel(captionKey, this).setDefaultValue(caption);
    }
}
