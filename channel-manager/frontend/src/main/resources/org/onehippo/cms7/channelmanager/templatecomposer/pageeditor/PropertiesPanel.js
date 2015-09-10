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
(function () {

  "use strict";

  Ext.namespace('Hippo.ChannelManager.TemplateComposer');

  function getVariantName (variant) {
    return variant.variantName || variant.name;
  }

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
    variantsUuid: null,
    locale: null,
    firePropertiesChangedEvents: false,
    variantAdderXType: null,
    propertiesEditorXType: null,

    // set in functions
    componentId: null,
    pageRequestVariants: {},
    componentVariants: null,
    lastModifiedTimestamp: null,

    constructor: function (config) {
      this.composerRestMountUrl = config.composerRestMountUrl;
      this.variantsUuid = config.variantsUuid;
      this.pageRequestVariants = config.pageRequestVariants;
      this.mountId = config.mountId;
      this.resources = config.resources;
      this.locale = config.locale;

      this.componentMessageBus = Hippo.createMessageBus('properties-panel');

      this.variantAdderXType = config.variantAdderXType;
      this.propertiesEditorXType = config.propertiesEditorXType;

      // also store the resources in a global variable, since pluggable subclasses need to access them too
      Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources = config.resources;

      config = Ext.apply(config, {activeTab: 0});
      Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.constructor.call(this, Ext.apply(config, {
        border: false,
        tabMarginTop: 0
      }));
    },

    initComponent: function () {
      Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.initComponent.apply(this, arguments);

      //TODO: add 'clientvalidation' event to notify parent window
      this.addEvents('visibleHeightChanged');

      this.on('beforetabchange', function (panel, newTab, currentTab) {
        var proceed;
        if (!Ext.isDefined(newTab)) {
          proceed = true;
        } else {
          proceed = newTab.fireEvent('beforeactivate', newTab, panel);
          if (proceed && currentTab) {
            currentTab.fireEvent('beforedeactivate', currentTab);
          }
        }
        return proceed;
      }, this);

      this.on('tabchange', function (panel, tab) {
        if (this.firePropertiesChangedEvents && tab && tab.propertiesForm) {
          tab.propertiesForm.firePropertiesChanged();
          tab.syncVisibleHeight();
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
     * @param readOnly whether to show the properties read-only or not
     */
    load: function (componentId, pageRequestVariants, lastModifiedTimestamp, readOnly) {
      if (this.componentVariants !== null) {
        this.componentVariants.un('invalidated', this.updateUI, this);
        this._fireInitialPropertiesChangedIfNeeded();
      }

      this.componentId = componentId;
      this.pageRequestVariants = pageRequestVariants;
      this.lastModifiedTimestamp = lastModifiedTimestamp;
      this.isReadOnly = readOnly;

      this.componentVariants = new Hippo.ChannelManager.TemplateComposer.ComponentVariants({
        componentId: componentId,
        lastModifiedTimestamp: lastModifiedTimestamp,
        composerRestMountUrl: this.composerRestMountUrl,
        variantsUuid: this.variantsUuid,
        resources: this.resources,
        locale: this.locale
      });

      if (!this.componentVariants.isMultivariate()) {
        this._hideTabs();
      }

      this.componentVariants.on('invalidated', this.updateUI, this);

      this.updateUI();
    },

    updateUI: function (triggeredByVariantId) {
      var existingTabs = triggeredByVariantId ? this._getTabs() : [],
        reusableTabs = existingTabs.filter(function (tab) {
          // reuse all variant tabs except the changed one that triggerd the UI update
          return tab.variant && tab.variant.id !== triggeredByVariantId;
        });

      this.firePropertiesChangedEvents = false;
      this.beginUpdate();
      this.removeAll();
      this.componentVariants.get().when(function (variants) {
        var endUpdate;

        this._initTabs(variants, reusableTabs);
        this.adjustBodyWidth(this.tabWidth);

        this.firePropertiesChangedEvents = true;
        this._selectBestMatchingTab(triggeredByVariantId, variants);

        endUpdate = this.endUpdate.createDelegate(this);
        this._loadTabs().when(endUpdate).otherwise(endUpdate);
      }.createDelegate(this)).otherwise(function (response) {
        Hippo.Msg.alert('Failed to get variants.', 'Only the default variant will be available: ' + response.status + ':' + response.statusText);
        this.endUpdate();
      }.createDelegate(this));
    },

    _getTabs: function () {
      var tabs = [];
      this.items.each(function (tab) {
        tabs.push(tab);
      });
      return tabs;
    },

    _loadTabs: function () {
      var futures = [];

      this.items.each(function (tab) {
        futures.push(tab.load());
      }, this);

      return Hippo.Future.join(futures);
    },

    onHide: function () {
      this._stopValidationMonitoring();
      this._renderInitialComponentState();
    },

    _stopValidationMonitoring: function () {
      var activeForm = this.getActiveTab().propertiesForm;
      if (activeForm) {
        activeForm.stopMonitoring();
      }
    },

    _fireInitialPropertiesChangedIfNeeded: function () {
      var isActiveTabDirty = this.getActiveTab().propertiesForm.isDirty();

      if (this.firePropertiesChangedEvents) {
        this.componentVariants.get().when(function (variants) {
          var initialVariantId = this._getBestMatchingVariantId('', variants);
          if (isActiveTabDirty || initialVariantId !== this._getCurrentVariantId()) {
            this._renderInitialComponentState();
          }
        }.createDelegate(this));
      } else if (isActiveTabDirty) {
        this._renderInitialComponentState();
      }
    },

    _initTabs: function (variants, reusableTabs) {
      var reusablePropertiesForms = {};

      reusableTabs.forEach(function (tab) {
        reusablePropertiesForms[tab.variant.id] = tab.propertiesForm;
      });

      Ext.each(variants, function (variant) {
        var tab, propertiesForm;

        if ('plus' === variant.id) {
          if (!this.isReadOnly) {
            tab = this._createVariantAdder(variant, Ext.pluck(variants, 'id'));
            this.add(tab);
          }
        } else {
          propertiesForm = this._createOrReusePropertiesForm(variant, reusablePropertiesForms);
          tab = this._createPropertiesEditor(variant, variants, propertiesForm);
          this.add(tab);
        }
      }, this);
    },

    _createVariantAdder: function (variant, skipVariantIds) {
      return Hippo.ExtWidgets.create('Hippo.ChannelManager.TemplateComposer.VariantAdder', {
        composerRestMountUrl: this.composerRestMountUrl,
        componentId: this.componentId,
        locale: this.locale,
        skipVariantIds: skipVariantIds,
        title: getVariantName(variant),
        variantsUuid: this.variantsUuid,
        getCurrentVariant: this.getCurrentVariant.bind(this),
        componentMessageBus: this.componentMessageBus,
        listeners: {
          'save': function (variant) {
            this._onPropertiesSaved(variant);
          },
          'copy': this._copyVariant,
          scope: this
        }
      });
    },

    _createOrReusePropertiesForm: function (variant, formsToReuse) {
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
          isReadOnly: this.isReadOnly,
          bubbleEvents: ['variantDirty', 'variantPristine', 'close'],
          listeners: {
            propertiesChanged: this._onPropertiesChanged,
            propertiesSaved: this._onPropertiesSaved,
            propertiesDeleted: this._onPropertiesDeleted,
            // TODO: catch and fire 'clientvalidation' event to notify parent window
            scope: this
          }
        });
      }
    },

    _onPropertiesChanged: function (propertiesMap) {
      this.fireEvent('propertiesChanged', this.componentId, propertiesMap);
    },

    _renderInitialComponentState: function () {
      this.fireEvent('propertiesChanged', this.componentId, {});
    },

    _onPropertiesSaved: function (savedVariantId) {
      this._reloadCleanupAndFireEvent(savedVariantId, 'save');
    },

    _onPropertiesDeleted: function (deletedVariantId) {
      this._reloadCleanupAndFireEvent(deletedVariantId, 'delete');
    },

    _reloadCleanupAndFireEvent: function (changedVariantId, event) {
      this.componentVariants.invalidate(changedVariantId);
      this.componentVariants.cleanup().when(function () {
        this.fireEvent(event);
      }.createDelegate(this)).otherwise(function (response) {
        Hippo.Msg.alert('Error', 'Failed to reload component configuration: '
        + response.status + ', ' + response.statusText
        + '. Please close the component properties window and try again.');
      });
    },

    _createPropertiesEditor: function (variant, variants, propertiesForm) {
      var editor = Hippo.ExtWidgets.create('Hippo.ChannelManager.TemplateComposer.PropertiesEditor', {
        cls: 'component-properties-editor',
        componentId: this.componentId,
        variant: variant,
        allVariants: variants.slice(0, variants.length - 1),
        title: getVariantName(variant),
        propertiesForm: propertiesForm,
        isReadOnly: this.isReadOnly,
        componentMessageBus: this.componentMessageBus
      });

      editor.on('visibleHeightChanged', function (editor, visibleHeight) {
        this._syncVisibleHeight(visibleHeight);
      }, this);

      editor.on('variantDirty', function () {
        this.setTitle('* ' + getVariantName(variant));
      }, editor);

      editor.on('variantPristine', function () {
        this.setTitle(getVariantName(variant));
      }, editor);

      editor.on('beforeactivate', function () {
        propertiesForm.startMonitoring();
      });

      editor.on('beforedeactivate', function () {
        propertiesForm.stopMonitoring();
      });

      this.relayEvents(editor, ['close']);

      return editor;
    },

    _syncVisibleHeight: function (editorVisibleHeight) {
      var tabsHeight = this.stripWrap.getHeight(),
        visibleHeight = Math.max(tabsHeight, editorVisibleHeight);
      this.fireEvent('visibleHeightChanged', visibleHeight);
    },

    _hideTabs: function () {
      this.tabWidth = 0;
    },

    getCurrentVariant: function () {
      return this.getActiveTab().variant;
    },

    _getCurrentVariantId: function () {
      var currentVariant = this.getCurrentVariant();
      return currentVariant ? currentVariant.id : null;
    },

    _getBestMatchingVariantId: function (variantId, variants) {
      var tabIndex = this._getBestMatchingTabIndex(variantId, variants),
        variant = this.getItem(tabIndex).variant;
      return variant ? variant.id : undefined;
    },

    _selectBestMatchingTab: function (variantId, variants) {
      var tabIndex = this._getBestMatchingTabIndex(variantId, variants);
      this.setActiveTab(tabIndex);
    },

    _getBestMatchingTabIndex: function (variantId, variants) {
      var tabIndex = 0,
        candidates = [
          variantId,
          this.pageRequestVariants[this.componentId]
        ];

      candidates.some(function (candidate) {
        var index = this._getTabIndexByVariant(candidate, variants);
        if (index >= 0) {
          tabIndex = index;
          return true;
        }
      }.bind(this));

      return tabIndex;
    },

    _getTabIndexByVariant: function (variantId, variants) {
      var result = -1;
      if (!Ext.isEmpty(variantId)) {
        variants.some(function (variant, i) {
          if (variants[i].id === variantId) {
            result = i;
            return true;
          }
        });
      }
      return result;
    },

    _copyVariant: function (existingVariantId, newVariant) {
      var existingTab, newPropertiesForm, newTab, newTabIndex;

      existingTab = this._getTab(existingVariantId);
      if (Ext.isDefined(existingTab) && existingTab instanceof Hippo.ChannelManager.TemplateComposer.PropertiesEditor) {
        newPropertiesForm = existingTab.propertiesForm.createCopy(newVariant);
        newTab = this._createPropertiesEditor(
          newVariant,
          Ext.pluck(this.items.getRange(), "variant"),
          newPropertiesForm);
        newTabIndex = this.items.length - 1;
        this.insert(newTabIndex, newTab);
        this.setActiveTab(newTabIndex);
        this.syncSize();
        newTab.syncVisibleHeight();
        newPropertiesForm.firePropertiesChanged();
      } else {
        console.log("Cannot find tab for variant '" + existingVariantId + "', copy to '" + newVariant + "' failed");
      }
    },

    _getTab: function (variantId) {
      var tab;

      this.items.each(function (item) {
        if (Ext.isDefined(item.variant) && item.variant.id === variantId) {
          tab = item;
        }
      });
      return tab;
    },

    /**
     * Return editors that have dirty forms
     * @returns {Array}
     * @private
     */
    _getDirtyEditors: function () {
      var editors = [];
      this.items.each(function (item) {
        if (Ext.isDefined(item.propertiesForm) && item.propertiesForm.isDirty()) {
          editors.push(item);
        }
      });
      return editors;
    },

    /**
     * Save all dirty forms in the panel
     * @param success
     * @param fail
     */
    saveAll: function () {
      var dirtyEditors = this._getDirtyEditors(),
        afterSaveCallback = null,
        saveAllDefer = null,
        promises = [];

      dirtyEditors.forEach(function(editor) {
        promises.push(editor.save());
        if (afterSaveCallback === null) {
          afterSaveCallback = editor.getCallbackAfterSave();
        }
      });

      saveAllDefer = $.Deferred();
      $.when.apply($, promises).then(function () {
        if (afterSaveCallback) {
          afterSaveCallback();
        }
        saveAllDefer.resolve();
      }, function () {
        saveAllDefer.reject();
      });

      return saveAllDefer.promise();
    }
  });

}());
