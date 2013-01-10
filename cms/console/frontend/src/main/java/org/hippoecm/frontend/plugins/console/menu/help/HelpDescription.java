/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.hippoecm.frontend.plugins.console.menu.help;

import org.apache.wicket.IClusterable;

/**
 * Help definition holder
 *
 * @version "$Id$"
 */
public class HelpDescription implements IClusterable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private String shortcutName;


    public HelpDescription(String name, String description, String shortcutName) {
        this.name = name;
        this.description = description;
        this.shortcutName = shortcutName;
    }

    public HelpDescription() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShortcutName() {
        return shortcutName;
    }

    public void setShortcutName(String shortcutName) {
        this.shortcutName = shortcutName;
    }
}
