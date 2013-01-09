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

    TINY(16), SMALL(32), MEDIUM(48), LARGE(64), HUGE(128);

    /**
     * Finds the most appropriate IconType for a particular configuration value.
     * The symbolic names (tiny, small, medium, large & huge) are preferred.
     * 
     * @param name
     * @return
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
            return HUGE;
        } catch (NumberFormatException nfe) {
            // not an integer
            ITitleDecorator.log.warn("Invalid name '" + name + "' specified for IconType.  Use 'tiny', 'small', 'medium', 'large', 'huge' or an integer.");
        }
        return MEDIUM;
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