package org.hippoecm.hst.content.beans;

import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType = "contentbeanstest:deeperlink")
public class DeeperLink extends HippoDocument {
    public DeepLink getContentbeanstest_deeplink() {
        return getBean("contentbeanstest:contentbeanstest_deeplink",
                DeepLink.class);
    }
}
