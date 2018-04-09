/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.dialog.images;

import java.io.Serializable;

/**
 * Configuration of the image picker.  Contains the preferred resource definitions;
 * these will be used when available on the selected image.
 * FIXME: now used by the image item factory; should be used by the dialog itself
 */
public class ImagePickerSettings implements Serializable {

    private String[] preferredResourceNames = new String[0];

    public void setPreferredResourceNames(String[] preferredResourceNames) {
        this.preferredResourceNames = preferredResourceNames;
    }

    public String[] getPreferredResourceNames() {
        return preferredResourceNames;
    }
}
