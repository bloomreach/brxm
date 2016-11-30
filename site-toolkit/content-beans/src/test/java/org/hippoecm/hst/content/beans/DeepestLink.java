package org.hippoecm.hst.content.beans;

import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType = "contentbeanstest:deepestlink")
public class DeepestLink extends HippoDocument {
    public DeeperLink getContentbeanstest_deeperlink() {
        return getBean("contentbeanstest:contentbeanstest_deeperlink", DeeperLink.class);
    }
}
