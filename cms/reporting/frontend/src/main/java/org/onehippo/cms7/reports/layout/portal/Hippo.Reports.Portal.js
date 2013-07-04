/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

Ext.ns('Hippo.Reports');
YAHOO.namespace('hippo');

Hippo.Reports.Portal = Ext.extend(Ext.Panel, {
    layout : 'column',
    monitorResize: false,
    defaultType : 'Hippo.Reports.PortalColumn',
    hideBorders: false,
    border: false,

    initComponent: function() {
        Hippo.Reports.Portal.superclass.initComponent.call(this);

        // recalculate the ExtJs layout when the YUI layout manager fires a resize event
        this.on('afterlayout', function(portal, layout) {
            YAHOO.hippo.LayoutManager.registerResizeListener(this.getEl().dom, this, this.doLayout, true);
        }, this, {single: true});
    }

});

Ext.reg('Hippo.Reports.Portal', Hippo.Reports.Portal);


Hippo.Reports.PortalColumn = Ext.extend(Ext.Container, {
    layout : 'anchor',
    defaultType : 'Hippo.Reports.Portlet'
});

Ext.reg('Hippo.Reports.PortalColumn', Hippo.Reports.PortalColumn);

