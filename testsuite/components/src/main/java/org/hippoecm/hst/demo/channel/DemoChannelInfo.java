/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.channel;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.demo.components.CssDisplayValueListProvider;

@FieldGroupList({
        @FieldGroup(
                titleKey = "fields.demochannel",
                value = { "exampleValue", "theme", "defaultCssDisplay" }
        )
})
public interface DemoChannelInfo extends ChannelInfo {

    @Parameter(name = "exampleValue")
    String getExampleValue();

    @Parameter(name = "theme", defaultValue = "default")
    @DropDownList({"default", "metal", "warm"})
    String getTheme();

    @Parameter(name = "defaultCssDisplay")
    @DropDownList(valueListProvider = CssDisplayValueListProvider.class)
    String getDefaultCssDisplay();

}
