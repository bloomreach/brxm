/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.richtext;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;

public class RichTextLink implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String name;
    private IDetachable model;

    public RichTextLink(IDetachable model, String name) {
        this.model = model;
        this.name = name;
    }

    public IDetachable getTargetId() {
        return model;
    }

    public String getName() {
        return name;
    }

    public void setName(String nodeName) {
        this.name = nodeName;
    }

}
