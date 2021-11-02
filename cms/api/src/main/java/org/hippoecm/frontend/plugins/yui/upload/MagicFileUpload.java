/*
 *  Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.upload;

import org.apache.commons.fileupload.FileItem;
import org.apache.wicket.markup.html.form.upload.FileUpload;

public class MagicFileUpload extends FileUpload {

    private FileItem item;

    /**
     * @param item The uploaded file item
     */
    public MagicFileUpload(final FileItem item) {
        super(item);
        this.item = item;
    }

    public String getBrowserProvidedContentType() {
        if (item instanceof MagicMimeTypeFileItem) {
            return ((MagicMimeTypeFileItem)item).getBrowserProvidedContentType();
        }
        return item.getContentType();
    }
}
