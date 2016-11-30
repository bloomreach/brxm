package org.hippoecm.hst.content.beans;

import org.hippoecm.hst.content.beans.standard.HippoBean;

@Node(jcrType = "contentbeanstest:subcontentdocument")
public class SubContentDocument extends ContentDocument {

  public HippoBean getActuallystilladocbase() {
    final String item = getProperty("contentbeanstest:actuallystilladocbase");
    if (item == null) {
      return null;
    }
    return getBeanByUUID(item, HippoBean.class);
  }

}
