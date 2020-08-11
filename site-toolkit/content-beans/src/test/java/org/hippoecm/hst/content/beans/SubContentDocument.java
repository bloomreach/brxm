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

import org.hippoecm.hst.content.beans.standard.HippoBean;

@Node(jcrType = "contentbeanstest:subcontentdocument")
public class SubContentDocument extends ContentDocument {

  public HippoBean getActuallystilladocbase() {
    final String item = getSingleProperty("contentbeanstest:actuallystilladocbase");
    if (item == null) {
      return null;
    }
    return getBeanByUUID(item, HippoBean.class);
  }

}
