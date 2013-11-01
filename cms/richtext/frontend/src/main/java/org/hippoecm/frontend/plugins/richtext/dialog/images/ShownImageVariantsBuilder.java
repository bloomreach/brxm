/*
 *  Copyright 2008 Hippo.
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

import java.util.ArrayList;
import java.util.List;

class ShownImageVariantsBuilder {

    private List<String> allImageVariants;
    private List<String> includedImageVariants;
    private List<String> excludedImageVariants;
    private final List<String> shownImageVariants = new ArrayList<String>();

    /**
     * Constructs a list based on all image variants, the excluded image variants and the included image variants
     *
     * @param initials list with initial items
     * @param excludedImageVariants list with items that should be omitted
     * @param includedImageVariants list with items that should be added, null if no includedImageVariants are configured
     * @return The intersection of all image variants and included image variants minus the blacklist
     */
    static List<String> getAllowedList(List<String> initials, List<String> excludedImageVariants, List<String> includedImageVariants) {
        ShownImageVariantsBuilder builder = new ShownImageVariantsBuilder();
        builder.setAllImageVariants(initials);
        builder.setExcludedImageVariants(excludedImageVariants);
        builder.setIncludedImageVariants(includedImageVariants);
        builder.build();
        return builder.getShownImageVariants();
    }

    void build() {
        shownImageVariants.addAll(allImageVariants);
        if (useIncludedImageVariants()) {
            shownImageVariants.retainAll(includedImageVariants);
        }
        if (useExcludedImageVariants()){
            shownImageVariants.removeAll(excludedImageVariants);
        }
    }

    private boolean useExcludedImageVariants() {
        return excludedImageVariants!=null;
    }

    List<String> getShownImageVariants() {
        return shownImageVariants;
    }

    void setAllImageVariants(final List<String> allImageVariants) {
        this.allImageVariants = allImageVariants;
    }

    void setIncludedImageVariants(final List<String> includedImageVariants) {
        this.includedImageVariants = includedImageVariants;
    }

    void setExcludedImageVariants(final List<String> excludedImageVariants) {
        this.excludedImageVariants = excludedImageVariants;
    }

    private boolean useIncludedImageVariants() {
        return includedImageVariants !=null;
    }
}
