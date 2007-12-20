/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.template;

import org.apache.wicket.IClusterable;

public class FieldDescriptor implements IClusterable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String path;
    private String template;
    private String renderer;

    public FieldDescriptor(String name, String path, String template, String renderer) {
        this.name = name;
        this.path = path;
        this.template = template;
        this.renderer = renderer;
    }

    public String getName() {
        return name;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getTemplate() {
        return template;
    }
    
    public String getRenderer() {
        return renderer;
    }
}
