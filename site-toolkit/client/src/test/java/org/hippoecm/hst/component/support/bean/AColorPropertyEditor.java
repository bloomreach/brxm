/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.component.support.bean;

import java.awt.Color;
import java.beans.PropertyEditorSupport;

/**
 * AColorPropertyEditor
 * @version $Id$
 */
public class AColorPropertyEditor extends PropertyEditorSupport {
    
    @Override
    public String getAsText() {
        Color color = (Color) getValue();
        return "#" + Integer.toHexString(color.getRGB() & 0x00ffffff);
    }
    
    @Override
    public void setAsText(String value) {
        Color color = new Color(Integer.parseInt(value.substring(1), 16) & 0x00ffffff);
        setValue(color);
    }
    
}
