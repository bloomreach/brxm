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

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugins.yui.upload.model.UploadedFile;

/**
 * This interface represents a service that will be processed when a file is being uploaded to the CMS.
 */
public interface FileUploadPreProcessorService extends IClusterable {

    String PRE_PROCESSOR_ID = "pre.processor.id";
    String DEFAULT_ID = "service.upload.pre.processor";

    void process(final UploadedFile uploadedFile);
}
