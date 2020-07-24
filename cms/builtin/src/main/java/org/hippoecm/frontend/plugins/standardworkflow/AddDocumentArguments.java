/*
 *  Copyright 2012-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standardworkflow;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;

public class AddDocumentArguments implements IClusterable {
    private String targetName;
    private String uriName;
    private String prototype;
    private String language;
    /* The identifier of the selected XPageLayout */
    private XPageLayout xPageLayout;

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(final String targetName) {
        this.targetName = targetName;
    }

    public String getUriName() {
        return uriName;
    }

    public void setUriName(final String uriName) {
        this.uriName = uriName;
    }

    public String getPrototype() {
        return prototype;
    }

    public void setPrototype(final String prototype) {
        this.prototype = prototype;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public XPageLayout getXPageLayout() {
        return xPageLayout;
    }

    public void setXPageLayout(final XPageLayout xPageLayout) {
        this.xPageLayout = xPageLayout;
    }
}
