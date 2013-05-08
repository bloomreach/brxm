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
    defaultDevice: 'default',

    constructor: function (config) {
        this.store = config.deviceStore;
        this.baseImageUrl = config.baseImageUrl;
        this.defaultDeviceIds = config.defaultDeviceIds;
        this.templateComposer = config.templateComposer;
        Hippo.ChannelManager.DeviceManager.superclass.constructor.call(this, config);
    },
    getChannelId: function() {
        return Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').channelId;
    },
    setChannelDefaults: function(channelId, devices) {
        this.store.filterBy(function(record) {
            return devices.length === 0 || devices.indexOf(record.get('id')) >= 0;
        }, this);
    },
    setDevice: function(selectedDeviceId) {
        var r, cmp, iFrame, parent, size, image, css, rootPanel;
        rootPanel = Ext.getCmp('rootPanel');
        r = this.findRecord('id', selectedDeviceId);
        if (!Ext.isEmpty(r)) {
            this.setValue(r.get('name'));
            cmp = Ext.getCmp('Iframe');
            iFrame = cmp.items.items[0].getEl();
            parent = iFrame.parent();
            image = Ext.get('deviceImage');
            css = selectedDeviceId + (Ext.isIE8 ? 'IE8' : '');
            if (selectedDeviceId !== 'default') {
                image.set({
                    src: this.baseImageUrl + r.get('relativeImageUrl')
                });
            }
            cmp.getEl().set({
                cls: 'x-panel ' + css
            });
        }
    },
    initComponent: function () {
        Hippo.ChannelManager.DeviceManager.superclass.initComponent.call(this);
        this.on('select', function (combo, record, index) {
            var selectedDeviceId = record.get('id'),
                    channelId = combo.getChannelId();
            Ext.state.Manager.set(channelId + '_skin', selectedDeviceId);
            combo.setDevice(selectedDeviceId);
        });
        var channelId = this.getChannelId(),
                cmp, iFrame, parent, shownDeviceId;
        cmp = Ext.getCmp('Iframe');
        iFrame = cmp.items.items[0].getEl();
        parent = iFrame.parent();
        parent.createChild({
            id: 'deviceImage',
            tag: 'img',
            src: Ext.BLANK_IMAGE_URL
        });
        this.addEvents('setchanneldefaults');
        this.fireEvent('setchanneldefaults', channelId);
        if (this.templateComposer.isPreviewMode()) {
            shownDeviceId = Ext.state.Manager.get(channelId + '_skin', this.defaultDeviceIds[channelId]);
        } else {
            shownDeviceId = 'default';
            this.hide();
        }
        this.setDevice(shownDeviceId);
    }
});