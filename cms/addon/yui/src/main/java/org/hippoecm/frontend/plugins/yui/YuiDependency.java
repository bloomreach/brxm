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
package org.hippoecm.frontend.plugins.yui;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class YuiDependency implements Serializable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String namespace;
    private String module;
    private String suffix;

    private boolean hasCss;
    private boolean sourceNotFound;

    private YuiDependency[] optionalDependencies;

    public YuiDependency(String module) {
        this(YuiHeaderContributor.YUI_NAMESPACE, module);
    }

    public YuiDependency(String module, YuiDependency[] optionalDependencies) {
        this(YuiHeaderContributor.YUI_NAMESPACE, module, optionalDependencies);
    }

    public YuiDependency(String namespace, String module) {
        this(namespace, module, null);
    }

    public YuiDependency(String namespace, String module, YuiDependency[] optionalDependencies) {
        this.namespace = namespace;
        this.module = module;
        this.optionalDependencies = optionalDependencies;
        suffix = "";
    }

    public String getNamespace() {
        return namespace;
    }

    public String getModule() {
        return module;
    }

    public YuiDependency[] getOptionalDependencies() {
        return optionalDependencies;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getModulePath() {
        return getBasePath() + "/" + module;
    }

    public String getRealModulePath() {
        return getBasePath() + "/" + getRealModule();
    }

    public String getBasePath() {
        return YuiHeaderContributor.YUI_BUILD_ROOT + "/" + namespace + "/" + YuiHeaderContributor.YUI_BUILD_VERSION
                + "/" + module;
    }

    public String getRealModule() {
        return module + suffix;
    }

    public boolean isSourceNotFound() {
        return sourceNotFound;
    }

    public void setSourceNotFound(boolean sourceNotFound) {
        this.sourceNotFound = sourceNotFound;
    }

    public void setHasCss(boolean hasCss) {
        this.hasCss = hasCss;
    }

    public boolean getHasCss() {
        return hasCss;
    }

    public String getCssPath() {
        return getBasePath() + "/assets/" + module + ".css";
    }

    public String toString() {
        return new ToStringBuilder(this).append("module", module).toString();
    }

    public int hashCode() {
        return new HashCodeBuilder(3, 7).append(module).toHashCode();
    }

    public boolean equals(Object pObject) {
        if (pObject instanceof YuiDependency) {
            YuiDependency bean = (YuiDependency) pObject;
            return new EqualsBuilder().append(module, bean.module).isEquals();
        }
        return false;
    }
}
