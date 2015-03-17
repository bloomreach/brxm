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
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * References to icons.
 */
public enum Icon {
    ARROW_DOWN,
    ARROW_UP,
    BULLET_EXTRA_LARGE,
    BULLET_LARGE,
    BULLET_MEDIUM,
    BULLET_SMALL,
    BULLHORN_THINNER,
    CALENDAR_DAY,
    CALENDAR_MONTH,
    CARET_DOWN_CIRCLE,
    CARET_DOWN_EXTRA_LARGE,
    CARET_DOWN_LARGE,
    CARET_DOWN_MEDIUM,
    CARET_DOWN_SMALL,
    CARET_RIGHT_EXTRA_LARGE,
    CARET_RIGHT_LARGE,
    CARET_RIGHT_MEDIUM,
    CARET_RIGHT_SMALL,
    CARET_UP_CIRCLE,
    CHECK_CIRCLE,
    CHECK_CIRCLE_CLOCK,
    CHECK_SQUARE,
    CHEVRON_DOWN,
    CHEVRON_DOWN_THICK,
    CHEVRON_DOWN_THIN,
    CHEVRON_DOWN_CIRCLE,
    CHEVRON_LEFT,
    CHEVRON_LEFT_THICK,
    CHEVRON_LEFT_THIN,
    CHEVRON_RIGHT,
    CHEVRON_RIGHT_THICK,
    CHEVRON_RIGHT_THIN,
    CHEVRON_UP,
    CHEVRON_UP_THICK,
    CHEVRON_UP_THIN,
    CHEVRON_UP_CIRCLE,
    CODE,
    COMPONENT,
    COMPRESS,
    CROP,
    EMPTY,
    EXCLAMATION,
    EXCLAMATION_CIRCLE,
    EXCLAMATION_TRIANGLE,
    EXPAND,
    FILE,
    FILES,
    FILE_COMPOUND,
    FILE_COMPOUND_PLUS,
    FILE_IMAGE,
    FILE_IMAGE_PLUS,
    FILE_NEWS,
    FILE_PENCIL,
    FILE_PLUS,
    FILE_TEXT,
    FILE_TEXT_THIN,
    FILE_THIN,
    FILE_UNLOCKED,
    FLASK,
    FLOPPY,
    FLOPPY_TIMES_CIRCLE,
    FOLDER,
    FOLDER_OPEN,
    FOLDER_PLUS,
    FOLDER_THIN,
    FONT,
    FORWARD,
    GEAR,
    GLOBE,
    GLOBE_ABSTRACT,
    INFO_CIRCLE,
    LINK,
    LIST_UL,
    LOCKED,
    MINUS_CIRCLE,
    MINUS_CIRCLE_CLOCK,
    MOVE_INTO,
    OVERLAY_CHECK_CIRCLE_THIN,
    OVERLAY_CHECK_CIRCLE_EXCLAMATION_TRIANGLE_THIN,
    OVERLAY_MINUS_CIRCLE_THIN,
    OVERLAY_PLUS,
    PENCIL_SQUARE,
    PLUS,
    PLUS_SQUARE,
    REFRESH,
    RESTORE,
    SEARCH,
    SORT,
    STEP_BACKWARD,
    STEP_FORWARD,
    THUMBNAILS,
    TIMES,
    TIMES_CIRCLE,
    TRANSLATE,
    TYPE,
    UNLOCKED;

    private static final Logger log = LoggerFactory.getLogger(Icon.class);

    private static final String ICONS_DIR = "images/icons/";
    private static final String SPRITE_FILE_NAME = ICONS_DIR + "hippo-icon-sprite.svg";

    public static String getIconSprite() {
        PackageResourceReference hippoIcons = getIconSpriteReference();
        try {
            return svgAsString(hippoIcons);
        } catch (ResourceStreamNotFoundException|IOException e) {
            log.warn("Cannot find Hippo icon sprite", e);
            return "";
        }
    }

    private static PackageResourceReference getIconSpriteReference() {
        return new PackageResourceReference(Icon.class, SPRITE_FILE_NAME);
    }

    private static String svgAsString(PackageResourceReference reference) throws ResourceStreamNotFoundException, IOException {
        final PackageResource resource = reference.getResource();
        final IResourceStream resourceStream = resource.getResourceStream();
        if (resourceStream == null) {
            throw new NullPointerException("Failed to load SVG icon " + resource.toString());
        }
        String data = IOUtils.toString(resourceStream.getInputStream());
        //skip everything (comments, xml declaration and dtd definition) before <svg element
        return data.substring(data.indexOf("<svg "));
    }

    private String getFileName() {
        return StringUtils.replace(name().toLowerCase(), "_", "-");
    }

    /**
     * Returns an inline svg representation of this icon that refers to the icon in the sprite.
     * It is of the form <svg class="..css classes.."><use xlink:href="#spriteId"/></svg>
     *
     * @see Icon#getSpriteId()
     * @see Icon#getCssClasses()
     */
    public String getSpriteReference() {
        return "<svg class=\"" + getCssClasses() + "\"><use xlink:href=\"#" + getSpriteId() + "\" /></svg>";
    }

    /**
     * Returns an inline svg representation of this icon. All CSS classes of this icon will be set.
     *
     * @see Icon#getCssClasses()
     */
    public String getInlineSvg() {
        final String iconPath = ICONS_DIR + getFileName() + ".svg";
        final PackageResourceReference reference = new PackageResourceReference(Icon.class, iconPath);
        try {
            return "<svg class=\"" + getCssClasses() + "\" " + StringUtils.substringAfter(svgAsString(reference), "<svg ");
        } catch (ResourceStreamNotFoundException|IOException e) {
            log.warn("Cannot find inline svg of {}", name(), e);
            return "";
        }
    }

    /**
     * @return the id of this icon in the generated icon sprint.
     * For example, the icon {@link FOLDER_OPEN} will have the
     * icon sprite id "hi-folder-open".
     */
    String getSpriteId() {
        return "hi-" + getFileName();
    }

    /**
     * @return all CSS helper classes to identify an icon. For example, the icon {@link FOLDER_OPEN_THIN}
     * will get the CSS classes "hi hi-folder-open hi-small".
     */
    private String getCssClasses() {
        final String[] nameParts = StringUtils.split(name().toLowerCase(), '_');
        final StringBuilder cssClasses = new StringBuilder("hi");

        final String lastNamePart = nameParts[nameParts.length -1];
        // Map thickness of lines in svg to dimensions of svg element
        switch (lastNamePart) {
            case "thick":
                cssClasses.append(" hi-mini");
                break;
            case "thin":
                cssClasses.append(" hi-small");
                break;
            case "thinner":
                cssClasses.append(" hi-medium");
                break;
            default:
                cssClasses.append(" hi-tiny");
                break;
        }

        String name = null;
        for (int i = 0; i < nameParts.length; i++) {
            if (name == null) {
                name = " hi-" + nameParts[i];
            } else if (!nameParts[i].equals("thick") && !nameParts[i].equals("thin") && !nameParts[i].equals("thinner")) {
                name += "-" + nameParts[i];
            }
        }
        cssClasses.append(name);

        return cssClasses.toString();
    }
}
