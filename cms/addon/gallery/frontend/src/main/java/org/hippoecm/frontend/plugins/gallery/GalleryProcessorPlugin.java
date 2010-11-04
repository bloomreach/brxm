/*
 *  Copyright 2010 Hippo.
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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.processor.ScalingParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Registers a {@link ScalingGalleryProcessor} and initializes the {@link ScalingParameters} for each scaled image.
 * Configuration of the scaling parameters of each image is done in a child node of type frontend:plugconfig with
 * a name equal to the node name of the image. Each node can contain integer properties for the width and height, and
 * a boolean property for the upscaling setting. Example:</p>
 * <pre>
 *   <sv:node sv:name="hippogallery:thumbnail">
 *     <sv:property sv:name="jcr:primaryType" sv:type="Name">
 *       <sv:value>frontend:pluginconfig</sv:value>
 *     </sv:property>
 *     <sv:property sv:name="width" sv:type="Long">
 *       <sv:value>60</sv:value>
 *     </sv:property>
 *     <sv:property sv:name="height" sv:type="Long">
 *       <sv:value>60</sv:value>
 *     </sv:property>
 *     <sv:property sv:name="upscaling" sv:type="Boolean">
 *       <sv:value>false</sv:value>
 *     </sv:property>
 *   </sv:node>
 * </pre>
 * <p>
 * A width or height of 0 or less indicates "unbounded", and results in a bounding box that does not restrict scaling
 * in either the width or height, respectively. When both width and height are 0 or less, the image should not be
 * scaled at all but merely copied. The 'upscaling' property indicates whether images that are smaller than the
 * bounding box should be scaled up or not. By default, the width and height of a the bounding box are 0, and
 * upscaling is disabled.</p>
 *
 * @see {@link ScalingParameters}
 * @see {@link ScalingGalleryProcessor}
 */
public class GalleryProcessorPlugin extends Plugin {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(GalleryProcessorPlugin.class);

    protected static final String CONFIG_PARAM_WIDTH = "width";
    protected static final String CONFIG_PARAM_HEIGHT = "height";
    protected static final String CONFIG_PARAM_UPSCALING = "upscaling";
    protected static final int DEFAULT_WIDTH = 0;
    protected static final int DEFAULT_HEIGHT = 0;
    protected static final boolean DEFAULT_UPSCALING = false;

    public GalleryProcessorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final GalleryProcessor processor = createScalingGalleryProcessor(config);
        final String id = config.getString("gallery.processor.id", "gallery.processor.service");

        context.registerService(processor, id);
    }

    protected ScalingGalleryProcessor createScalingGalleryProcessor(IPluginConfig config) {
        final ScalingGalleryProcessor processor = new ScalingGalleryProcessor();

        for (IPluginConfig scaleConfig: config.getPluginConfigSet()) {
            final String nodeName = StringUtils.substringAfterLast(scaleConfig.getName(), ".");

            if (!StringUtils.isEmpty(nodeName)) {
                final int width = scaleConfig.getAsInteger(CONFIG_PARAM_WIDTH, DEFAULT_WIDTH);
                final int height = scaleConfig.getAsInteger(CONFIG_PARAM_HEIGHT, DEFAULT_HEIGHT);
                final boolean upscaling = scaleConfig.getAsBoolean(CONFIG_PARAM_UPSCALING, DEFAULT_UPSCALING);

                final ScalingParameters parameters = new ScalingParameters(width, height, upscaling);
                log.debug("Scaling parameters for {}: {}", nodeName, parameters);
                processor.addScalingParameters(nodeName, parameters);
            }
        }

        return processor;
    }

}
