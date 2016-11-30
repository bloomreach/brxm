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
