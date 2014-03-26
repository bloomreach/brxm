/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * @version "$Id$"
 */
@FieldGroupList({
        @FieldGroup(
                titleKey = "carousel.documents",
                value = {"document1", "document2", "document3", "document4", "document5",
                        "document6", "document7", "document8", "document9", "document10"}
        ),
        @FieldGroup(
                titleKey = "carousel.settings",
                value = {"cycle", "interval", "showNavigation"}
        )
})

public interface EssentialsCarouselComponentInfo {


    String HIPPO_DOCUMENT = "hippo:document";
    String BANNERS_INITIAL_PATH = "banners";
    String CMS_PICKERS_DOCUMENTS_ONLY = "cms-pickers/documents-only";

    //############################################
    // CAROUSEL SETTINGS
    //############################################
    @Parameter(name = "pause", displayName = "Pause carousel on mouse enter", defaultValue = "false")
    Boolean getPause();

    @Parameter(name = "cycle", displayName = "Cycle carousel continuously", defaultValue = "false")
    Boolean getCycle();

    @Parameter(name = "carouselHeight", defaultValue = "250", required = true, displayName = "Carousel height")
    Integer getCarouselHeight();

    @Parameter(name = "carouselWidth", defaultValue = "700", required = true, displayName = "Carousel width")
    Integer getCarouselWidth();

    @Parameter(name = "interval", defaultValue = "5000", required = true, displayName = "Carousel interval (milli seconds)")
    Integer getInterval();

    @Color
    @Parameter(name = "carouselBackgroundColor", defaultValue = "#FFFFFF", required = true, displayName = "Carousel background color)")
    String getCarouselBackgroundColor();

    @Parameter(name = "showNavigation", defaultValue = "true", displayName = "Show carousel navigation")
    Boolean getShowNavigation();


    //############################################
    // DOCUMENTS
    //############################################


    @Parameter(name = "document1", required = true, displayName = "Carousel item 1")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem1();

    @Parameter(name = "document2", required = false, displayName = "Carousel item 2")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem2();

    @Parameter(name = "document3", required = false, displayName = "Carousel item 3")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem3();

    @Parameter(name = "document4", required = false, displayName = "Carousel item 4")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem4();

    @Parameter(name = "document5", required = false, displayName = "Carousel item 5")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem5();

    @Parameter(name = "document6", required = false, displayName = "Carousel item 6")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem6();

    @Parameter(name = "document7", required = false, displayName = "Carousel item 7")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem7();

    @Parameter(name = "document8", required = false, displayName = "Carousel item 8")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem8();

    @Parameter(name = "document9", required = false, displayName = "Carousel item 9")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem9();

    @Parameter(name = "document10", required = false, displayName = "Carousel item 10")
    @JcrPath(isRelative = true, pickerInitialPath = BANNERS_INITIAL_PATH, pickerSelectableNodeTypes = {HIPPO_DOCUMENT}, pickerConfiguration = CMS_PICKERS_DOCUMENTS_ONLY)
    String getCarouselItem10();

}
