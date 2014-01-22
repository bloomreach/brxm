/*
 *  Copyright 2013-2014 Hippo B.V. (http://www.onehippo.com)
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
Ext.namespace('Hippo.ChannelManager');

Hippo.ChannelManager.DeviceManager = Ext.extend(Ext.form.ComboBox, {
    id: 'deviceManager',
    displayField: 'name',
    mode: 'local',
    forceSelection: true,
    triggerAction: 'all',
    lastQuery: '',
    selectOnFocus: true,
    editable: false,
    disableKeyFilter: true,
    listeners: {
        beforequery: function(queryEvent){
            // delete the previous query so the store will reload the next time it expands
            delete queryEvent.combo.lastQuery;
        }
    },

    constructor: function (config) {
        this.store = config.deviceStore;
        this.templateComposer = config.templateComposer;
        this.iframe = Ext.getCmp('pageEditorIFrame');
        if (!Hippo.ChannelManager.DeviceManager.deviceImage) {
            Hippo.ChannelManager.DeviceManager.deviceImage = this.iframe.getFrameElement().parent().createChild({
                tag: 'img',
                src: Ext.BLANK_IMAGE_URL
            });
        }
        this.channel = config.channel;

        Hippo.ChannelManager.DeviceManager.superclass.constructor.call(this, config);
    },

    initComponent: function () {
        Hippo.ChannelManager.DeviceManager.superclass.initComponent.call(this);

        this.on('select', function (combo, record, index) {
            var selectedDeviceId = record.get('id');
            Ext.state.Manager.set(combo.channel.id + '_skin', selectedDeviceId);
            combo.setDevice(record);
        });

        var deviceRecord;
        if (this.templateComposer.isPreviewMode()) {
            this.updateDevices(true);
            deviceRecord = this.getDeviceRecord(Ext.state.Manager.get(this.channel.id + '_skin'));
        } else {
            this.updateDevices(false);
            deviceRecord = this.getDeviceRecord('desktop');
            this.hide();
        }
        this.setDevice(deviceRecord);
    },

    updateDevices: function(filter) {
        var channelDevices = this.channel.devices;
        this.store.filterBy(function(record) {
            if (filter && channelDevices && channelDevices.length > 0) {
                return channelDevices.indexOf(record.get('id')) >= 0;
            } else {
                return true;
            }
        }, this);
    },

    setDevice: function(deviceRecord) {
        var selectedDeviceId = deviceRecord.get('id'), imageUrl = Ext.BLANK_IMAGE_URL;

        this.setValue(deviceRecord.get('name'));
        if (selectedDeviceId !== 'desktop') {
            imageUrl = deviceRecord.get('imageUrl');
        }
        Hippo.ChannelManager.DeviceManager.deviceImage.set({
            src: imageUrl
        });
        this.iframe.getEl().set({
            cls: 'x-panel ' + selectedDeviceId + (Ext.isIE8 ? 'IE8' : '')
        });
        this.iframe.doLayout(false,true);
    },

    /**
     * Look up record for the device, falling back to default devices when it cannot be found.
     *
     * @param selectedDeviceId
     * @returns {*}
     */
    getDeviceRecord: function(selectedDeviceId) {
        var record, order = [ selectedDeviceId, this.channel.defaultDeviceId, 'desktop' ];

        Ext.each(order, function (device) {
            if (device) {
                record = this.findRecord('id', device);
                if (record) {
                    return false;
                }
            }
        }, this);
        if (record) {
            return record;
        }
        return this.store.getAt(0);
    }

});