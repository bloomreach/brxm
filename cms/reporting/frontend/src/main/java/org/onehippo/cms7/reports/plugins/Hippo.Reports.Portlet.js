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

Hippo.Reports.Portlet = Ext.extend(Ext.Panel, {
    anchor : '100%',
    frame : false,
    collapsible : false,
    draggable : false,
    cls: 'hippo-report',
    layout: 'card',
    monitorResize: false,
    activeItem: 0,
    deferredRender: true,

    showMessage: function(msg, bodyCssClass) {
        if (!this.rendered) {
            this.showMessage.defer(10, this, arguments);
            return;
        }
        if (bodyCssClass == null) {
            bodyCssClass = 'hippo-report-message-normal';
        }
        this.add({
            html: msg,
            cls: 'hippo-report-message-wrapper',
            bodyCssClass: bodyCssClass,
            border: false
        });
        this.getLayout().setActiveItem(this.items.getCount() - 1);
    },

    showError: function(msg) {
        this.showMessage(msg, 'hippo-report-message-error');
    }

});

Ext.reg('Hippo.Reports.Portlet', Hippo.Reports.Portlet);


Hippo.Reports.RefreshObservable = Ext.extend(Ext.util.Observable, {
    constructor: function(config){
        this.addEvents({
            "refresh" : true
        });
        Hippo.Reports.RefreshObservable.superclass.constructor.call(this, config)
    }
});
Hippo.Reports.RefreshObservableInstance = new Hippo.Reports.RefreshObservable();

