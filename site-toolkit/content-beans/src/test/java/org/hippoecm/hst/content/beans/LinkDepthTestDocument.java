/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans;

import org.hippoecm.hst.content.beans.standard.HippoBean;

@Node(jcrType = "contentbeanstest:linkdepthtestdocument")
public class LinkDepthTestDocument extends ContentDocument {


    public HippoBean getDocbase() {
        final String item = getProperty("hippo:docbase");
        if (item == null) {
            return null;
        }
        return getBeanByUUID(item, HippoBean.class);
    }

    public HippoBean getHippo_mirror_() {
        return getLinkedBean("contentbeanstest:hippo_mirror_", HippoBean.class);
    }

    public DeepLink getContentbeanstest_deeplink() {
        return getBean("contentbeanstest:contentbeanstest_deeplink", DeepLink.class);
    }

    public DeeperLink getContentbeanstest_deeperlink() {
        return getBean("contentbeanstest:contentbeanstest_deeperlink",
            DeeperLink.class);
    }

    public DeepestLink getContentbeanstest_deepestlink() {
        return getBean("contentbeanstest:contentbeanstest_deepestlink",
            DeepestLink.class);
    }

    public TooDeepLink getContentbeanstest_toodeeplink() {
        return getBean("contentbeanstest:contentbeanstest_toodeeplink",
            TooDeepLink.class);
    }
}
