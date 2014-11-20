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
package org.hippoecm.frontend.plugins.standards.tree.icon;

import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.skin.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconResourceReference extends PackageResourceReference {
    public static final Logger log = LoggerFactory.getLogger(IconResourceReference.class);
    
    private Icon icon;
    
    public IconResourceReference(Icon icon) {
        super("icon-resource-reference");
        this.icon = icon;
    }

    @Override
    public PackageResource getResource() {
        throw new UnsupportedOperationException("IconResourceReference is only meant as a marker class");
    }

    public Icon getIcon() {
        return icon;
    }

}
