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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.service.IconSize;

/**
 * References to icons.
 */
public enum Icon {

    BULLET_SMALL,
    BULLET_MEDIUM,
    BULLET_LARGE,
    BULLET_XLARGE,

    CARET_SMALL,
    CARET_MEDIUM,
    CARET_LARGE,
    CARET_XLARGE,

    DROPDOWN_TINY,

    FOLDER_TINY,
    FOLDER_OPEN_TINY;

    private PackageResourceReference cachedReference;
    
    /**
     * @return a resource reference for the icon.
     */
    public PackageResourceReference getReference() {
        if (cachedReference == null) {
            final String fileName = StringUtils.replace(name().toLowerCase(), "_", "-");
            cachedReference = getIconReference(fileName);
        }
        return cachedReference;
    }

    private static PackageResourceReference getIconReference(final String name) {
        return new PackageResourceReference(Icon.class, "images/icons/" + name + ".svg");
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
    public static PackageResourceReference referenceByName(final String name, final IconSize size, final Icon defaultIcon) {
        final String fullName = name + "-" + size.name().toLowerCase();
        final PackageResourceReference reference = getIconReference(fullName);
        if (PackageResource.exists(reference.getKey())) {
            return reference;
        } else if (defaultIcon != null) {
            return defaultIcon.getReference();
        } else {
            return null;
        }
    }

}
