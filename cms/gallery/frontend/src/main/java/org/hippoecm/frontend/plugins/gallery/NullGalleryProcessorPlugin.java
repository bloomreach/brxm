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
package org.hippoecm.frontend.plugins.gallery;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.NullGalleryProcessor;

/**
 * Registers an {@link org.hippoecm.frontend.plugins.gallery.model.NullGalleryProcessor} service. The configuration option 'gallery.processor.id' specifies
 * the CMS service id of this gallery processor. If no service id is specified, the service id
 * 'identity.gallery.processor' is used.
 */
public class NullGalleryProcessorPlugin extends Plugin {

    private static final long serialVersionUID = 1L;

    public NullGalleryProcessorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        NullGalleryProcessor processor = new NullGalleryProcessor();
        context.registerService(processor, config.getString("gallery.processor.id", "identity.gallery.processor"));
    }

}
