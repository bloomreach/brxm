package org.hippoecm.hst.content.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType = "contentbeanstest:contentdocument")
public class ContentDocument extends BaseDocument {
  public String getIntroduction() {
    return getProperty("contentbeanstest:introduction");
  }

  public String getTitle() {
    return getProperty("contentbeanstest:title");
  }

  public HippoHtml getContent() {
    return getHippoHtml("contentbeanstest:content");
  }

  public Calendar getPublicationDate() {
    return getProperty("contentbeanstest:publicationdate");
  }

  public HippoBean getHippo_mirror() {
    return getLinkedBean("contentbeanstest:hippo_mirror", HippoBean.class);
  }
}
