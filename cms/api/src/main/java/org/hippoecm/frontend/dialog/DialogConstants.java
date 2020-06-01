/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;

public interface DialogConstants {

    ResourceReference AJAX_LOADER_GIF = new PackageResourceReference(DialogConstants.class, "ajax-loader.gif");

    String BUTTON = "button";

    IValueMap SMALL = new ValueMap("width=380,height=250").makeImmutable();
    IValueMap SMALL_AUTO = new ValueMap("width=380,height=auto").makeImmutable();
    IValueMap MEDIUM = new ValueMap("width=475,height=375").makeImmutable();
    IValueMap MEDIUM_AUTO = new ValueMap("width=475,height=auto").makeImmutable();
    IValueMap LARGE = new ValueMap("width=855,height=450").makeImmutable();
    IValueMap LARGE_AUTO = new ValueMap("width=855,height=auto").makeImmutable();
}
