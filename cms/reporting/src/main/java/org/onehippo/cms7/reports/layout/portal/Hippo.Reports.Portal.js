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
            var yuiLayout = this.getEl().findParent("div.yui-layout-unit");
            YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, this.doLayout, true);
        }, this, {single: true});
    }

});

Ext.reg('Hippo.Reports.Portal', Hippo.Reports.Portal);


Hippo.Reports.PortalColumn = Ext.extend(Ext.Container, {
    layout : 'anchor',
    defaultType : 'Hippo.Reports.Portlet'
});

Ext.reg('Hippo.Reports.PortalColumn', Hippo.Reports.PortalColumn);


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

