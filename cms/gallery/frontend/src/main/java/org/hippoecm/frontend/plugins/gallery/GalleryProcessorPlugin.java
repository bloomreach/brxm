/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;

public class GalleryProcessorPlugin extends Plugin {

    public GalleryProcessorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        DefaultGalleryProcessor processor = new DefaultGalleryProcessor();
        if (config.containsKey("gallery.thumbnail.size")) {
            processor.setThumbnailSize(config.getInt("gallery.thumbnail.size"));
        }
        context.registerService(processor, config.getString(GalleryProcessor.GALLERY_PROCESSOR_ID,
                GalleryProcessor.DEFAULT_GALLERY_PROCESSOR_ID));
    }

}
