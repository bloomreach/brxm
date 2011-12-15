/**
 * Copyright 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";

Ext.QuickTips.init();
Ext.namespace('Hippo.ChannelManager');

/**
 * @class Hippo.ChannelManager.ChannelFormPanel
 * @extends Ext.form.FormPanel
 */
Hippo.ChannelManager.ChannelFormPanel = Ext.extend(Ext.form.FormPanel, {

    constructor: function(config) {
        this.store = config.store;
        this.resources = config.resources;
        Hippo.ChannelManager.ChannelFormPanel.superclass.constructor.call(this, config);
    },

    initComponent: function() {
        var me = this;
        var config = {
            padding: 5,
            url: me.store.url,
            defaults: {
                labelAlign: 'top',
                width: 450,
                labelWidth: 100
            },
            items:[
                {
                    xtype: 'displayfield',
                    fieldLabel: me.resources['new-channel-field-blueprint'],
                    id: 'displayedBlueprintId',
                    style: {
                        textAlign: 'left',
                        paddingLeft: '70px'
                    }
                },
                {
                    xtype: 'hidden',
                    id: 'blueprintId'
                },
                {
                    xtype: 'textfield',
                    fieldLabel: me.resources['new-channel-field-name'],
                    id: 'name',
                    allowBlank: false
                },
                {
                    xtype: 'textfield',
                    fieldLabel:  me.resources['new-channel-field-url'],
                    id: 'url',
                    allowBlank: false,
                    validator: function(value) {
                        var expr = /(^(http|https):\/\/([\-\w]+\.)*\w(\/[%\-\w]+(\.\w{2,})?)*)/i;
                        if (expr.test(value) === true) {
                            return true;
                        } else {
                            return me.resources['error-new-channel-url-format'];
                        }
                    }
                },
                {
                    xtype: 'linkpicker',
                    fieldLabel: me.resources['new-channel-field-content'],
                    id: 'contentRoot',
                    style: {
                        marginLeft: '69px'
                    },
                    pickerConfig: {
                        configuration: "cms-pickers/folders",
                        selectableNodeTypes: "hippostd:folder"
                    }
                }
            ]
        };

        Ext.apply(this, Ext.apply(this.initialConfig, config));
        Hippo.ChannelManager.ChannelFormPanel.superclass.initComponent.apply(this, arguments);

        this.addEvents('channel-created');

        this.on('beforeshow', function () {
            this.getForm().reset();

            var blueprint = Ext.getCmp('blueprints-panel').getSelectionModel().getSelected();
            Ext.getCmp('displayedBlueprintId').setValue(blueprint.id);
            Ext.getCmp('blueprintId').setValue(blueprint.id);

            var contentRootCmp = Ext.getCmp('contentRoot');
            if (blueprint.get('hasContentPrototype')) {
                contentRootCmp.hide();
            } else {
                contentRootCmp.setDefaultValue(blueprint.get('contentRoot'));
                contentRootCmp.show();
            }

            this.getComponent('name').focus(false, 10);
        }, this);

        this.doLayout();
    },

    submitForm: function() {
        var form = this.getForm();
        var panel = this;
        if (form.isValid()) {
            form.submit({
                params: {
                    xaction: 'create',
                    records: Ext.encode(form.getValues())
                },

                success: function(form, action) {
                    //Event to hide the window & rerender the channels panel
                    panel.fireEvent('channel-created');
                },
                failure: function(form, action) {
                    switch (action.failureType) {
                        case Ext.form.Action.CLIENT_INVALID:
                            Ext.Msg.alert("Error", "Please check the highlighted fields");
                            break;
                        case Ext.form.Action.CONNECT_FAILURE:
                            Ext.Msg.alert("Error", "Unable to connect to the server");
                            break;
                        case Ext.form.Action.SERVER_INVALID:
                            Ext.Msg.alert("Error", "Error while creating the Channel " + action.result.msg);
                            break;
                    }
                }
            });
        }
    }
});

Ext.reg('Hippo.ChannelManager.ChannelFormPanel', Hippo.ChannelManager.ChannelFormPanel);
