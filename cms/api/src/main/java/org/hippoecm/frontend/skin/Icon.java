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
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * References to icons.
 */
public enum Icon {
    ADD_DOCUMENT_TINY,
    ADD_FOLDER_TINY,
    ADD_OVERLAY_TINY,
    ARROW_DOWN_TINY,
    ARROW_UP_TINY,
    BULLET_LARGE,
    BULLET_TINY,
    CALENDAR_MONTH_TINY,
    CARET_UP_TINY,
    CARET_RIGHT_TINY,
    CARET_DOWN_TINY,
    CARET_LEFT_TINY,
    CLOSE_TINY,
    COLLAPSE_TINY,
    CONFIGURATION_TINY,
    CONTEXT_MENU_TINY,
    DELETE_TINY,
    DEPUBLISH_SCHEDULED_TINY,
    DEPUBLISH_TINY,
    DOCUMENT_TINY,
    DOCUMENT_SMALL,
    DOCUMENT_FILES_TINY,
    DOCUMENT_UNLOCKED_TINY,
    DROPDOWN_TINY,
    EDIT_TINY,
    EMPTY_TINY,
    EMPTY_SMALL,
    EXPAND_TINY,
    FOLDER_TINY,
    FOLDER_SMALL,
    FOLDER_OPEN_TINY,
    GLOBAL_TINY,
    HISTORY_TINY,
    INFO_CIRCLE_TINY,
    LIST_TINY,
    MOVE_TINY,
    NEW_COLLECTION_TINY,
    NEW_COMPOUND_TINY,
    NEW_DOCUMENT_TINY,
    NEW_FILE_FOLDER_TINY,
    NEW_FOLDER_TINY,
    NEW_IMAGE_FOLDER_TINY,
    NEW_IMAGE_TINY,
    NEW_TRANSLATED_FOLDER_TINY,
    ORDER_TINY,
    PLUS_TINY,
    PUBLISH_SCHEDULED_TINY,
    PUBLISH_TINY,
    RENAME_TINY,
    RESTORE_TINY,
    SAVE_TINY,
    SAVE_CLOSE_TINY,
    SEARCH_TINY,
    STATE_CHANGED_SMALL,
    STATE_LIVE_SMALL,
    STATE_NEW_SMALL,
    THUMBNAILS_TINY,
    TRANSLATE_TINY,
    UNLOCKED_TINY,
    URL_NAVIGATION_TINY;

    private static final Logger log = LoggerFactory.getLogger(Icon.class);

    private static final String ICONS_DIR = "images/icons/";
    private static final String SPRITE_FILE_NAME = ICONS_DIR + "hippo-icons.svg";

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
     * For example, the icon {@link CARET_DOWN_TINY} will have the
     * icon sprite id "hi-caret-down-tiny".
     */
    String getSpriteId() {
        return "hi-" + getFileName();
    }

    /**
     * @return all CSS helper classes to identify an icon. For example, the icon {@link CARET_DOWN_TINY}
     * will get the CSS classes "hi hi-caret hi-caret-down hi-tiny".
     */
    private String getCssClasses() {
        final String[] nameParts = StringUtils.split(name().toLowerCase(), '_');
        final StringBuilder cssClasses = new StringBuilder("hi");

        String name = null;
        for (int i = 0; i < nameParts.length; i++) {
            if (i == nameParts.length - 1) {
                // Last name part is always the size e.g. 'tiny' in CARET_DOWN_TINY
                cssClasses.append(" hi-").append(nameParts[i]);
            } else {
                if (name == null) {
                    name = " hi-" + nameParts[i];
                } else {
                    name += "-" + nameParts[i];
                }
                cssClasses.append(name);
            }
        }
        return cssClasses.toString();
    }

}
