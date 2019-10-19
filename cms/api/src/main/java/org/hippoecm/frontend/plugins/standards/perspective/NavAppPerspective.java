/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend.plugins.standards.perspective;

import java.io.Serializable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Provides utility methods for NavApp related properties of Perspectives
 */
public class NavAppPerspective implements Serializable {


    private static final long serialVersionUID = 8374034275242281396L;

    private final String perspectiveClassName;
    private final String id;
    private final String path;

    /**
     * Constructs a new instance from the fully qualified perspective class name.
     * It is assumed that the provided class name is a subclass of Perspective.
     *
     * @param perspectiveClassName fully qualified class name
     */
    public NavAppPerspective(String perspectiveClassName) {

        this.perspectiveClassName = perspectiveClassName;

        final String lowerCasedPerspectiveName = getLowerCasedPerspectiveName(perspectiveClassName);
        id = String.format("hippo-perspective-%s", lowerCasedPerspectiveName);
        path = lowerCasedPerspectiveName.replace("perspective", "");
    }

    NavAppPerspective(Class<? extends Perspective> perspectiveClass) {
        this(perspectiveClass.getName());
    }

    /**
     * Returns the unique NavApp menu item id of the perspective with the given class name.
     *
     * @return app id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the NapApp path of this  perspective. The app path is part of the browser location URL and is needed
     * to support bookmark-able links.
     *
     * @return app path of the perspective
     */
    public String getAppPath() {
        return path;
    }

    /**
     * Returns the localized human readable name of the perspective with the given class name.
     *
     * @param locale requested locale
     * @return display name of the perspective
     */
    public String getDisplayName(Locale locale) {
        try {
            final ResourceBundle bundle = ResourceBundle.getBundle(perspectiveClassName, locale);
            if (bundle != null && bundle.containsKey(Perspective.TITLE_KEY)) {
                return bundle.getString(Perspective.TITLE_KEY);
            }
        } catch (MissingResourceException ignored) {
            // perspective-title and resource bundle are optional. Ignore if the resource bundle doesn't exist
        }
        return null;
    }

    private static String getLowerCasedPerspectiveName(String perspectiveClassName) {
        final int lastDotIndex = perspectiveClassName.lastIndexOf('.');
        return perspectiveClassName.substring(1 + lastDotIndex).toLowerCase();
    }

}
