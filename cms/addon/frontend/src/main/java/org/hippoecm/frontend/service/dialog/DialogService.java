/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.service.dialog;

import org.hippoecm.frontend.sa.dialog.DialogWindow;
import org.hippoecm.frontend.sa.core.PluginContext;
import org.hippoecm.frontend.service.IDialogService;

public class DialogService extends DialogWindow implements IDialogService {
    private static final long serialVersionUID = 1L;

    private String wicketId;
    private String serviceId;
    private PluginContext context;
    
    public DialogService() {
        super("id");
    }

    public void init(PluginContext context, String serviceId, String wicketId) {
        this.context = context;
        this.serviceId = serviceId;
        this.wicketId = wicketId;
        context.registerService(this, serviceId);
    }
    
    public void destroy() {
        context.unregisterService(this, serviceId);
    }

    @Override
    public String getId() {
        return wicketId;
    }
}
