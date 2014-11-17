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

import java.util.EnumSet;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.service.IconSize;

/**
 * References to icons.
 */
public enum Icon {
    
    HIPPO_ICONS,

    BULLET_TINY,
    CARET_UP_TINY,
    CARET_RIGHT_TINY,
    CARET_DOWN_TINY,
    CARET_LEFT_TINY,
    DROPDOWN_TINY,
    FOLDER_TINY,
    FOLDER_OPEN_TINY;

    private IconResourceReference cachedReference;
    
    /**
     * @return a resource reference for the icon.
     */
    public IconResourceReference getReference() {
        if (cachedReference == null) {
            final String fileName = StringUtils.replace(name().toLowerCase(), "_", "-");
            cachedReference = getIconReference(fileName, this);
        }
        return cachedReference;
    }

    private static IconResourceReference getIconReference(final String name, final Icon icon) {
        return new IconResourceReference(Icon.class, "images/icons/" + name + ".svg", icon);
    }

    /**
     * Tries to look up an icon by name and size. Returns the resource reference of the default icon
     * if no such icon exists, or <code>null</code> if the default icon is <code>null</code>.
     * @param name the name of the icon
     * @param size the size of the icon
     * @param defaultIcon the icon to use when the icon with the given name and size cannot be found,
     *                    or null to return null as default value.
     * @return a resource reference for the icon, or the resource reference of the default icon when
     * an icon with the given name and size does not exists, or null when the default value is null.
     */
    public static IconResourceReference referenceByName(final String name, final IconSize size, final Icon defaultIcon) {
        final Icon icon = Icon.valueOf(name, size);
        if (icon != null) {
            return icon.getReference();
        } else if (defaultIcon != null) {
            return defaultIcon.getReference();
        } else {
            return null;
        }
    }

    private static Icon valueOf(final String name, final IconSize size) {
        final String enumName = StringUtils.replace(name, "-", "_") + "_" + size.toString();
        for (Icon value : EnumSet.allOf(Icon.class)) {
            if (enumName.equalsIgnoreCase(value.name())) {
                return value;
            }
        }
        return null;
    }
}
