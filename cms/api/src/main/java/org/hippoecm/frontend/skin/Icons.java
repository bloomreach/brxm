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

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * References to icons.
 */
public class Icons {

    public static final ResourceReference BULLET_XL = new PackageResourceReference(Icons.class, "images/icons/bullet-xlarge.svg");
    
    public static final ResourceReference FOLDER_TINY = new PackageResourceReference(Icons.class, "images/icons/folder-tiny.svg");
    public static final ResourceReference FOLDER_OPEN_TINY = new PackageResourceReference(Icons.class, "images/icons/folder-open-tiny.svg");
    
    public static final Map<String, ResourceReference> ALL = new HashMap<>(3);
    
    static {
        ALL.put("bullet-xlarge", BULLET_XL);
        
        ALL.put("folder-tiny", FOLDER_TINY);
        ALL.put("folder-open-tiny", FOLDER_OPEN_TINY);
    }

    public static ResourceReference byName(final String name, final String size) {
        return ALL.get(name + "-" + size);
    }
}
