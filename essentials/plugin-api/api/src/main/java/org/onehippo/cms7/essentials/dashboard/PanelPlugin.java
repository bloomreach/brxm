/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard;

import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

/**
 * @version "$Id$"
 */
public abstract class PanelPlugin extends Panel {


    private static final long serialVersionUID = 1L;
    private final PluginContext context;

    protected PanelPlugin(final String id, final PluginContext context) {
        super(id);
        this.context = context;
    }

    public PluginContext getContext() {
        return context;
    }

    /**
     * Logout  all JCR sessions
     * <p> <strong>NOTE:</strong> no save or session refresh is called, only {@code session.logout()} is callled</p>
     */
    protected void onRemove() {

        // cleanup connections:
        final Session session = context.getSession();
        if (session != null) {
            session.logout();
        }


    }

}
