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

package org.hippoecm.frontend.plugins.xinha.dialog.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.XinhaDialogBehavior;
import org.hippoecm.frontend.plugins.xinha.services.links.ExternalXinhaLink;

public class ExternalLinkBehavior extends XinhaDialogBehavior {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public ExternalLinkBehavior(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        IModel<ExternalXinhaLink> model = new Model<ExternalXinhaLink>(new ExternalXinhaLink(getParameters()));
        getDialogService().show(new ExternalLinkDialog(getPluginContext(), getPluginConfig(), model));
    }

}
