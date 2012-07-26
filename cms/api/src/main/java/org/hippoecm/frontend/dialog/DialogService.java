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
package org.hippoecm.frontend.dialog;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.behavior.IBehavior;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.service.ServiceTracker;

public class DialogService extends DialogWindow {

    private static final long serialVersionUID = 1L;

    private String wicketId;
    private String serviceId;
    private IPluginContext context;
    private IServiceTracker<IBehavior> tracker;
    private List<IBehavior> dialogBehaviors;

    public DialogService() {
        super("id");
        dialogBehaviors = new ArrayList<IBehavior>();
    }

    public void init(IPluginContext context, String serviceId, String wicketId) {
        this.context = context;
        this.serviceId = serviceId;
        this.wicketId = wicketId;
        context.registerService(this, serviceId);
        context.registerTracker(tracker = new ServiceTracker<IBehavior>(IBehavior.class) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onServiceAdded(IBehavior service, String name) {
                dialogBehaviors.add(service);
            }

            @Override
            protected void onRemoveService(IBehavior service, String name) {
                dialogBehaviors.remove((IBehavior) service);
            }
        }, serviceId);
    }

    public void destroy() {
        context.unregisterService(this, serviceId);
        context.unregisterTracker(tracker, serviceId);
    }

    @Override
    public String getId() {
        return wicketId;
    }

}
