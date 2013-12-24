/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

    /**
     * Retrieves all variants for the component specified by the config property 'componentId'.
     * The REST endpoint for retrieving the global variants is determined by the config property 'variantsUuid'.
     * When 'variantsUuid' is empty or undefined, a single variant with the ID 'hippo-default' will always be returned.
     *
     * The retrieved variants will be cached. Calling 'invalidate()' will invalidate the cache and fire the
     * 'invalidated' event. The next call to 'get()' will then retrieve the component variants again.
     *
     * @type {*}
     */
    Hippo.ChannelManager.TemplateComposer.ComponentVariants = Ext.extend(Ext.util.Observable, {

        variantsFuture: null,

        constructor: function(config) {
            this.variantsUuid = config.variantsUuid;
            this.componentId = config.componentId;
            this.lastModifiedTimestamp = config.lastModifiedTimestamp;
            this.composerRestMountUrl = config.composerRestMountUrl;
            this.resources = config.resources;
            this.locale = config.locale;

            Hippo.ChannelManager.TemplateComposer.ComponentVariants.superclass.constructor.call(this, config);

            this.addEvents('invalidated');
        },

        isMultivariate: function() {
            return !Ext.isEmpty(this.variantsUuid);
        },

        get: function() {
            if (this.variantsFuture === null) {
                this.variantsFuture = this._loadVariants();
            }
            return this.variantsFuture;
        },

        invalidate: function() {
            this.variantsFuture = null;
            this.fireEvent('invalidated');
        },

        cleanup: function() {
            var cleanupFuture = this.isMultivariate() ? this._cleanupVariants() : Hippo.Future.constant();
            return cleanupFuture;
        },

        _loadVariants: function() {
            if (!this.isMultivariate()) {
                return Hippo.Future.constant([
                    {id: 'hippo-default', name: this.resources['properties-panel-variant-default']}
                ]);
            } else {
                var self = this;
                return new Hippo.Future(function(success, fail) {
                    if (self.componentId) {
                        Ext.Ajax.request({
                            url: self.composerRestMountUrl + '/' + self.componentId + './?FORCE_CLIENT_HOST=true',
                            success: function(result) {
                                var jsonData = Ext.util.JSON.decode(result.responseText),
                                    variantIds = jsonData.data;

                                self._loadComponentVariants(variantIds).when(function(variants) {
                                    variants.push({id: 'plus', name: self.resources['properties-panel-variant-plus']});
                                    success(variants);
                                }).otherwise(function(response) {
                                    fail(response);
                                });
                            },
                            failure: function(result) {
                                fail(result.response);
                            }
                        });
                    } else {
                        success([
                            {id: 'hippo-default', name: self.resources['properties-panel-variant-default']}
                        ]);
                    }
                });
            }
        },

        _loadComponentVariants: function(variantIds) {
            return new Hippo.Future(function(success, fail) {
                Ext.Ajax.request({
                    url: this.composerRestMountUrl + '/' + this.variantsUuid + './componentvariants?locale=' + this.locale + '&FORCE_CLIENT_HOST=true',
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    params: Ext.util.JSON.encode(variantIds),
                    success: function(result) {
                        var jsonData = Ext.util.JSON.decode(result.responseText),
                            variants = jsonData.data;
                        success(variants);
                    },
                    failure: function(result) {
                        fail(result.response);
                    },
                    scope: this
                });
            }.createDelegate(this));
        },

        _cleanupVariants: function() {
            return new Hippo.Future(function(success) {
                this.get().when(function(variants) {
                    var variantIds = [];
                    Ext.each(variants, function(variant) {
                        if (variant.id !== 'hippo-default' && variant.id !== 'plus') {
                            variantIds.push(variant.id);
                        }
                    });
                    Ext.Ajax.request({
                        method: 'POST',
                        url: this.composerRestMountUrl + '/' + this.componentId + './' + '?FORCE_CLIENT_HOST=true',
                        headers: {
                            'FORCE_CLIENT_HOST': 'true',
                            'Content-Type': 'application/json',
                            'lastModifiedTimestamp': this.lastModifiedTimestamp
                        },
                        params: Ext.util.JSON.encode(variantIds),
                        scope: this,
                        success: success,
                        failure: success  // ignore failures silently, try cleanup next time
                    });
                }.createDelegate(this));
            }.createDelegate(this));
        }
    });

    /**
     * Renders the properties of all variants of a component in separate tabs. When the config property
     * 'variantsUuid' is not defined, no tabs will be shown and only a single variant 'hippo-default' will be used.
     *
     * @type {*}
     */
    Hippo.ChannelManager.TemplateComposer.PropertiesPanel = Ext.extend(Ext.ux.tot2ivn.VrTabPanel, {

        // set in constructor
        composerRestMountUrl: null,
        mountId: null,
        resources: null,
        variants: null,
        variantsUuid: null,
        locale: null,
        fireVariantChangeEvents: false,
        variantAdderXType: null,
        propertiesEditorXType: null,

        // set in functions
        componentId: null,
        forcedVariantId: null,
        initialForcedVariantId: null,
        pageRequestVariants: [],
        componentVariants: null,
        lastModifiedTimestamp: null,

        constructor: function(config) {
            this.composerRestMountUrl = config.composerRestMountUrl;
            this.variantsUuid = config.variantsUuid;
            this.pageRequestVariants = config.pageRequestVariants;
            this.mountId = config.mountId;
            this.resources = config.resources;
            this.locale = config.locale;

            this.variantAdderXType = config.variantAdderXType;
            this.propertiesEditorXType = config.propertiesEditorXType;

            // also store the resources in a global variable, since pluggable subclasses need to access them too
            Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources = config.resources;

            config = Ext.apply(config, { activeTab: 0 });
            Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.constructor.call(this, config);
        },

        initComponent: function() {
            Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.initComponent.apply(this, arguments);

            this.addEvents('visibleHeightChanged');

            this.on('beforetabchange', function(panel, newTab) {
                if (!Ext.isDefined(newTab)) {
                    return true;
                } else {
                    return newTab.fireEvent('beforeactivate', newTab, panel);
                }
            }, this);

            this.on('tabchange', function(panel, tab) {
                if (this.fireVariantChangeEvents && tab) {
                    this.fireEvent('variantChange', tab.componentId, tab.variant ? tab.variant.id : undefined);
                }
            }, this);

        },

        /**
         * Load and show all variant tabs for the given component. The initially selected tab is determined by the
         * 'forced variant' (the variant the component has been forced to render, e.g. because that variant
         * has been selected in this properties window before) and the available page request variants (e.g. the
         * possible variants for which this page can be rendered).
         *
         * @param componentId the UUID of the component
         * @param forcedVariantId the variant that has been forced upon this component, or undefined if no specific
         *        variant has been forced.
         * @param pageRequestVariants the possible page request variants
         * @param lastModifiedTimestamp the time this component has last been modified
         */
        load: function(componentId, forcedVariantId, pageRequestVariants, lastModifiedTimestamp) {
            if (this.componentVariants !== null) {
                this.componentVariants.un('invalidated', this.updateUI, this);
            }

            this.componentId = componentId;
            this.forcedVariantId = forcedVariantId;
            this.initialForcedVariantId = forcedVariantId;
            this.pageRequestVariants = pageRequestVariants;
            this.lastModifiedTimestamp = lastModifiedTimestamp;

            this.componentVariants = new Hippo.ChannelManager.TemplateComposer.ComponentVariants({
                componentId: componentId,
                lastModifiedTimestamp: lastModifiedTimestamp,
                composerRestMountUrl: this.composerRestMountUrl,
                variantsUuid: this.variantsUuid,
                resources: this.resources,
                locale: this.locale
            });

            this.componentVariants.on('invalidated', this.updateUI, this);

            this.updateUI();
        },

        reload: function(variantId) {
            this.forcedVariantId = variantId;
            this.componentVariants.invalidate();
            return this.componentVariants.get();
        },

        updateUI: function() {
            this.fireVariantChangeEvents = false;
            this.beginUpdate();
            this.removeAll();

            if (this.componentVariants.isMultivariate()) {
                this._showTabs();
            } else {
                this._hideTabs();
            }

            this.componentVariants.get().when(function(variants) {
                var endUpdate;

                this._initTabs(variants);
                this.adjustBodyWidth(this.tabWidth);

                this.fireVariantChangeEvents = true;
                this._selectBestMatchingTab(this.forcedVariantId, variants);

                endUpdate = this.endUpdate.createDelegate(this);
                this._loadAllTabs().when(endUpdate).otherwise(endUpdate);
            }.createDelegate(this)).otherwise(function(response) {
                Hippo.Msg.alert('Failed to get variants.', 'Only the default variant will be available: ' + response.status + ':' + response.statusText);
                this.endUpdate();
            }.createDelegate(this));
        },

        _loadAllTabs: function() {
            var futures = [];

            this.items.each(function(item) {
                futures.push(item.load());
            }, this);

            return Hippo.Future.join(futures);
        },

        selectInitialVariant: function() {
            if (this.fireVariantChangeEvents) {
                this.componentVariants.get().when(function(variants) {
                    var initialVariantId = this._getBestMatchingVariantId(this.initialForcedVariantId, variants),
                        currentVariantId = this._getCurrentVariantId();
                    if (initialVariantId !== currentVariantId) {
                        this.fireEvent('variantChange', this.componentId, initialVariantId);
                    }
                }.createDelegate(this));
            }
        },

        _initTabs: function(variants) {
            var propertiesEditorCount, i, tabComponent, propertiesForm;
            propertiesEditorCount = variants.length - 1;
            Ext.each(variants, function(variant) {
                if ('plus' === variant.id) {
                    tabComponent = this._createVariantAdder(variant, Ext.pluck(variants, 'id'));
                } else {
                    propertiesForm = new Hippo.ChannelManager.TemplateComposer.PropertiesForm({
                        variant: variant,
                        mountId: this.mountId,
                        composerRestMountUrl: this.composerRestMountUrl,
                        locale: this.locale,
                        componentId: this.componentId,
                        lastModifiedTimestamp: this.lastModifiedTimestamp,
                        bubbleEvents: ['cancel'],
                        margins: {
                            top: 0,
                            right: 10,
                            bottom: 0,
                            left: 0
                        },
                        listeners: {
                            propertiesSaved: this._onPropertiesSaved,
                            propertiesDeleted: this._onPropertiesDeleted,
                            scope: this
                        }
                    });
                    tabComponent = this._createPropertiesEditor(variant, propertiesEditorCount, propertiesForm);
                }
                this.add(tabComponent);
            }, this);
        },

        _onPropertiesSaved: function(savedVariantId) {
            Hippo.ChannelManager.TemplateComposer.Instance.selectVariant(this.componentId, savedVariantId);
            this._reloadCleanupAndFireEvent(savedVariantId, 'save');
        },

        _onPropertiesDeleted: function() {
            this._reloadCleanupAndFireEvent(this.initialForcedVariantId, 'delete');
        },

        _reloadCleanupAndFireEvent: function(variantId, event) {
            this.reload(variantId).when(function() {
                this.componentVariants.cleanup().when(function() {
                    this.fireEvent(event);
                }.createDelegate(this));
            }.createDelegate(this)).otherwise(function(response) {
                Hippo.Msg.alert('Error', 'Failed to reload component configuration: '
                    + response.status + ', ' + response.statusText
                    + '. Please close the component properties window and try again.');
            });
        },

        _createVariantAdder: function(variant, skipVariantIds) {
            return Hippo.ExtWidgets.create('Hippo.ChannelManager.TemplateComposer.VariantAdder', {
                composerRestMountUrl: this.composerRestMountUrl,
                componentId: this.componentId,
                locale: this.locale,
                skipVariantIds: skipVariantIds,
                title: variant.name,
                variantsUuid: this.variantsUuid,
                listeners: {
                    'save': function(tab, variant) {
                        this._onPropertiesSaved(variant);
                    },
                    'copy': this._copyVariant,
                    scope: this
                }
            });
        },

        _createPropertiesEditor: function(variant, variantCount, propertiesForm) {
            var editor = Hippo.ExtWidgets.create('Hippo.ChannelManager.TemplateComposer.PropertiesEditor', {
                cls: 'component-properties-editor',
                autoScroll: true,
                componentId: this.componentId,
                variant: variant,
                variantCount: variantCount,
                title: variant.name,
                propertiesForm: propertiesForm
            });
            editor.on('visibleHeightChanged', function(editor, visibleHeight) {
                this._syncVisibleHeight(visibleHeight);
            }, this);
            this.relayEvents(editor, ['cancel']);
            return editor;
        },

        _syncVisibleHeight: function(editorVisibleHeight) {
            var tabsHeight = this.stripWrap.getHeight(),
                visibleHeight = Math.max(tabsHeight, editorVisibleHeight);
            this.fireEvent('visibleHeightChanged', visibleHeight);
        },

        _hideTabs: function() {
            this.tabWidth = 0;
        },

        _showTabs: function() {
            this.tabWidth = 130;
        },

        _getCurrentVariantId: function() {
            return this.getActiveTab().variant ? this.getActiveTab().variant.id : null;
        },

        _getBestMatchingVariantId: function(forcedVariantId, variants) {
            var tabIndex = this._getBestMatchingTabIndex(forcedVariantId, variants),
                variant = this.getItem(tabIndex).variant;
            return variant ? variant.id : undefined;
        },

        _selectBestMatchingTab: function(forcedVariantId, variants) {
            var tabIndex = this._getBestMatchingTabIndex(forcedVariantId, variants);
            this.setActiveTab(tabIndex);
        },

        _getBestMatchingTabIndex: function(forcedVariantId, variants) {
            var tabIndex, i, len;

            // first check if any tab matches the forced variant
            tabIndex = this._getTabIndexByVariant(forcedVariantId, variants);
            if (tabIndex >= 0) {
                return tabIndex;
            }

            // second, find tab with the best-matching page request variant
            for (i = 0, len = this.pageRequestVariants.length; i < len; i++) {
                tabIndex = this._getTabIndexByVariant(this.pageRequestVariants[i], variants);
                if (tabIndex >= 0) {
                    return tabIndex;
                }
            }

            // third, return the first tab
            return 0;
        },

        _getTabIndexByVariant: function(variantId, variants) {
            var i, len;
            if (!Ext.isEmpty(variantId)) {
                for (i = 0, len = variants.length; i < len; i++) {
                    if (variants[i].id === variantId) {
                        return i;
                    }
                }
            }
            return -1;
        },

        _copyVariant: function(existingVariant, newVariant) {
            var existingTab, newPropertiesForm, newTab, newTabIndex;

            existingTab = this._getTab(existingVariant);
            if (Ext.isDefined(existingTab) && existingTab instanceof Hippo.ChannelManager.TemplateComposer.PropertiesEditor) {
                newPropertiesForm = existingTab.propertiesForm.copy(newVariant);
                newTab = this._createPropertiesEditor(newVariant, this.items.length, newPropertiesForm);
                newTabIndex = this.items.length - 1;
                this.insert(newTabIndex, newTab);
                this.setActiveTab(newTabIndex);
                this.syncSize();
                newTab.syncVisibleHeight();
            } else {
                console.log("Cannot find tab for variant '" + existingVariant + "', copy to '" + newVariant + "' failed");
            }
        },

        _getTab: function(variantId) {
            var tab;

            this.items.each(function(item) {
                if (Ext.isDefined(item.variant) && item.variant.id === variantId) {
                    tab = item;
                }
            });
            return tab;
        }

    });

    Hippo.ChannelManager.TemplateComposer.PropertiesForm = Ext.extend(Ext.FormPanel, {

        mountId: null,
        variant: null,
        newVariantId: null,
        composerRestMountUrl: null,
        componentId: null,
        locale: null,

        PADDING: 10,

        constructor: function(config) {
            this.variant = config.variant;
            this.newVariantId = this.variant.id;
            this.mountId = config.mountId;
            this.composerRestMountUrl = config.composerRestMountUrl;
            this.locale = config.locale;
            this.componentId = config.componentId;
            this.lastModifiedTimestamp = config.lastModifiedTimestamp;

            Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.constructor.call(this, Ext.apply(config, {
                cls: 'templateComposerPropertiesForm'
            }));
        },

        copy: function(newVariant) {
            var copy = new Hippo.ChannelManager.TemplateComposer.PropertiesForm(this.initialConfig);
            copy.variant = newVariant;
            copy._loadProperties(this.records);
            return copy;
        },

        initComponent: function() {
            var buttons = [];
            if (this.variant.id !== 'hippo-default') {
                buttons.push({
                    text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-delete'],
                    handler: function() {
                        Ext.Ajax.request({
                            method: 'DELETE',
                            url: this.composerRestMountUrl + '/' + this.componentId + './' +
                                    encodeURIComponent(this.variant.id) + '?FORCE_CLIENT_HOST=true',
                            success: function() {
                                this.fireEvent('propertiesDeleted', this, this.variant.id);
                            },
                            scope: this
                        });
                    },
                    scope: this
                });
                buttons.push('->');
            }
            this.saveButton = new Ext.Button({
                text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-save'],
                handler: this._submitForm,
                scope: this,
                formBind: true
            });
            buttons.push(this.saveButton);
            buttons.push({
                text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-cancel'],
                scope: this,
                handler: function() {
                    this.fireEvent('cancel');
                }
            });

            Ext.apply(this, {
                autoHeight: true,
                border: false,
                padding: this.PADDING,
                autoScroll: true,
                labelWidth: 100,
                labelSeparator: '',
                monitorValid: true,
                defaults: {
                    anchor: '100%'
                },
                plugins: Hippo.ChannelManager.MarkRequiredFields,
                buttons: buttons
            });

            Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.initComponent.apply(this, arguments);

            this.addEvents('propertiesSaved', 'cancel', 'propertiesDeleted');
        },

        setNewVariant: function(newVariantId) {
            this.newVariantId = newVariantId;
        },

        getVisibleHeight: function() {
            if (this.rendered) {
                return this.getHeight() + (2 * this.PADDING);
            }
            return 0;
        },

        _submitForm: function() {
            var uncheckedValues = {};

            this.getForm().items.each(function(item) {
                if (item instanceof Ext.form.Checkbox) {
                    if (!item.checked) {
                        uncheckedValues[item.name] = 'off';
                    }
                }
            });

            this.getForm().submit({
                headers: {
                    'FORCE_CLIENT_HOST': 'true',
                    'lastModifiedTimestamp': this.lastModifiedTimestamp
                },
                params: uncheckedValues,
                url: this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id) + '/rename/' + encodeURIComponent(this.newVariantId) + '?FORCE_CLIENT_HOST=true',
                method: 'POST',
                success: function() {
                    this.fireEvent('propertiesSaved', this.newVariantId);
                }.bind(this),
                failure: function(form, action) {
                    Hippo.Msg.alert(Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['toolkit-store-error-message-title'],
                            Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['toolkit-store-error-message'], function (id) {
                                Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').pageContainer.pageContext = null;
                                // reload channel manager
                                Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').pageContainer.refreshIframe();
                            });
                }
            });
        },

        _createDocument: function(ev, target, options) {
            var createUrl, createDocumentWindow;

            createUrl = this.composerRestMountUrl + '/' + this.mountId + './create?FORCE_CLIENT_HOST=true';
            createDocumentWindow = new Ext.Window({
                title: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-window-title'],
                height: 150,
                width: 400,
                modal: true,
                items: [
                    {
                        xtype: 'form',
                        height: 150,
                        padding: 10,
                        labelWidth: 120,
                        id: 'createDocumentForm',
                        defaults: {
                            labelSeparator: '',
                            anchor: '100%'
                        },
                        items: [
                            {
                                xtype: 'textfield',
                                fieldLabel: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-field-name'],
                                allowBlank: false
                            },
                            {
                                xtype: 'textfield',
                                disabled: true,
                                fieldLabel: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-field-location'],
                                value: options.docLocation
                            }
                        ]
                    }
                ],
                layout: 'fit',
                buttons: [
                    {
                        text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-button'],
                        handler: function() {
                            var createDocForm = Ext.getCmp('createDocumentForm').getForm();
                            createDocForm.submit();
                            options.docName = createDocForm.items.get(0).getValue();

                            if (options.docName === '') {
                                return;
                            }
                            createDocumentWindow.hide();

                            Ext.Ajax.request({
                                url: createUrl,
                                params: options,
                                success: function() {
                                    Ext.getCmp(options.comboId).setValue(options.docLocation + "/" + options.docName);
                                },
                                failure: function() {
                                    Hippo.Msg.alert(Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-message'],
                                            Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-failed'],
                                            function() {
                                                Hippo.ChannelManager.TemplateComposer.Instance.initComposer();
                                            }
                                    );
                                }
                            });

                        }
                    }
                ]
            });
            createDocumentWindow.addButton(
                    {
                        text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-button-cancel']
                    },
                    function() {
                        this.hide();
                    },
                    createDocumentWindow
            );
            createDocumentWindow.show();
        },

        _loadProperties: function(records) {
            this.records = records;
            var length = records.length, i, record, groupLabel, lastGroupLabel, value, defaultValue;
            if (length === 0) {
                this.add({
                    html: "<div style='padding:5px' align='center'>" + Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-no-properties'] + "</div>",
                    xtype: "panel",
                    autoWidth: true,
                    layout: 'fit'
                });
                this.saveButton.hide();
            } else {
                for (i = 0; i < length; i++) {
                    record = records[i];
                    if (record.get('hiddenInChannelManager') === false) {
                        groupLabel = record.get('groupLabel');
                        if (groupLabel !== lastGroupLabel) {
                            this.add({
                                cls: 'field-group-title ' + (lastGroupLabel === undefined ? 'first-field-group-title' : ''),
                                text: Ext.util.Format.htmlEncode(groupLabel),
                                xtype: 'label'
                            });
                            lastGroupLabel = groupLabel;
                        }

                        value = record.get('value');
                        defaultValue = record.get('defaultValue');
                        if (!value || value.length === 0) {
                            value = defaultValue;
                        }

                        if (record.get('type') === 'documentcombobox') {
                            this.addDocumentComboBox(record, defaultValue, value);
                        } else if (record.get('type') === 'combo') {
                            this.addComboBox(record, defaultValue, value);
                        } else {
                            this.addComponent(record, defaultValue, value);
                        }
                    }
                }
                this.saveButton.show();
            }

            // do a shallow layout of the form to ensure our visible height is correct
            this.doLayout(true);

            this.fireEvent('propertiesLoaded', this);
        },

        addDocumentComboBox: function(record, defaultValue, value) {
            var comboStore, propertyField, createDocumentLinkId;

            comboStore = new Ext.data.JsonStore({
                root: 'data',
                url: this.composerRestMountUrl + '/' + this.mountId + './documents/' + record.get('docType') + '?FORCE_CLIENT_HOST=true',
                fields: ['path']
            });

            propertyField = this.add({
                fieldLabel: record.get('label'),
                xtype: 'combo',
                allowBlank: !record.get('required'),
                name: record.get('name'),
                value: value,
                defaultValue: defaultValue,
                store: comboStore,
                forceSelection: true,
                triggerAction: 'all',
                displayField: 'path',
                valueField: 'path'
            });

            if (record.get('allowCreation')) {
                createDocumentLinkId = Ext.id();

                this.add({
                    bodyCfg: {
                        tag: 'div',
                        cls: 'create-document-link',
                        html: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-document-link-text'].format('<a href="#" id="' + createDocumentLinkId + '">&nbsp;', '&nbsp;</a>&nbsp;')
                    },
                    border: false
                });

                this.on('afterlayout', function() {
                    Ext.get(createDocumentLinkId).on("click", this._createDocument, this, {
                        docType: record.get('docType'),
                        docLocation: record.get('docLocation'),
                        comboId: propertyField.id
                    });
                }, this, { single: true });
            }
        },

        addComboBox: function(record, defaultValue, value) {
            var comboBoxValues, comboBoxDisplayValues, dataIndex, comboBoxValuesLength, data = [];
            comboBoxValues = record.get(
                    'dropDownListValues'
            );
            comboBoxDisplayValues = record.get(
                    'dropDownListDisplayValues'
            );

            for (dataIndex = 0, comboBoxValuesLength = comboBoxValues.length; dataIndex < comboBoxValuesLength; dataIndex++) {
                data.push([comboBoxValues[dataIndex], comboBoxDisplayValues[dataIndex]]);
            }

            this.add({
                xtype: 'combo',
                fieldLabel: record.get('label'),
                store: new Ext.data.ArrayStore({
                    fields: [
                        'id',
                        'displayText'
                    ],
                    data: data
                }),
                value: value,
                hiddenName: record.get('name'),
                typeAhead: true,
                mode: 'local',
                triggerAction: 'all',
                selectOnFocus: true,
                valueField: 'id',
                displayField: 'displayText',
                listeners: {
                    afterrender: function() {
                        // workaround, the padding-left which gets set on the element, let the right box side disappear,
                        // removing the style attribute after render fixes the layout
                        var formElement = this.el.findParent('.x-form-element');
                        formElement.removeAttribute('style');
                    }
                }
            });
        },

        addComponent: function(record, defaultValue, value) {
            var propertyField, xtype = record.get('type'),
                    propertyFieldConfig = {
                        fieldLabel: record.get('label'),
                        xtype: xtype,
                        value: value,
                        defaultValue: defaultValue,
                        allowBlank: !record.get('required'),
                        name: record.get('name'),
                        listeners: {
                            change: function() {
                                var value = this.getValue();
                                if (typeof(value) === 'undefined' || (typeof(value) === 'string' && value.length === 0) || value === this.defaultValue) {
                                    this.addClass('default-value');
                                    this.setValue(this.defaultValue);
                                } else {
                                    this.removeClass('default-value');
                                }
                            },
                            afterrender: function() {
                                // workaround, the padding-left which gets set on the element, let the right box side disappear,
                                // removing the style attribute after render fixes the layout
                                var formElement = this.el.findParent('.x-form-element');
                                formElement.removeAttribute('style');
                            }
                        }
                    };

            if (xtype === 'checkbox') {
                propertyFieldConfig.checked = (value === true || value === 'true' || value === '1' || String(value).toLowerCase() === 'on');
            } else if (xtype === 'linkpicker') {
                propertyFieldConfig.renderStripValue = /^\/?(?:[^\/]+\/)*/g;
                propertyFieldConfig.pickerConfig = {
                    configuration: record.get('pickerConfiguration'),
                    remembersLastVisited: record.get('pickerRemembersLastVisited'),
                    initialPath: record.get('pickerInitialPath'),
                    isRelativePath: record.get('pickerPathIsRelative'),
                    rootPath: record.get('pickerRootPath'),
                    selectableNodeTypes: record.get('pickerSelectableNodeTypes')
                };
            }
            propertyField = this.add(propertyFieldConfig);
            if (value === defaultValue) {
                propertyField.addClass('default-value');
            }
        },

        _loadException: function(proxy, type, actions, options, response) {
            var errorText = Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-load-exception-text'].format(actions);
            if (type === 'response') {
                errorText += '\n' + Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-load-exception-response'].format(response.statusText, response.status, options.url);
            }

            this.add({
                xtype: 'label',
                text: errorText,
                fieldLabel: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-error-field-label']
            });
        },

        load: function() {
            return new Hippo.Future(function(success, fail) {
                var componentPropertiesStore = new Ext.data.JsonStore({
                    autoLoad: false,
                    method: 'GET',
                    root: 'properties',
                    fields: ['name', 'value', 'label', 'required', 'description', 'docType', 'type', 'docLocation', 'allowCreation', 'defaultValue',
                        'pickerConfiguration', 'pickerInitialPath', 'pickerRemembersLastVisited', 'pickerPathIsRelative', 'pickerRootPath', 'pickerSelectableNodeTypes',
                        'dropDownListValues', 'dropDownListDisplayValues', 'hiddenInChannelManager', 'groupLabel' ],
                    url: this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id) + '/' + this.locale + '?FORCE_CLIENT_HOST=true'
                });

                componentPropertiesStore.on('load', function(store, records) {
                    this._loadProperties(records);
                    success();
                }, this);
                componentPropertiesStore.on('exception', function() {
                    this._loadException.apply(this, arguments);
                    fail();
                }, this);
                componentPropertiesStore.load();
            }.createDelegate(this));
        },

        disableSave: function() {
            this.saveButton.disable();
        },

        enableSave: function() {
            this.saveButton.enable();
        }

    });
    Ext.reg('Hippo.ChannelManager.TemplateComposer.PropertiesForm', Hippo.ChannelManager.TemplateComposer.PropertiesForm);

}());