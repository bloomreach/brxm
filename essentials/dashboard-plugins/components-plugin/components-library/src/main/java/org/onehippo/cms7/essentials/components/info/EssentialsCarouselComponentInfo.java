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

import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * @version "$Id$"
 */
@FieldGroupList({
        @FieldGroup(
                titleKey = "carousel.documents",
                value = {"document1", "document2", "document3", "document4", "document5",
                        "document6", "document7", "document8", "document9",  "document10"}
        ),
        @FieldGroup(
                titleKey = "carousel.settings",
                value = {"fx", "displayTime", "showNavigation"}
        )
})

public interface EssentialsCarouselComponentInfo {


    //############################################
    // CAROUSEL SETTINGS
    //############################################
    @Parameter(name = "fx", required = true, displayName = "Animation type")
    @DropDownList(value = {"scrollHorz", "carousel","flipHorz", "shuffle","tileSlide"})
    String fx();

    @Parameter(name = "displayTime", defaultValue = "2000", required = true, displayName = "Display time (milli seconds)")
    Integer displayTime();

    @Parameter(name = "showNavigation", defaultValue = "false", displayName = "Show navigation")
    Boolean showNavigation();







    //############################################
    // DOCUMENTS
    //############################################


    @Parameter(name = "document1", required = false, displayName = "Carousel item 1")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem1();

    @Parameter(name = "document2", required = false, displayName = "Carousel item 2")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem2();

    @Parameter(name = "document3", required = false, displayName = "Carousel item 3")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem3();

    @Parameter(name = "document4", required = false, displayName = "Carousel item 4")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem4();

    @Parameter(name = "document5", required = false, displayName = "Carousel item 5")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem5();

    @Parameter(name = "document6", required = false, displayName = "Carousel item 6")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem6();

    @Parameter(name = "document7", required = false, displayName = "Carousel item 7")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem7();

    @Parameter(name = "document8", required = false, displayName = "Carousel item 8")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem8();

    @Parameter(name = "document9", required = false, displayName = "Carousel item 9")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem9();

    @Parameter(name = "document10", required = false, displayName = "Carousel item 10")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem10();

}
