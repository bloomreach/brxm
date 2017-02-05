/*
 * Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function ($) {

  "use strict";

  Ext.namespace('Hippo.ChannelManager.ChannelEditor');

  function getVariantName (variant) {
    return variant.variantName || variant.name;
  }

  /**
   * Renders the properties of all variants of a component in separate tabs. When the config property
   * 'variantsUuid' is not defined, no tabs will be shown and only a single variant 'hippo-default' will be used.
   *
   * @type {*}
   */
  Hippo.ChannelManager.ChannelEditor.ComponentPropertiesPanel = Ext.extend(Ext.ux.tot2ivn.VrTabPanel, {

    // set in constructor
    composerRestMountUrl: null,
    mountId: null,
    variantsUuid: null,
    locale: null,
    firePropertiesChangedEvents: false,
    variantAdderXType: null,
    propertiesEditorXType: null,

    // set in functions
    componentId: null,
    pageRequestVariants: {},
    componentVariants: null,
    lastModified: null,

    constructor: function (config) {
      this.composerRestMountUrl = config.composerRestMountUrl;
      this.variantsUuid = config.variantsUuid;
      this.pageRequestVariants = config.pageRequestVariants;
      this.mountId = config.mountId;
      this.locale = config.locale;

      this.componentMessageBus = Hippo.createMessageBus('properties-panel');

      this.variantAdderXType = config.variantAdderXType;
      this.propertiesEditorXType = config.propertiesEditorXType;

      config = Ext.apply(config, {activeTab: 0});
      Hippo.ChannelManager.ChannelEditor.ComponentPropertiesPanel.superclass.constructor.call(this, Ext.apply(config, {
        border: false,
        tabMarginTop: 0
      }));
    },

    initComponent: function () {
      Hippo.ChannelManager.ChannelEditor.ComponentPropertiesPanel.superclass.initComponent.apply(this, arguments);

      this.addEvents('visibleHeightChanged', 'onLoad', 'componentChanged', 'loadFailed');

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
        if (this.firePropertiesChangedEvents && tab && tab.componentPropertiesForm) {
          tab.componentPropertiesForm.firePropertiesChanged();
          tab.syncVisibleHeight();
        }
      }, this);
    },

    /**
     * Load and show all variant tabs for the given component. The initially selected tab is determined by
     * the available page request variants (e.g. the possible variants for which this page can be rendered).
     *
     * @param component object with all information about the component.
     * @param container object with information about the container of the component.
     * @param page object with all page meta-data.
     */
    setComponent: function (component, container, page) {
      if (!this.hasComponent) {
        this._setNewComponent(component, container, page);
      } else {
        this._startValidationMonitoring();
      }
      this.fireEvent('onLoad');
    },

    _setNewComponent: function (component, container, page) {
      if (this.componentVariants !== null) {
        this.componentVariants.un('invalidated', this.updateUI, this);
        this._fireInitialPropertiesChangedIfNeeded();
      }

      this.componentId = component.id;
      this.pageRequestVariants = page['HST-Page-Request-Variants'] || {};
      this.lastModified = component.lastModified;
      this.container = container;
      this.hasComponent = true;

      this.componentVariants = new Hippo.ChannelManager.ChannelEditor.ComponentVariants({
        componentId: component.id,
        lastModified: component.lastModified,
        composerRestMountUrl: this.composerRestMountUrl,
        variantsUuid: this.variantsUuid,
        locale: this.locale
      });

      if (!this.componentVariants.isMultivariate()) {
        this._hideTabs();
      }

      this.componentVariants.on('invalidated', this.updateUI, this);

      this.updateUI().otherwise(function (response) {
        var jsonData = Ext.util.JSON.decode(response.responseText),
          data = {};

        if (jsonData && jsonData.message && jsonData.message.startsWith('javax.jcr.ItemNotFoundException')) {
          data.error = 'ITEM_NOT_FOUND';
          data.parameterMap = {
            component: component.label
          };
        } else {
          data.error = 'UNKNOWN';
        }
        this.fireEvent('loadFailed', data);
      }.bind(this));
    },

    updateUI: function (changedVariantIds, activeVariantId) {
      var existingTabs = changedVariantIds ? this._getTabs() : [],
        reusableTabs = existingTabs.filter(function (tab) {
          // reuse all variant tabs except the changed ones that have been saved
          return tab.variant && changedVariantIds.indexOf(tab.variant.id) === -1;
        });

      this.firePropertiesChangedEvents = false;
      this.beginUpdate();
      this.removeAll();
      return this.componentVariants.get().when(function (variants) {
        this._initTabs(variants, reusableTabs);
        this.adjustBodyWidth(this.tabWidth);

        this.firePropertiesChangedEvents = true;

        this._selectBestMatchingTab(activeVariantId, variants).then(function () {
          this._loadTabs().always(this.endUpdate.bind(this));
        }.bind(this));
      }.bind(this)).otherwise(this.endUpdate.bind(this));
    },

    _getTabs: function () {
      var tabs = [];
      this.items.each(function (tab) {
        tabs.push(tab);
      });
      return tabs;
    },

    _loadTabs: function () {
      var loadAllTabs = [];

      this.items.each(function (tab) {
        loadAllTabs.push(tab.load());
      }, this);

      return $.when.apply($, loadAllTabs);
    },

    onHide: function () {
      this._stopValidationMonitoring();
    },

    _startValidationMonitoring: function () {
      var activeTab = this.getActiveTab();
      if (activeTab && activeTab.componentPropertiesForm) {
        activeTab.componentPropertiesForm.startMonitoring();
      }
    },

    _stopValidationMonitoring: function () {
      var activeTab = this.getActiveTab();
      if (activeTab && activeTab.componentPropertiesForm) {
        activeTab.componentPropertiesForm.stopMonitoring();
      }
    },

    _fireInitialPropertiesChangedIfNeeded: function () {
      var isActiveTabDirty = this.getActiveTab().componentPropertiesForm.isDirty();

      if (this.firePropertiesChangedEvents) {
        this.componentVariants.get().when(function (variants) {
          var initialVariantId = this._getBestMatchingVariantId('', variants);
          if (isActiveTabDirty || initialVariantId !== this._getCurrentVariantId()) {
            this.renderInitialComponentState();
          }
        }.createDelegate(this));
      } else if (isActiveTabDirty) {
        this.renderInitialComponentState();
      }
    },

    _initTabs: function (variants, reusableTabs) {
      var reusableComponentPropertiesForms = {};

      reusableTabs.forEach(function (tab) {
        reusableComponentPropertiesForms[tab.variant.id] = tab.componentPropertiesForm;
      });

      Ext.each(variants, function (variant) {
        var tab, componentPropertiesForm;

        if ('plus' === variant.id) {
          if (!this.container.isDisabled) {
            tab = this._createVariantAdder(variant, Ext.pluck(variants, 'id'));
            this.add(tab);
          }
        } else {
          componentPropertiesForm = this._createOrReuseComponentPropertiesForm(variant, reusableComponentPropertiesForms);
          tab = this._createComponentPropertiesEditor(variant, variants, componentPropertiesForm);
          this.add(tab);
        }
      }, this);
    },

    _createVariantAdder: function (variant, skipVariantIds) {
      return Hippo.ExtWidgets.create('Hippo.ChannelManager.ChannelEditor.ComponentVariantAdder', {
        composerRestMountUrl: this.composerRestMountUrl,
        componentId: this.componentId,
        locale: this.locale,
        skipVariantIds: skipVariantIds,
        title: getVariantName(variant),
        variantsUuid: this.variantsUuid,
        getCurrentVariant: this.getCurrentVariant.bind(this),
        componentMessageBus: this.componentMessageBus,
        listeners: {
          'copy': this._copyVariant,
          scope: this
        }
      });
    },

    _createOrReuseComponentPropertiesForm: function (variant, formsToReuse) {
      if (formsToReuse.hasOwnProperty(variant.id)) {
        return formsToReuse[variant.id].createCopy(variant);
      } else {
        return new Hippo.ChannelManager.ChannelEditor.ComponentPropertiesForm({
          variant: variant,
          mountId: this.mountId,
          composerRestMountUrl: this.composerRestMountUrl,
          locale: this.locale,
          componentId: this.componentId,
          lastModified: this.lastModified,
          isReadOnly: this.container.isDisabled,
          bubbleEvents: ['variantDirty', 'variantPristine', 'clientvalidation', 'componentLocked'],
          listeners: {
            propertiesChanged: this._onPropertiesChanged,
            propertiesDeleted: this._onPropertiesDeleted,
            scope: this
          }
        });
      }
    },

    _onPropertiesChanged: function (form, propertiesMap) {
      if (form === this.getActiveTab().componentPropertiesForm) {
        // Only propagate the changed properties from the active tab. Sometimes tabs that have just been deactivated
        // still fire a propertiesChanged event. That confuses the channel editor, so ignore those events.
        this.fireEvent('propertiesChanged', this.componentId, propertiesMap);
      }
    },

    renderInitialComponentState: function () {
      this.fireEvent('propertiesChanged', this.componentId, {});
    },

    notifyComponentChanged: function () {
      this.fireEvent('componentChanged', this.componentId);
    },

    /**
     * Event called after all dirty forms are saved
     * @param savedVariantIds  list of dirty variants that have been saved
     * @param activeVariantId  the active variant in the panel
     * @private
     */
    _onSaved: function (savedVariantIds, activeVariantId) {
      this._reloadCleanupAndFireEvent(savedVariantIds, activeVariantId, 'save');
    },

    _onPropertiesDeleted: function (deletedVariantId) {
      // set the active tab be the first one (i.e. the hippo-default variant)
      this._reloadCleanupAndFireEvent([deletedVariantId], 'hippo-default', 'variantDeleted');
    },

    _onVariantsDeleted: function (deletedVariantIds, newActiveVariantId) {
      this._reloadCleanupAndFireEvent(deletedVariantIds, newActiveVariantId, 'variantDeleted');
    },

    _reloadCleanupAndFireEvent: function (changedVariantIds, activeVariantId, event) {
      this.componentVariants.invalidate(changedVariantIds, activeVariantId);
      this.componentVariants.cleanup().when(function () {
        this.fireEvent(event);
      }.createDelegate(this)).otherwise(function (response) {
        Hippo.Msg.alert('Error', 'Failed to reload component configuration: '
        + response.status + ', ' + response.statusText
        + '. Please close the component properties window and try again.');
      });
    },

    _createComponentPropertiesEditor: function (variant, variants, componentPropertiesForm) {
      var editor = Hippo.ExtWidgets.create('Hippo.ChannelManager.ChannelEditor.ComponentPropertiesEditor', {
        cls: 'component-properties-editor',
        componentId: this.componentId,
        variant: variant,
        allVariants: variants.slice(0, variants.length - 1),
        title: getVariantName(variant),
        componentPropertiesForm: componentPropertiesForm,
        isReadOnly: this.container.isDisabled,
        isInherited: this.container.isInherited,
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
        componentPropertiesForm.startMonitoring();
      });

      editor.on('beforedeactivate', function () {
        componentPropertiesForm.stopMonitoring();
      });

      editor.on('variantsDeleted', this._onVariantsDeleted, this);

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
      var fetchInitialActiveVariantId;

      if (variantId) {
        fetchInitialActiveVariantId = $.Deferred().resolve(variantId).promise();
      } else {
        fetchInitialActiveVariantId = this._getFirstPropertiesEditor().getInitialActiveVariantId();
      }

      return fetchInitialActiveVariantId.then(function (activeVariantId) {
        var tabIndex = this._getBestMatchingTabIndex(activeVariantId, variants);
        this.setActiveTab(tabIndex);
      }.bind(this));
    },

    _getFirstPropertiesEditor: function () {
      var result = this.items[0];
      this.items.find(function (item) {
        if (item.variant.id !== 'plus') {
          result = item;
          return true;
        }
      });
      return result;
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
      if (Ext.isDefined(existingTab) && existingTab instanceof Hippo.ChannelManager.ChannelEditor.ComponentPropertiesEditor) {
        newPropertiesForm = existingTab.componentPropertiesForm.createCopy(newVariant);
        newPropertiesForm.hideDelete();
        newTab = this._createComponentPropertiesEditor(
          newVariant,
          Ext.pluck(this.items.getRange(), "variant"),
          newPropertiesForm);
        newTabIndex = this.items.length - 1;
        this.insert(newTabIndex, newTab);
        this.setActiveTab(newTabIndex);
        this.syncSize();
        newTab.syncVisibleHeight();
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
        if (Ext.isDefined(item.componentPropertiesForm) && item.componentPropertiesForm.isDirty()) {
          editors.push(item);
        }
      });
      return editors;
    },

    // Update the activeVariantId based on the mapping between old and new variant id after save.
    // @see ComponentPropertiesEditor#save for more detail
    _findActiveVariantId: function(mapVariantIds, activeVariantId) {
      mapVariantIds.some(function (entry) {
        if (entry.oldId === activeVariantId) {
          activeVariantId = entry.newId;
          return true;
        }
      });
      return activeVariantId;
    },

    /**
     * Save all dirty forms in the panel
     * @param success
     * @param fail
     */
    saveAll: function () {
      var dirtyEditors = this._getDirtyEditors(),
        savePromises = [],
        dirtyVariantIds = [],
        activeVariantId = this.getActiveTab().variant.id;

      dirtyEditors.forEach(function(editor) {
        savePromises.push(editor.save());
        dirtyVariantIds.push(editor.variant.id);
      });

      return $.when.apply($, savePromises).then(function () {
        var afterSavePromises = [],
          mapVariantIds = [].slice.call(arguments),
          savedVariantIds = Ext.pluck(mapVariantIds, "newId");

        activeVariantId = this._findActiveVariantId(mapVariantIds, activeVariantId);
        this._onSaved(savedVariantIds, activeVariantId);

        dirtyEditors.forEach(function(editor) {
          afterSavePromises.push(editor.onAfterSave());
        });
        return $.when.apply($, afterSavePromises).then(this.notifyComponentChanged.bind(this));
      }.bind(this));
    },

    deleteComponent: function () {
      this.fireEvent('deleteComponent', this.componentId);
    }
  });

}(jQuery));
