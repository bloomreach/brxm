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

import java.util.HashMap;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.IDialogModel;
import org.hippoecm.frontend.plugins.xinha.dialog.XinhaDialogBehavior;
import org.hippoecm.frontend.plugins.xinha.services.links.ExternalXinhaLink;

public class ExternalLinkBehavior extends XinhaDialogBehavior {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public ExternalLinkBehavior(IPluginContext context, IPluginConfig config, String serviceId) {
        super(context, config, serviceId);
    }
    
    @Override
    protected void configureModal(final ModalWindow modal) {
        super.configureModal(modal);
        modal.setInitialHeight(200);
        modal.setInitialWidth(400);
    }
    
    @Override
    protected String getId() {
        return "externallinks";
    }

    @Override
    protected IDialogModel newDialogModel(HashMap<String, String> p) {
        return new ExternalXinhaLink(p, null);
    }

    @Override
    protected void onOk(IDialogModel model) {
    }

    @Override
    protected void onRemove(IDialogModel model) {
        model.reset();
    }

}
