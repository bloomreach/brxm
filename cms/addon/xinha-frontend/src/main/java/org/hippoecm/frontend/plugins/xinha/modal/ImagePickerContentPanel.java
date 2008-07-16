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
package org.hippoecm.frontend.plugins.xinha.modal;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

public class ImagePickerContentPanel extends XinhaContentPanel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public ImagePickerContentPanel(XinhaModalWindow modal, Map<String, String> parameters) {
        super(modal, parameters);

        form.add(new TextField("url", new XinhaImageModel(parameters, XinhaImageProperty.URL)));
    }

    @Override
    protected String getSelectedValue() {
        return MapToJavascriptObject(parameters);
    }

    private class XinhaImageModel implements IModel {
        private static final long serialVersionUID = 1L;

        private Map<String, String> values;
        private XinhaImageProperty p;

        public XinhaImageModel(Map<String, String> values, XinhaImageProperty p) {
            this.values = (values != null) ? values : new HashMap<String, String>();
            this.p = p;
        }

        public Object getObject() {
            return values.get(p.getValue());
        }

        public void setObject(Object object) {
            if (object == null)
                return;
            values.put(p.getValue(), (String) object);
        }

        public void detach() {
        }

    }

}
