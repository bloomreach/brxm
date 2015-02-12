/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
            var isActiveTabDirty = this.getActiveTab().propertiesForm.isDirty();

            if (this.fireVariantChangeEvents) {
                this.componentVariants.get().when(function(variants) {
                    var initialVariantId = this._getBestMatchingVariantId(this.initialForcedVariantId, variants);
                    if (isActiveTabDirty || initialVariantId !== this._getCurrentVariantId()) {
                        this.fireEvent('variantChange', this.componentId, initialVariantId);
                    }
                }.createDelegate(this));
            } else if (isActiveTabDirty) {
                this.fireEvent('variantChange', this.componentId, undefined);
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
                        bubbleEvents: ['variantDirty', 'variantPristine', 'close'],
                        margins: {
                            top: 0,
                            right: 10,
                            bottom: 0,
                            left: 0
                        },
                        listeners: {
                            propertiesChanged: this._onPropertiesChanged,
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

        _onPropertiesChanged: function(propertiesMap) {
            this.fireEvent('propertiesChanged', this.componentId, propertiesMap);
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

            editor.on('variantDirty', function() {
                this.setTitle('* ' + variant.name);
            }, editor);

            editor.on('variantPristine', function() {
                this.setTitle(variant.name);
            }, editor);

            this.relayEvents(editor, ['close']);

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

}());