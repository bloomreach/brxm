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

package org.hippoecm.frontend.plugins.cms.browse.tree.yui;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public abstract class WicketTreeHelperBehavior extends AbstractYuiBehavior {
    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$"; 
  
    WicketTreeHelperSettings settings;
    
    public WicketTreeHelperBehavior(IYuiManager manager, WicketTreeHelperSettings settings) {
        super(manager);
        this.settings = settings;
    }
    
    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(YuiTreeNamespace.NS, "treehelper");
        context.addOnWinLoad(new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;
            
            @Override
            public Object getObject() {
                return getInitString();
            }
        });
    }
    public String getInitString() {
        return "YAHOO.hippo.TreeHelper.init(" + settings.toScript() + ");";
    }

    public String getRenderString() {
        return "YAHOO.hippo.TreeHelper.render('" + getWicketId() + "');";
    }

    public String getUpdateString() {
        return "YAHOO.hippo.TreeHelper.updateMouseListeners('" + getWicketId() + "');";
    }
    
    protected abstract String getWicketId();

}
