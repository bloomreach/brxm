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
package org.hippoecm.frontend.plugins.console.editor;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.resource.JcrResource;
import org.hippoecm.frontend.resource.JcrResourceStream;

class ResourceEditor extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private static final long ONE_KB = 1024;
    private static final long ONE_MB = ONE_KB * ONE_KB;
    private static final long ONE_GB = ONE_KB * ONE_MB;

    ResourceEditor(String id, JcrNodeModel nodeModel) {
        super(id);
        JcrResourceStream stream = new JcrResourceStream(nodeModel);
        ResourceLink link = new ResourceLink("resource-link", new JcrResource(stream));
        add(link);

        String size;
        long length = stream.length();
        if (length / ONE_GB > 0) {
            size = String.valueOf(length / ONE_GB) + " GB";
        } else if (length / ONE_MB > 0) {
            size = String.valueOf(length / ONE_MB) + " MB";
        } else if (length / ONE_KB > 0) {
            size = String.valueOf(length / ONE_KB) + " KB";
        } else {
            size = String.valueOf(length) + " bytes";
        }
        link.add(new Label("resource-link-text", "download (" + size + ")"));

    }

}
