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

package org.hippoecm.frontend.plugins.yui.upload.model;

import org.apache.wicket.util.io.IClusterable;

/**
 * Custom Upload Processor interface. It's used by the CMS editor to perform custom user-defined transformations when a
 * file is uploaded to the system.
 */
public interface IUploadPreProcessor extends IClusterable {
    /**
     * If this interface is implemented, the logic inside this method will be executed before persisting the uploaded
     * file. Any modification to the provided {@link UploadedFile} object will be persisted
     *
     * @param uploadedFile
     */
    void process(final UploadedFile uploadedFile);
}
