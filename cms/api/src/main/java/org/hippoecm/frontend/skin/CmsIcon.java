/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.skin;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.hippoecm.frontend.service.IconSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * References to icons that are only available in the CMS (as opposed to icons in {@link Icon} that are
 * coming from the Hippo Theme).
 */
public enum CmsIcon {

    AJAX_LOADER,
    DELETE,
    DELETE_HOVER,
    FILE_UNLOCKED,
    FLOPPY_TIMES_CIRCLE,
    OVERLAY_PLUS,
    SORT_ASCENDING,
    SORT_DESCENDING,
    STATEORDER_DOWN,
    STATEORDER_HOVER,
    STATEORDER_NONE,
    STATEORDER_UP;

    private static final Logger log = LoggerFactory.getLogger(CmsIcon.class);

    private static final String ICONS_DIR = "images/icons/";

    /**
     * Returns an inline svg representation of this icon. All CSS classes of this icon will be set.
     *
     * @see org.hippoecm.frontend.skin.CmsIcon#getCssClasses
     */
    public String getInlineSvg(final IconSize size, String... cssClasses) {
        final String iconPath = ICONS_DIR + getFileName() + ".svg";
        final PackageResourceReference reference = new PackageResourceReference(CmsIcon.class, iconPath);
        try {
            return "<svg class=\"" + getCssClasses(size) + IconUtil.cssClassesAsString(cssClasses) + "\" "
                    + StringUtils.substringAfter(IconUtil.svgAsString(reference), "<svg ");
        } catch (ResourceStreamNotFoundException|IOException e) {
            log.warn("Cannot find inline svg of {}", name(), e);
            return "";
        }
    }

    /**
     * @return all CSS helper classes to identify an icon. For example, the icon {@link #DELETE_HOVER}
     * will get the CSS classes "hi hi-delete-hover".
     *
     * @param size The size of the icon
     */
    String getCssClasses(final IconSize size) {
        return "hi hi-" + getFileName() +  " hi-" + size.name().toLowerCase();
    }

    private String getFileName() {
        return StringUtils.replace(name().toLowerCase(), "_", "-");
    }
}
