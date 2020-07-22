/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.hst.platform.api.experiencepages;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;

public class XPageLayout {

    private final String label;
    private final String key;
    private JcrTemplateNode jcrTemplateNode;

    public XPageLayout(final String key, final String label, final JcrTemplateNode jcrTemplateNode) {
        this.key = key;
        if (StringUtils.isEmpty(label)) {
            this.label = key;
        } else {
            this.label = label;
        }
        this.jcrTemplateNode = jcrTemplateNode;
    }

    public String getLabel() {
        return label;
    }

    public String getKey() {
        return key;
    }

    public JcrTemplateNode getJcrTemplateNode() {
        return jcrTemplateNode;
    }

}
