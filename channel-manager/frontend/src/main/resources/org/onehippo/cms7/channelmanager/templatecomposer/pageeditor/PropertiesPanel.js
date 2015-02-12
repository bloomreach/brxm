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
        firePropertiesChangedEvents: false,
        variantAdderXType: null,
        propertiesEditorXType: null,

        // set in functions
        componentId: null,
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
                if (this.firePropertiesChangedEvents && tab && tab.propertiesForm) {
                    tab.propertiesForm.firePropertiesChanged();
                }
            }, this);
        },

        /**
         * Load and show all variant tabs for the given component. The initially selected tab is determined by
         * the available page request variants (e.g. the possible variants for which this page can be rendered).
         *
         * @param componentId the UUID of the component
         * @param pageRequestVariants the possible page request variants
         * @param lastModifiedTimestamp the time this component has last been modified
         */
        load: function(componentId, pageRequestVariants, lastModifiedTimestamp) {
            if (this.componentVariants !== null) {
                this.componentVariants.un('invalidated', this.updateUI, this);
            }

            this.componentId = componentId;
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

            if (this.componentVariants.isMultivariate()) {
                this._showTabs();
            } else {
                this._hideTabs();
            }

            this.componentVariants.on('invalidated', this.updateUI, this);

            this.updateUI();
        },

        updateUI: function(triggeredByVariantId) {
            var existingTabs = triggeredByVariantId ? this._getTabs() : [],
                reusableTabs = existingTabs.filter(function(tab) {
                    // reuse all variant tabs except the changed one that triggerd the UI update
                    return tab.variant && tab.variant.id !== triggeredByVariantId;
                });

            this.firePropertiesChangedEvents = false;
            this.beginUpdate();
            this.removeAll();
            this.componentVariants.get().when(function(variants) {
                var endUpdate;

                this._initTabs(variants, reusableTabs);
                this.adjustBodyWidth(this.tabWidth);

                this.firePropertiesChangedEvents = true;
                this._selectBestMatchingTab(triggeredByVariantId, variants);

                endUpdate = this.endUpdate.createDelegate(this);
                this._loadTabs().when(endUpdate).otherwise(endUpdate);
            }.createDelegate(this)).otherwise(function(response) {
                Hippo.Msg.alert('Failed to get variants.', 'Only the default variant will be available: ' + response.status + ':' + response.statusText);
                this.endUpdate();
            }.createDelegate(this));
        },

        _getTabs: function() {
            var tabs = [];
            this.items.each(function(tab) {
                tabs.push(tab);
            });
            return tabs;
        },

        _loadTabs: function() {
            var futures = [];

            this.items.each(function(tab) {
                futures.push(tab.load());
            }, this);

            return Hippo.Future.join(futures);
        },

        fireInitialPropertiesChangedIfNeeded: function() {
            var isActiveTabDirty = this.getActiveTab().propertiesForm.isDirty();

            if (this.firePropertiesChangedEvents) {
                this.componentVariants.get().when(function(variants) {
                    var initialVariantId = this._getBestMatchingVariantId('', variants);
                    if (isActiveTabDirty || initialVariantId !== this._getCurrentVariantId()) {
                        this._getTab(initialVariantId).propertiesForm.fireInitialPropertiesChanged();
                    }
                }.createDelegate(this));
            } else if (isActiveTabDirty) {
                this.getActiveTab().propertiesForm.fireInitialPropertiesChanged();
            }
        },

        _initTabs: function(variants, reusableTabs) {
            var propertiesEditorCount = variants.length - 1,
                reusablePropertiesForms = {};

            reusableTabs.forEach(function(tab) {
                reusablePropertiesForms[tab.variant.id] = tab.propertiesForm;
            });

            Ext.each(variants, function(variant) {
                var tab, propertiesForm;

                if ('plus' === variant.id) {
                    tab = this._createVariantAdder(variant, Ext.pluck(variants, 'id'));
                } else {
                    propertiesForm = this._createOrReusePropertiesForm(variant, reusablePropertiesForms);
                    tab = this._createPropertiesEditor(variant, variants, propertiesForm);
                }
                this.add(tab);
            }, this);
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
                    'save': function(variant) {
                        this._onPropertiesSaved(variant);
                    },
                    'copy': this._copyVariant,
                    scope: this
                }
            });
        },

        _createOrReusePropertiesForm: function(variant, formsToReuse) {
            if (formsToReuse.hasOwnProperty(variant.id)) {
                return formsToReuse[variant.id].createCopy(variant);
            } else {
                return new Hippo.ChannelManager.TemplateComposer.PropertiesForm({
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
            }
        },

        _onPropertiesChanged: function(propertiesMap) {
            this.fireEvent('propertiesChanged', this.componentId, propertiesMap);
        },

        _onPropertiesSaved: function(savedVariantId) {
            this._reloadCleanupAndFireEvent(savedVariantId, 'save');
        },

        _onPropertiesDeleted: function(deletedVariantId) {
            this._reloadCleanupAndFireEvent(deletedVariantId, 'delete');
        },

        _reloadCleanupAndFireEvent: function(changedVariantId, event) {
            this.componentVariants.invalidate(changedVariantId);
            this.componentVariants.cleanup().when(function() {
                this.fireEvent(event);
            }.createDelegate(this)).otherwise(function(response) {
                Hippo.Msg.alert('Error', 'Failed to reload component configuration: '
                + response.status + ', ' + response.statusText
                + '. Please close the component properties window and try again.');
            });
        },

        _createPropertiesEditor: function(variant, variants, propertiesForm) {
            var editor = Hippo.ExtWidgets.create('Hippo.ChannelManager.TemplateComposer.PropertiesEditor', {
                cls: 'component-properties-editor',
                autoScroll: true,
                componentId: this.componentId,
                variant: variant,
                allVariants: variants,
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

        _getBestMatchingVariantId: function(variantId, variants) {
            var tabIndex = this._getBestMatchingTabIndex(variantId, variants),
                variant = this.getItem(tabIndex).variant;
            return variant ? variant.id : undefined;
        },

        _selectBestMatchingTab: function(variantId, variants) {
            var tabIndex = this._getBestMatchingTabIndex(variantId, variants);
            this.setActiveTab(tabIndex);
        },

        _getBestMatchingTabIndex: function(variantId, variants) {
            var tabIndex, i, len;

            // first check if any tab matches the given variant
            tabIndex = this._getTabIndexByVariant(variantId, variants);
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
                newPropertiesForm = existingTab.propertiesForm.createCopy(newVariant);
                newTab = this._createPropertiesEditor(newVariant,
                    Ext.pluck(this.items.getRange(), "variant"),
                    newPropertiesForm);
                newTabIndex = this.items.length - 1;
                this.insert(newTabIndex, newTab);
                this.setActiveTab(newTabIndex);
                this.syncSize();
                newTab.syncVisibleHeight();
                newPropertiesForm.firePropertiesChanged();
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