package org.hippoecm.hst.pagemodelapi.common.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType="unittestproject:textpage")
public class TextBean extends HippoDocument {

    public HippoBean getNewsDocument() {
        // hard-coded uuid of news bean to make sure serialized of linked bean also works
        HippoBean beanByUUID = getBeanByUUID("303d40eb-f98c-4d61-84c7-a1ba14b5ceb3", HippoBean.class);
        return beanByUUID;
    }
}
