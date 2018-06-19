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
package org.onehippo.test.channels;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;
import org.onehippo.test.annotations.AllInOneAnnotation;


/**
 * Retrieves the properties of the GoGreen channels.
 */
@FieldGroupList({
        @FieldGroup(
                titleKey = "fields.channel",
                value = { "logo", "pageTitlePrefix" }
        )
})
public interface AllInOneChannelInfo extends ChannelInfo {

    @Parameter(name = "logo", displayName = "Logo")
//    @JcrPath(
//            pickerSelectableNodeTypes = { "hippogogreengallery:imageset" },
//            pickerInitialPath = "/content/gallery/logos"
//    )
    @AllInOneAnnotation
    String getLogoPath();

    @Parameter(name = "pageTitlePrefix", displayName = "Page title prefix", defaultValue = "Hippo Go Green")
    String getPageTitlePrefix();

}
