/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.model;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public class PageRepresentation {

    private String canonicalIdentifier;
    private String name;
    private boolean inherited;
    private boolean prototype;
    private String iconPath;

    public PageRepresentation represent(final HstComponentConfiguration page) {
        canonicalIdentifier = page.getCanonicalIdentifier();
        name = page.getName();
        inherited = page.isInherited();
        prototype = page.isPrototype();
        iconPath = page.getIconPath();
        return this;
    }

    public String getCanonicalIdentifier() {
        return canonicalIdentifier;
    }

    public void setCanonicalIdentifier(final String canonicalIdentifier) {
        this.canonicalIdentifier = canonicalIdentifier;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(final boolean inherited) {
        this.inherited = inherited;
    }

    public boolean isPrototype() {
        return prototype;
    }

    public void setPrototype(final boolean prototype) {
        this.prototype = prototype;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(final String iconPath) {
        this.iconPath = iconPath;
    }
}
