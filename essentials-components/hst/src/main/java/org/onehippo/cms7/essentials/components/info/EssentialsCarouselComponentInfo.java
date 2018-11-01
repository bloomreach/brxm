/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.essentials.components.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
        @FieldGroup(
                titleKey = "group.documents",
                value = {"document1", "document2", "document3", "document4", "document5", "document6"}
        ),
        @FieldGroup(
                titleKey = "group.settings",
                value = {"cycle", "pause", "interval", "showNavigation", "carouselWidth", "carouselHeight", "carouselBackgroundColor"}
        )
})
public interface EssentialsCarouselComponentInfo {
    String HIPPO_DOCUMENT = "hippo:document";
    String BANNERS_INITIAL_PATH = "banners";
    String CMS_PICKERS_DOCUMENTS_ONLY = "cms-pickers/documents-only";

    //############################################
    // CAROUSEL SETTINGS
    //############################################
    @Parameter(name = "pause", defaultValue = "true")
    Boolean getPause();

    @Parameter(name = "cycle", defaultValue = "true")
    Boolean getCycle();

    @Parameter(name = "carouselHeight", defaultValue = "250", required = true)
    Integer getCarouselHeight();

    @Parameter(name = "carouselWidth", defaultValue = "700", required = true)
    Integer getCarouselWidth();

    @Parameter(name = "interval", defaultValue = "3000", required = true)
    Integer getInterval();

    @Parameter(name = "carouselBackgroundColor", defaultValue = "#FFFFFF", required = true)
    String getCarouselBackgroundColor();

    @Parameter(name = "showNavigation", defaultValue = "true")
    Boolean getShowNavigation();

    //############################################
    // DOCUMENTS
    //############################################
    @Parameter(name = "document1", required = true)
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem1();

    @Parameter(name = "document2")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem2();

    @Parameter(name = "document3")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem3();

    @Parameter(name = "document4")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem4();

    @Parameter(name = "document5")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem5();

    @Parameter(name = "document6")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem6();
}
