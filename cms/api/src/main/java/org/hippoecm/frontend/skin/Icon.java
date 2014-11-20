/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * References to icons.
 */
public enum Icon {

    BULLET_TINY,
    BULLET_LARGE,
    CARET_UP_TINY,
    CARET_RIGHT_TINY,
    CARET_DOWN_TINY,
    CARET_LEFT_TINY,
    DROPDOWN_TINY,
    FOLDER_TINY,
    FOLDER_OPEN_TINY;

    private static final Logger log = LoggerFactory.getLogger(Icon.class);

    private static final String SPRITE_FILE_NAME = "images/icons/hippo-icons.svg";

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
        String data = IOUtils.toString(reference.getResource().getResourceStream().getInputStream());
        //skip everything (comments, xml declaration and dtd definition) before <svg element
        return data.substring(data.indexOf("<svg "));
    }

    private String getFileName() {
        return StringUtils.replace(name().toLowerCase(), "_", "-");
    }

    /**
     * Returns an inline svg representation of this icon of the form
     * <svg class="..css classes.."><use xlink:href="#spriteId"/></svg>
     *
     * @see Icon#getSpriteId()
     * @see Icon#getCssClasses()
     */
    public String getInlineSvg() {
        return "<svg class=\"" + getCssClasses() + "\"><use xlink:href=\"#" + getSpriteId() + "\" /></svg>";
    }

    /**
     * @return the id of this icon in the generated icon sprint.
     * For example, the icon {@link CARET_DOWN_MEDIUM} will have the
     * icon sprite id "hi-caret-down-medium".
     */
    String getSpriteId() {
        return "hi-" + getFileName();
    }

    /**
     * @return all CSS helper classes to identify an icon. For example, the icon {@link CARET_DOWN_TINY}
     * will get the CSS classes "hi hi-medium hi-caret hi-caret-down".
     */
    private String getCssClasses() {
        final StringBuilder cssClasses = new StringBuilder("hi");

        final String[] nameParts = StringUtils.split(name().toLowerCase(), '_');
        final String name = nameParts[0];                       // e.g. 'caret' in CARET_DOWN_TINY
        final String size = nameParts[nameParts.length - 1];    // e.g. 'tiny' in CARET_DOWN_TINY

        cssClasses.append(" hi-").append(size);
        cssClasses.append(" hi-").append(name);

        if (nameParts.length == 3) {
            final String variant = nameParts[1];                // e.g. 'down' in 'CARET_DOWN_MEDIUM'
            cssClasses.append(" hi-").append(name).append("-").append(variant);
        }
        return cssClasses.toString();
    }

}
