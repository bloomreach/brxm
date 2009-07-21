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
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class HstComponentPickerDialog extends HstPickerDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(HstComponentPickerDialog.class);

    public HstComponentPickerDialog(final IPluginContext context, IPluginConfig config, IModel model,
            List<String> nodetypes) {
        super(context, new HstPickerConfig(config) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getPath() {
                HstContext ctx = context.getService(HstContext.class.getName(), HstContext.class);
                return ctx.config.getPath();
            }

        }, model, nodetypes);
    }

    @Override
    protected boolean isValidSelection(Node node) throws RepositoryException {
        return false;
    }
}
