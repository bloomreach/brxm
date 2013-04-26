Ext.namespace('Hippo.ChannelManager');

Hippo.ChannelManager.DeviceManager = Ext.extend(Ext.form.ComboBox, {
    id: 'deviceManager',
    displayField: 'name',
    //typeAhead: true,
    mode: 'local',
    forceSelection: true,
    triggerAction: 'all',
    lastQuery: '',
    emptyText: 'Choose a device...',
    selectOnFocus: true,
    def : 'default',
    ie8 : 'IE8 is not supported!',
    editable: false,
    disableKeyFilter: true,
    listeners: {
        beforequery: function(queryEvent){
            // delete the previous query so the store will reload the next time it expands
            delete queryEvent.combo.lastQuery;
        }
    },
    defaultDevice: 'default',

    constructor: function (config) {
        if (!Ext.isIE8) {
            this.store = config.deviceStore;
            var cmp = Ext.getCmp('Iframe');
            cmp.getEl().set({
                cls: 'x-panel default'
            });
        } else {
            this.emptyText = this.ie8;
        }
        Hippo.ChannelManager.DeviceManager.superclass.constructor.call(this, config);
    },
    getChannelId: function() {
        return Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').channelId;
    },
    setChannelDefaults: function(channelId, devices, defaultDevice) {
        this.store.filterBy(function(record) {
            return devices.length === 0 || devices.indexOf(record.get('id')) >= 0;
        }, this);
        var selectedDeviceId = Ext.state.Manager.get(channelId + '_skin', defaultDevice);
        this.setDevice(selectedDeviceId);
    },
    setDevice: function(selectedDeviceId) {
        var r, cmp, iFrame, parent, size;
        r = this.findRecord('id', selectedDeviceId);
        if (!Ext.isEmpty(r)) {
            this.setValue(r.get('name'));
            cmp = Ext.getCmp('Iframe');
            iFrame = cmp.items.items[0].getEl();
            parent = iFrame.parent();

            cmp.getEl().set({
                cls: 'x-panel ' + selectedDeviceId
            });

            iFrame.dom.removeAttribute('style');  //clear style
            parent.dom.removeAttribute('style');

            //while selecting default, set the iframe and iframe partent size same as the rootpanel size.
            if (selectedDeviceId === 'default') {
                size = Ext.getCmp('rootPanel').getSize();
                iFrame.setSize(size);
                parent.setSize(size);
            }
        }
    },
    initComponent: function () {
        Hippo.ChannelManager.DeviceManager.superclass.initComponent.call(this);
        if (!Ext.isIE8) {
            this.on('select', function (combo, record, index) {
                var selectedDeviceId = record.get('id'),
                        channelId = combo.getChannelId();
                Ext.state.Manager.set(channelId + '_skin', selectedDeviceId);
                combo.setDevice(selectedDeviceId);
            });
            var channelId = this.getChannelId();
            this.addEvents('setchanneldefaults');
            this.fireEvent('setchanneldefaults', channelId);
        }
    }
});