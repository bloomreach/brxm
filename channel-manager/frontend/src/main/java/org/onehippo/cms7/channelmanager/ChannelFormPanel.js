/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
(function() {
    "use strict";

    Ext.QuickTips.init();
    Ext.namespace('Hippo.ChannelManager');

    /**
     * @class Hippo.ChannelManager.ChannelFormPanel
     * @extends Ext.form.FormPanel
     */
    Hippo.ChannelManager.ChannelFormPanel = Ext.extend(Ext.form.FormPanel, {

        // The name and URL of a channel should not end in "-preview" because
        // the HST uses this suffix internally to distinguish live and preview sites
        ILLEGAL_CHANNEL_SUFFIX: "-preview",

        constructor: function(config) {
            this.store = config.store;
            this.resources = config.resources;
            Hippo.ChannelManager.ChannelFormPanel.superclass.constructor.call(this, config);
        },

        initComponent: function() {
            var me, config;
            me = this;
            config = {
                padding: 5,
                url: me.store.proxy.url,
                defaults: {
                    labelAlign: 'top',
                    width: 450,
                    labelWidth: 100
                },
                plugins: Hippo.ChannelManager.MarkRequiredFields,
                items: [
                    {
                        xtype: 'panel',
                        id: 'errorMessage',
                        width: '100%',
                        baseCls: 'x-form-invalid',
                        padding: 10,
                        style: 'margin-bottom: 10px',
                        hidden: true
                    },
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
                        allowBlank: false,
                        validator: function(value) {
                            return !me.endsWith(value, me.ILLEGAL_CHANNEL_SUFFIX);
                        }
                    },
                    {
                        xtype: 'textfield',
                        fieldLabel: me.resources['new-channel-field-url'],
                        id: 'url',
                        allowBlank: false,
                        validator: function(value) {
                            var expr = /^https?:\/\/([\-\w]+\.)*\w+(\/[\-\w]+)*$/i;
                            if (expr.test(value) === true) {
                                return !me.endsWith(value, me.ILLEGAL_CHANNEL_SUFFIX);
                            } else {
                                return me.resources['error-new-channel-url-format'];
                            }
                        }
                    },
                    {
                        xtype: 'linkpicker',
                        fieldLabel: me.resources['new-channel-field-content'],
                        id: 'contentRoot',
                        hideMode: 'visibility',
                        style: {
                            marginLeft: '69px'
                        },
                        pickerConfig: {
                            configuration: 'cms-pickers/folders',
                            selectableNodeTypes: ['hippostd:folder']
                        }
                    }
                ]
            };

            Ext.apply(this, Ext.apply(this.initialConfig, config));
            Hippo.ChannelManager.ChannelFormPanel.superclass.initComponent.apply(this, arguments);

            this.addEvents('channel-created');

            this.on('beforeshow', function() {
                var blueprint, contentRoot, contentRootCmp;
                this.getForm().reset();
                this.hideError();

                blueprint = Ext.getCmp('blueprints-panel').getSelectionModel().getSelected();
                Ext.getCmp('displayedBlueprintId').setValue(blueprint.get('name'));
                Ext.getCmp('blueprintId').setValue(blueprint.id);

                contentRootCmp = Ext.getCmp('contentRoot');
                if (blueprint.get('hasContentPrototype')) {
                    contentRootCmp.hide();
                } else {
                    contentRoot = blueprint.get('contentRoot');
                    contentRootCmp.setDefaultValue(contentRoot);
                    contentRootCmp.setValue(contentRoot);
                    contentRootCmp.show();
                }

                this.getComponent('name').focus(false, 10);
            }, this);

            this.doLayout();
        },

        // checks whether str ends with suffix
        endsWith: function(str, suffix) {
            return str !== null && suffix !== null && str.indexOf(suffix, str.length - suffix.length) !== -1;
        },

        submitForm: function(enableNextButtonCallback) {
            var form, panel;
            form = this.getForm();
            panel = this;
            if (form.isValid()) {
                form.submit({
                    params: {
                        xaction: 'create',
                        records: Ext.encode(form.getValues())
                    },
                    success: function() {
                        //Event to hide the window & rerender the channels panel
                        panel.fireEvent('channel-created');
                    },
                    failure: function(form, action) {
                        switch (action.failureType) {
                            case Ext.form.Action.CLIENT_INVALID:
                                panel.showError(panel.resources['error-check-highlighted-fields']);
                                break;
                            case Ext.form.Action.CONNECT_FAILURE:
                                panel.showError(panel.resources['error-cannot-connect-to-server']);
                                break;
                            case Ext.form.Action.SERVER_INVALID:
                                panel.showError(action.result.message);
                                break;
                        }
                        enableNextButtonCallback(true);
                    }
                });
            }
        },

        showError: function(message) {
            var panel = this.getComponent('errorMessage');
            panel.update(message);
            panel.show();
        },

        hideError: function() {
            this.getComponent('errorMessage').hide();
        }

    });

    Ext.reg('Hippo.ChannelManager.ChannelFormPanel', Hippo.ChannelManager.ChannelFormPanel);

}());