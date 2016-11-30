package org.hippoecm.hst.content.beans;

import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType = "contentbeanstest:toodeeplink")
public class TooDeepLink extends HippoDocument {
    public DeepestLink getContentbeanstest_deepestlink() {
        return getBean("contentbeanstest:contentbeanstest_deepestlink", DeepestLink.class);
    }
}
