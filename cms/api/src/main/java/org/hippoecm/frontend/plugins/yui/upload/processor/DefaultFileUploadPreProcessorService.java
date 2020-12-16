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

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.model.IUploadPreProcessor;
import org.hippoecm.frontend.plugins.yui.upload.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link FileUploadPreProcessorService}. This implementation contains a list of {@link
 * IUploadPreProcessor} and executes them all when the method process is invoked.
 */
public class DefaultFileUploadPreProcessorService implements FileUploadPreProcessorService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFileUploadPreProcessorService.class);

    private List<IUploadPreProcessor> preProcessors = new LinkedList<>();

    public static FileUploadPreProcessorService getPreProcessorService(final IPluginContext pluginContext,
                                                                       final IPluginConfig pluginConfig) {
        return getPreProcessorService(pluginContext, pluginConfig, DEFAULT_ID);
    }

    public static FileUploadPreProcessorService getPreProcessorService(final IPluginContext pluginContext,
                                                                       final IPluginConfig pluginConfig,
                                                                       final String id) {
        String serviceId = pluginConfig.getString(FileUploadPreProcessorService.PRE_PROCESSOR_ID, id);
        FileUploadPreProcessorService preProcessorService = pluginContext.getService(serviceId,
                FileUploadPreProcessorService.class);

        if (preProcessorService == null) {
            preProcessorService = new DefaultFileUploadPreProcessorService();
            log.info("Cannot load pre processor service with id '{}', using the default service '{}'",
                    serviceId, preProcessorService.getClass().getName());
        }
        return preProcessorService;
    }

    protected final void addPreProcessor(IUploadPreProcessor preProcessor) {
        preProcessors.add(preProcessor);
    }

    @Override
    public void process(final UploadedFile uploadedFile) {
        preProcessors.forEach(preProcessor -> {
            try {
                preProcessor.process(uploadedFile);
            } catch (Exception exception) {
                log.error(String.format("There was an error when executing the file upload pre processor %s",
                        preProcessor.getClass().getName()), exception);
            }
        });
    }

}
