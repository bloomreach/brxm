/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.yui.upload.processor;

import java.util.Objects;
import java.util.Optional;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.model.IUploadPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService.DEFAULT_ID;

public class DefaultFileUploadPreProcessorPlugin extends Plugin {

    private static final String PRE_PROCESSORS_NODE_KEY = "preProcessors";
    private static final String CLASS_NAME_KEY = "className";
    private static final Logger log = LoggerFactory.getLogger(DefaultFileUploadPreProcessorPlugin.class);

    /**
     * Construct a new Plugin.
     *
     * @param context the plugin context
     * @param config  the plugin config
     */
    public DefaultFileUploadPreProcessorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final String id = config.getString(FileUploadPreProcessorService.PRE_PROCESSOR_ID, DEFAULT_ID);
        context.registerService(getPreProcessorService(), id);
    }

    public FileUploadPreProcessorService getPreProcessorService() {
        DefaultFileUploadPreProcessorService defaultFileUploadPreProcessorService = new DefaultFileUploadPreProcessorService();
        Optional.ofNullable(getPluginConfig())
                .map(pluginConfig -> pluginConfig.getPluginConfig(PRE_PROCESSORS_NODE_KEY))
                .map(IPluginConfig::getPluginConfigSet)
                .ifPresent(preProcessors ->
                        preProcessors.stream()
                                .map(preProcessor ->
                                        Optional.ofNullable(preProcessor.getString(CLASS_NAME_KEY, null))
                                                .map(className -> {
                                                    try {
                                                        return this.getClass().getClassLoader().loadClass(className);
                                                    } catch (ClassNotFoundException cnfe) {
                                                        log.error(String.format("The class %s defined as a file " +
                                                                "upload pre processor wasn't found on the classpath",
                                                                className), cnfe);
                                                    }
                                                    return null;
                                                })
                                                .filter(IUploadPreProcessor.class::isAssignableFrom)
                                                .map(preProcessorClass -> {
                                                    try {
                                                        return (IUploadPreProcessor) preProcessorClass.newInstance();
                                                    } catch (IllegalAccessException | InstantiationException |
                                                            ClassCastException e) {
                                                        log.error("Error when instantiating the class " +
                                                                preProcessorClass.getName(), e);
                                                    }
                                                    return null;
                                                })
                                                .orElse(null)
                                )
                                .filter(Objects::nonNull)
                                .forEach(defaultFileUploadPreProcessorService::addPreProcessor)
                );
        return defaultFileUploadPreProcessorService;
    }

}
