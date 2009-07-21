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

package org.hippoecm.hst.plugins.frontend.editor.dialogs;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSitemapItemPickerDialog extends HstPickerDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: HstContentPickerDialog.java 18922 2009-07-21 12:27:50Z abogaart $";

    static final Logger log = LoggerFactory.getLogger(HstSitemapItemPickerDialog.class);

    public HstSitemapItemPickerDialog(final IPluginContext context, IPluginConfig config, IModel model,
            List<String> nodetypes) {
        super(context, new HstPickerConfig(config) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getPath() {
                HstContext hc = context.getService(HstContext.class.getName(), HstContext.class);
                return hc.sitemap.getPath();
            }

        }, model, nodetypes);
    }

    @Override
    protected boolean isValidSelection(Node node) throws RepositoryException {
        return node.isNodeType("hst:sitemapitem");
    }
    
    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=207,height=399");
    }
}
