/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType = "contentbeanstest:contentdocument")
public class ContentDocument extends BaseDocument {
  public String getIntroduction() {
    return getSingleProperty("contentbeanstest:introduction");
  }

  public String getTitle() {
    return getSingleProperty("contentbeanstest:title");
  }

  public HippoHtml getContent() {
    return getHippoHtml("contentbeanstest:content");
  }

  public Calendar getPublicationDate() {
    return getSingleProperty("contentbeanstest:publicationdate");
  }

  public HippoBean getHippo_mirror() {
    return getLinkedBean("contentbeanstest:hippo_mirror", HippoBean.class);
  }
}
