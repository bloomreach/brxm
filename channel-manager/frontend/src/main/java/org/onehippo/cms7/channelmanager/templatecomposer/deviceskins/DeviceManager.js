/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
        this.baseImageUrl = config.baseImageUrl;
        this.defaultDeviceIds = config.defaultDeviceIds;
        this.devices = config.devices;
        this.templateComposer = config.templateComposer;
        this.iframe = Hippo.ChannelManager.TemplateComposer.IFramePanel.Instance;
        Hippo.ChannelManager.DeviceManager.superclass.constructor.call(this, config);
    },

    getChannelId: function() {
        return Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').channelId;
    },

    setChannelDefaults: function(channelId) {
        var channelDevices = this.devices[channelId];
        this.store.filterBy(function(record) {
            return channelDevices.length === 0 || channelDevices.indexOf(record.get('id')) >= 0;
        }, this);
    },

    setDevice: function(selectedDeviceId) {
        var r, imageUrl = Ext.BLANK_IMAGE_URL;
        r = this.findRecord('id', selectedDeviceId);
        this.setValue(r.get('name'));
        if (selectedDeviceId !== 'default') {
            imageUrl = this.baseImageUrl + r.get('relativeImageUrl');
        }
        this.deviceImage.set({
            src: imageUrl
        });
        this.iframe.getEl().set({
            cls: 'x-panel ' + selectedDeviceId + (Ext.isIE8 ? 'IE8' : '')
        });
        this.iframe.doLayout(false,true);
    },

    initComponent: function () {
        Hippo.ChannelManager.DeviceManager.superclass.initComponent.call(this);

        this.on('select', function (combo, record, index) {
            var selectedDeviceId = record.get('id');
            Ext.state.Manager.set(combo.getChannelId() + '_skin', selectedDeviceId);
            combo.setDevice(selectedDeviceId);
        });

        var channelId = this.getChannelId(),
                iFrame, shownDeviceId;

        iFrame = this.iframe.getFrameElement();
        this.deviceImage = iFrame.parent().createChild({
            tag: 'img',
            src: Ext.BLANK_IMAGE_URL
        });

        this.setChannelDefaults(channelId);
        if (this.templateComposer.isPreviewMode()) {
            shownDeviceId = Ext.state.Manager.get(channelId + '_skin', this.defaultDeviceIds[channelId]);
        } else {
            shownDeviceId = 'default';
            this.hide();
        }
        this.setDevice(shownDeviceId);
    }
});