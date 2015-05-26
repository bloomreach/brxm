/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.jquery.upload;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;

/**
 * @deprecated This class is deprecated from version 2.28.01 by {@link org.hippoecm.frontend.plugins.jquery.upload.multiple.FileUploadWidget}
 * {@inheritDoc}
 */
@Deprecated
public abstract class FileUploadWidget extends org.hippoecm.frontend.plugins.jquery.upload.multiple.FileUploadWidget {
    public FileUploadWidget(final String uploadPanel, final IPluginConfig pluginConfig, final FileUploadValidationService validator) {
        super(uploadPanel, pluginConfig, validator);
    }
}
