/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.service.restproxy.test.channels;

import org.hippoecm.frontend.service.restproxy.test.annotations.FieldGroup;
import org.hippoecm.frontend.service.restproxy.test.annotations.FieldGroupList;
import org.hippoecm.frontend.service.restproxy.test.annotations.JcrPath;
import org.hippoecm.frontend.service.restproxy.test.annotations.Parameter;

/**
 * Retrieves the properties of the GoGreen channels.
 */
@FieldGroupList({
        @FieldGroup(
                titleKey = "fields.channel",
                value = { "logo", "pageTitlePrefix", "themeCss" }
        )
})
public interface WebsiteInfo {

    @Parameter(name = "logo")
    @JcrPath(
            pickerSelectableNodeTypes = { "hippogogreengallery:imageset" },
            pickerInitialPath = "/content/gallery/logos"
    )
    String getLogoPath();

    @Parameter(name = "pageTitlePrefix", defaultValue = "Hippo Go Green")
    String getPageTitlePrefix();

    @Parameter(name = "themeCss", defaultValue = "/content/assets/themes/css/green.css")
    @JcrPath(
            pickerConfiguration = "cms-pickers/assets",
            pickerSelectableNodeTypes = {"hippogallery:exampleAssetSet"},
            pickerInitialPath = "/content/assets/themes/css"
    )
    String getThemeCss();

}
