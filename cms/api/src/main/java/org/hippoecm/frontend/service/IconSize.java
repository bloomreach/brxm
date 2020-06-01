/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service;

/**
 * Icon types (sizes) used.
 */
public enum IconSize {

    S(8),
    M(16),
    L(32),
    XL(48);

    /**
     * Finds the most appropriate icon size for a particular configuration value.
     * The symbolic names (s, m, l and xl) are preferred.
     *
     * @param name a symbolic icon size name (case insensitive) or an integer size value.
     * @return the most appropriate icon size: either the exact matching one, or the first
     * icon size that is equal to or bigger than the given integer size.
     */
    public static IconSize getIconSize(String name) {
        for (IconSize type : values()) {
            if (type.toString().equalsIgnoreCase(name)) {
                return type;
            }
        }
        try {
            int size = Integer.parseInt(name);
            for (IconSize type : values()) {
                if (type.getSize() >= size) {
                    return type;
                }
            }
            return XL;
        } catch (NumberFormatException nfe) {
            // not an integer
            ITitleDecorator.log.warn("Invalid name '" + name + "' specified for IconType.  Use 's', 'm', 'l', 'xl', or an integer.");
        }
        return M;
    }

    private int size;

    private IconSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
