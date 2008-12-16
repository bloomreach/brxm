/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.xinha.dialog.images;

import java.util.Map;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.dialog.JsBean;

public class XinhaImage extends JsBean {
    private static final long serialVersionUID = 1L;

    final static String SVN_ID = "$Id$";

    public static final String BASE = "f_base";
    public static final String URL = "f_url";
    public static final String ALT = "f_alt";
    public static final String BORDER = "f_border";
    public static final String ALIGN = "f_align";
    public static final String VERTICAL_SPACE = "f_vert";
    public static final String HORIZONTAL_SPACE = "f_horiz";
    public static final String WIDTH = "f_width";
    public static final String HEIGHT = "f_height";

    private JcrNodeModel nodeModel;

    public XinhaImage(Map<String, String> values) {
        super(values);
    }

    public JcrNodeModel getNodeModel() {
        return nodeModel;
    }

    public void setNodeModel(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    public void setUrl(String url) {
        values.put(URL, url);
    }

    public String getUrl() {
        return values.get(URL);
    }

}
