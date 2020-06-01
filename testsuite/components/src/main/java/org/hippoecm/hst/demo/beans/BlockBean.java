/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType = "demosite:contentblockcompound")
public class BlockBean extends HippoCompound {
    private String header;
    private String image;

    public String getHeader() {
        return header == null ? (String) getProperty("demosite:header") : header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getImage() {
        return image == null ? (String) getProperty("demosite:country") : image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public HippoHtml getBody() {
        return getHippoHtml("demosite:body");
    }
}
