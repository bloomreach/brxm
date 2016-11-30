package org.hippoecm.hst.content.beans;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType = "contentbeanstest:deeplink")
public class DeepLink extends HippoDocument {
    public HippoBean getHippo_mirror() {
        return getLinkedBean("contentbeanstest:hippo_mirror", HippoBean.class);
    }
}
