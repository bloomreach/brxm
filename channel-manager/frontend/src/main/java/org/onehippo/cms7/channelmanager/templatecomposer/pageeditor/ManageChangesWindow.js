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
(function() {
    "use strict";

    Ext.namespace('Hippo.ChannelManager.TemplateComposer');

    function deleteRecordsFromStore(store, ids) {
        Ext.each(ids, function(id) {
            var record = store.getById(id);
            store.remove(record);
        });
    }

    Hippo.ChannelManager.TemplateComposer.ManageChangesWindow = Ext.extend(Ext.Window, {

        constructor: function(config) {
            var ManageChangesProxy, closeButton, grid;

            this.resources = config.resources;
            this.onSuccess = config.onSuccess;
            this.onFailure = config.onFailure;

            ManageChangesProxy = Ext.extend(Ext.data.HttpProxy, {
                buildUrl: function() {
                    var url = ManageChangesProxy.superclass.buildUrl.apply(this, arguments);
                    url = Ext.urlAppend(url, 'FORCE_CLIENT_HOST=true');
                    return url;
                }
            });

            this.apiBaseUrl = config.composerRestMountUrl + '/' + config.mountId + './userswithchanges';

            this.storeProxy = new ManageChangesProxy({
                api: {
                    read: this.apiBaseUrl,
                    create: '#',
                    update: '#',
                    destroy: '#'
                }
            });

            this.store = new Hippo.ChannelManager.TemplateComposer.RestStore({
                autoSave: false,  // only send records to the server when calling save()
                listful: true,    // always write records as an array, even if there's only one
                restful: false,   // do not use a separate Ajax call per modified record
                root: 'data',
                prototypeRecord: [
                    { name: 'id' }
                ],
                proxy: this.storeProxy,
                sortInfo: {
                    field: 'id',
                    direction: 'ASC'
                },
                listeners: {
                    save: {
                        fn: this.onChangesSucceeded,
                        scope: this
                    },
                    exception: {
                        fn: this.onChangesFailed,
                        scope: this
                    }
                }
            });

            this.publishButton = new Ext.Toolbar.Button({
                text: this.resources['manage-changes-publish-button'],
                iconCls: 'publish-changes',
                disabled: true,
                tooltip: this.resources['manage-changes-disabled-button-tooltip'],
                listeners: {
                    click: {
                        fn: this.publishChanges,
                        scope: this
                    }
                }
            });

            this.discardButton = new Ext.Toolbar.Button({
                text: this.resources['manage-changes-discard-button'],
                iconCls: 'discard-changes',
                disabled: true,
                tooltip: this.resources['manage-changes-disabled-button-tooltip'],
                listeners: {
                    click: {
                        fn: this.discardChanges,
                        scope: this
                    }
                }
            });

            closeButton = new Ext.Toolbar.Button({
                text: this.resources['manage-changes-close-button'],
                listeners: {
                    click: {
                        fn: this.close,
                        scope: this
                    }
                }
            });

            this.selectionModel = new Ext.grid.CheckboxSelectionModel({
                listeners: {
                    selectionchange: {
                        fn: this.onSelectionChanged,
                        scope: this
                    }
                }
            });

            grid = new Ext.grid.GridPanel({
                cm: new Ext.grid.ColumnModel([
                    this.selectionModel,
                    {
                        dataIndex: 'id',
                        header: this.resources['manage-changes-list-header-users'],
                        renderer: function(value) {
                            if (value === config.cmsUser) {
                                value += config.resources['manage-changes-list-user-cms-suffix'];
                            }
                            return value;
                        }
                    }
                ]),
                enableHdMenu: false,
                enableColumnHide: false,
                enableColumnMove: false,
                enableColumnResize: false,
                sm: this.selectionModel,
                store: this.store,
                viewConfig: {
                    emptyText: this.resources['manage-changes-list-empty'],
                    forceFit: true,
                    hideSortIcons: true,
                    scrollOffset: 0,
                    sortClasses: []
                }
            });

            Ext.apply(config, {
                title: this.resources['manage-changes-title'],
                cls: 'channel-manager-window-manage-changes',
                width: 400,
                height: 300,
                modal: true,
                padding: 10,
                layout: 'vbox',
                layoutConfig: {
                    align: 'stretch'
                },
                items: [
                    {
                        xtype: 'label',
                        text: Ext.util.Format.htmlEncode(this.resources['manage-changes-explanation']),
                        margins: '0 0 8px 0'
                    },
                    Ext.apply(grid, {
                        flex: 1
                    })
                ],
                listeners: {
                    beforeshow: {
                        fn: this.onBeforeShow,
                        scope: this
                    }
                },
                buttonAlign: 'left',
                buttons: [
                    this.publishButton,
                    this.discardButton,
                    { xtype: 'tbfill' },
                    closeButton
                ]
            });

            Hippo.ChannelManager.TemplateComposer.ManageChangesWindow.superclass.constructor.apply(this, arguments);
        },

        onBeforeShow: function() {
            this.store.load();
        },

        onSelectionChanged: function() {
            var noUsersSelected = this.selectionModel.getCount() === 0,
                buttonsTooltip = noUsersSelected ? this.resources['manage-changes-disabled-button-tooltip'] : '';

            Ext.each([ this.publishButton, this.discardButton ], function(button) {
                button.setDisabled(noUsersSelected);
                button.setTooltip(buttonsTooltip);
            });
        },

        getSelectedUserIds: function() {
            var result = [];
            Ext.each(this.selectionModel.getSelections(), function(record) {
                result.push(record.get('id'));
            });
            return result;
        },

        publishChanges: function() {
            this.confirmChanges('publish');
        },

        discardChanges: function() {
            this.confirmChanges('discard');
        },

        confirmChanges: function(action) {
            var confirmationTitle = this.resources['manage-changes-confirm-' + action + '-title'],
                selectedUserIds = this.getSelectedUserIds().sort(),
                self = this;

            Hippo.Msg.confirm(confirmationTitle, this.createConfirmationMessage(action, selectedUserIds),
                function(choice) {
                    if (choice === 'yes') {
                        self.processChanges(action, selectedUserIds);
                    }
                }
            );
        },

        createConfirmationMessage: function(action, userIds) {
            switch (userIds.length) {
                case 1:
                    return String.format(this.resources['manage-changes-confirm-' + action + '-message-one-user'], userIds[0]);
                case 2:
                    return String.format(this.resources['manage-changes-confirm-' + action + '-message-two-users'], userIds[0], userIds[1]);
                default:
                    return String.format(this.resources['manage-changes-confirm-' + action + '-message-comma-separated-users-and-last-one'],
                        userIds.slice(0, userIds.length - 1).join(', '), userIds[userIds.length - 1]);
            }
        },

        processChanges: function(action, userIds) {
            this.storeProxy.setApi(Ext.data.Api.actions.destroy, this.apiBaseUrl + '/' + action);
            deleteRecordsFromStore(this.store, userIds);
            this.store.save();
        },

        onChangesSucceeded: function() {
            this.onSuccess();
            this.close();
        },

        onChangesFailed: function() {
            this.onFailure();
            this.close();
        }

    });

    Ext.reg('Hippo.ChannelManager.TemplateComposer.ManageChangesWindow', Hippo.ChannelManager.TemplateComposer.ManageChangesWindow);

}());
