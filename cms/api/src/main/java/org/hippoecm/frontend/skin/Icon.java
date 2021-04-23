/*
 *  Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.hippoecm.frontend.service.IconSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * References to icons.
 */
public enum Icon {

    ARROW_DOWN,
    ARROW_DOWN_LINE,
    ARROW_FAT_DOWN_CIRCLE,
    ARROW_UP,
    ARROW_UP_LINE,
    ARROW_RIGHT_SQUARE,
    BELL,
    BULLET,
    BULLHORN,
    CALENDAR_DAY,
    CALENDAR_MONTH,
    CARET_DOWN,
    CARET_DOWN_CIRCLE,
    CARET_RIGHT,
    CARET_UP_CIRCLE,
    CHECK_CIRCLE,
    CHECK_CIRCLE_CLOCK,
    CHECK_SQUARE,
    CHEVRON_DOWN_CIRCLE,
    CHEVRON_DOWN,
    CHEVRON_LEFT_CIRCLE,
    CHEVRON_LEFT,
    CHEVRON_RIGHT_CIRCLE,
    CHEVRON_RIGHT,
    CHEVRON_UP_CIRCLE,
    CHEVRON_UP,
    CODE,
    COMPONENT,
    COMPRESS,
    CROP,
    EMPTY,
    EXCLAMATION_CIRCLE,
    EXCLAMATION,
    EXCLAMATION_TRIANGLE,
    EXPAND,
    FILE_COMPOUND,
    FILE_IMAGE,
    FILE,
    FILE_NEWS,
    FILE_PENCIL,
    FILE_TEXT,
    FILES,
    FLASK,
    FLOPPY,
    FOLDER,
    FOLDER_OPEN,
    FONT,
    FORWARD,
    GEAR,
    GLOBE_ABSTRACT,
    GLOBE,
    INFO_CIRCLE,
    INFO,
    LINK,
    LIST_UL,
    LOCKED,
    MIMETYPE_AUDIO,
    MIMETYPE_BINARY,
    MIMETYPE_DOC,
    MIMETYPE_DOCX,
    MIMETYPE_FLASH,
    MIMETYPE_IMAGE,
    MIMETYPE_ODP,
    MIMETYPE_ODS,
    MIMETYPE_ODT,
    MIMETYPE_PDF,
    MIMETYPE_PPT,
    MIMETYPE_PPTX,
    MIMETYPE_RTF,
    MIMETYPE_SXC,
    MIMETYPE_SXI,
    MIMETYPE_SXW,
    MIMETYPE_TEXT,
    MIMETYPE_VIDEO,
    MIMETYPE_XLS,
    MIMETYPE_XLSX,
    MIMETYPE_ZIP,
    MINUS_CIRCLE,
    MINUS_CIRCLE_CLOCK,
    MORE,
    MOVE_INTO,
    PENCIL_SQUARE,
    PIE_CHART,
    PLUS,
    PLUS_SQUARE,
    REFRESH,
    RESTORE,
    SEARCH,
    SORT,
    SORT_BY_ALPHA,
    STEP_BACKWARD,
    STEP_FORWARD,
    THUMBNAILS,
    TIMES,
    TIMES_CIRCLE,
    TRANSLATE,
    TYPE,
    UNLINK,
    UNLOCKED,
    USER_CIRCLE,
    XPAGE_DOCUMENT,
    XPAGE_FOLDER,
    XPAGE_FOLDER_OPEN;

    private static final Logger log = LoggerFactory.getLogger(Icon.class);

    private static final String SPRITE_FILE_NAME = "images/icons/hippo-icon-sprite.svg";

    public static String getIconSprite() {
        PackageResourceReference iconSprite = new PackageResourceReference(Icon.class, SPRITE_FILE_NAME);
        try {
            return IconUtil.svgAsString(iconSprite);
        } catch (ResourceStreamNotFoundException|IOException e) {
            log.warn("Cannot find Hippo icon sprite", e);
            return "";
        }
    }

    /**
     * Returns an inline svg representation of this icon that refers to the icon in the sprite. It is of the form <svg
     * class="..css classes.."><use xlink:href="#spriteId"/></svg>
     *
     * @param size       the size of the icon.
     * @param cssClasses additional CSS classes to set on the SVG element.
     * @see Icon#getSpriteId(IconSize)
     * @see Icon#getCssClasses(IconSize)
     */
    public String getSpriteReference(IconSize size, String... cssClasses) {
        String extraCssClasses = IconUtil.cssClassesAsString(cssClasses);
        if (StringUtils.isNotEmpty(extraCssClasses)) {
            extraCssClasses = " " + extraCssClasses;
        }
        return "<svg class=\"" + getCssClasses(size) + extraCssClasses + "\">" +
                "<use xlink:href=\"#" + getSpriteId(size) + "\" /></svg>";
    }

    /**
     * @return the id of this icon in the generated icon sprint.
     * For example, the icon {@link #FOLDER_OPEN} will have the
     * icon sprite id "hi-folder-open".
     */
    String getSpriteId(final IconSize size) {
        return "hi-"
                + StringUtils.replace(name().toLowerCase(), "_", "-")
                + "-"
                + size.name().toLowerCase();
    }

    /**
     * @return all CSS helper classes to identify an icon. For example, the icon {@link #FOLDER_OPEN}
     * will get the CSS classes "hi hi-folder-open hi-small".
     */
    String getCssClasses(final IconSize size) {
        return "hi hi-" + name().toLowerCase().replace("_", "-") + " hi-" + size.name().toLowerCase();
    }
}
