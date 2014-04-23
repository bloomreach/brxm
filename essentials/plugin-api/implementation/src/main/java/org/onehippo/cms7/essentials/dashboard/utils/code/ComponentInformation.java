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

package org.onehippo.cms7.essentials.dashboard.utils.code;

import java.util.ArrayList;
import java.util.List;

/**
 * Used
 *
 * @version "$Id: ComponentInformation.java 172484 2013-08-01 12:29:37Z mmilicevic $"
 */
public class ComponentInformation {


    private final List<String> imports = new ArrayList<>();
    private String extendingComponentName;
    private String targetClassName;
    private String targetPackageName;

    public void addImport(final String importName) {
        imports.add(importName);
    }

    public void addDefaultComponentImports() {
        addImport("org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated");
        addImport("org.hippoecm.hst.core.component.HstRequest");
        addImport("org.hippoecm.hst.core.component.HstResponse");
        addImport("org.hippoecm.hst.core.parameters.ParametersInfo");
        addImport("org.slf4j.LoggerFactory");
        addImport("org.slf4j.Logger");

    }

    public List<String> getImports() {
        return imports;
    }

    public String getExtendingComponentName() {
        return extendingComponentName;
    }

    public void setExtendingComponentName(final String extendingComponentName) {
        this.extendingComponentName = extendingComponentName;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public void setTargetClassName(final String targetClassName) {
        this.targetClassName = targetClassName;
    }

    public String getTargetPackageName() {
        return targetPackageName;
    }

    public void setTargetPackageName(final String targetPackageName) {
        this.targetPackageName = targetPackageName;
    }
}
