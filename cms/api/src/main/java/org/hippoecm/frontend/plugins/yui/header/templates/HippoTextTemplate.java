/*
 *  Copyright 2008 Hippo.
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

package org.hippoecm.frontend.plugins.yui.header.templates;

import java.util.Map;

import org.apache.wicket.util.template.PackagedTextTemplate;

public abstract class HippoTextTemplate extends DynamicTextTemplate {


    private static final long serialVersionUID = 1L;

    private String moduleClass;

    public HippoTextTemplate(PackagedTextTemplate template, String moduleClass) {
        super(template);
        this.moduleClass = moduleClass;
    }

    public HippoTextTemplate(Class<?> clazz, String filename, String moduleClass) {
        this(new PackagedTextTemplate(clazz, filename), moduleClass);
    }

    @Override
    protected Map<String, Object> getVariables() {
        Map<String, Object> vars = super.getVariables();
        vars.put("id", getId());
        vars.put("class", moduleClass);
        return vars;
    }

    abstract public String getId();

}
