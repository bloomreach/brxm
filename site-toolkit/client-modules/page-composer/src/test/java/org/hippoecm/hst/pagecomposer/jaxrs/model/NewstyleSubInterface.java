/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.Parameter;

interface NewstyleSubInterface extends NewstyleInterface {

    // overriding should work in combination with i18n
    @Override
    @Parameter(name = "01-image", defaultValue = "/content/gallery/default.png")
    @ImageSetPath
    String getImage();

    @Parameter(name = "15-subboolean")
    boolean isSubBoolean();
}
