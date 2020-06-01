/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.components;

import java.util.Date;

import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

public interface BannerInfo {
    @Parameter(name = "bannerWidth", displayName = "Banner Width")
    int getBannerWidth();

    @Parameter(name = "yesNo", displayName = "Yes or No ?")
    int getYesNO();

    @Parameter(name = "date", displayName = "Some Date")
    Date getDate();

    @Parameter(name = "borderColor", displayName = "Border Color", required = true)
    String getBorderColor();

    @Parameter(name = "content", displayName = "Content")
    String getContent();

    @Parameter(name = "bannerPath", displayName = "Banner Path")
    @JcrPath(
            pickerSelectableNodeTypes = {"demosite:qaimageset"},
            pickerInitialPath = "/content/gallery/images")
    String getPath();

    @Parameter(name = "cssDisplay", displayName = "CSS Display")
    @DropDownList(value = {"inline", "block", "flex"})
    String getCssDisplay();

    @Parameter(name = "cssDisplay2", displayName = "CSS Display 2")
    @DropDownList(valueListProvider = CssDisplayValueListProvider.class)
    String getCssDisplay2();
}
