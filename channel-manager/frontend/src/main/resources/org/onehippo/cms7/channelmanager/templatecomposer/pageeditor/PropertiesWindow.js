/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  "use strict";

  Ext.namespace('Hippo.ChannelManager.TemplateComposer');

  Hippo.ChannelManager.TemplateComposer.PropertiesWindow = Ext.extend(Hippo.ux.window.FloatingWindow, {

    _formStates: {},

    constructor: function (config) {
      var buttons = [],
        windowWidth = config.width;

      this.propertiesPanel = new Hippo.ChannelManager.TemplateComposer.PropertiesPanel({
        id: 'componentPropertiesPanel',
        resources: config.resources,
        locale: config.locale,
        composerRestMountUrl: config.composerRestMountUrl,
        variantsUuid: config.variantsUuid,
        globalVariantsStore: config.globalVariantsStore,
        globalVariantsStoreFuture: config.globalVariantsStoreFuture,
        mountId: config.mountId,
        listeners: {
          visibleHeightChanged: this._adjustHeight,
          close: this.hide,
          clientvalidation: this._onClientValidation,
          onLoad: this._resetFormStates,
          beforetabchange: function (panel, newTab, currentTab) {
            if (newTab) {
              newTab.addClass('qa-tab-active');
            }
            if (currentTab) {
              currentTab.removeClass('qa-tab-active');
            }
          },
          scope: this
        }
      });

      if (Ext.isDefined(this.variantsUuid)) {
        windowWidth += this.propertiesPanel.tabWidth;
      }

      this.saveButton = new Ext.Button({
        xtype: 'button',
        cls: 'btn btn-default',
        text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-save'],
        scope: this,
        handler: function () {
          this.propertiesPanel.saveAll().then(this._resetFormStates.bind(this));
        }
      });

      buttons.push(this.saveButton);
      buttons.push({
        xtype: 'button',
        cls: 'btn btn-default',
        text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-close'],
        scope: this,
        handler: function () {
          this.propertiesPanel.fireEvent('close');
        }
      });

      Hippo.ChannelManager.TemplateComposer.PropertiesWindow.superclass.constructor.call(this, Ext.apply(config, {
        layout: 'fit',
        width: windowWidth,
        items: this.propertiesPanel,
        buttons: buttons
      }));
    },

    initComponent: function () {
      Hippo.ChannelManager.TemplateComposer.PropertiesWindow.superclass.initComponent.apply(this, arguments);

      this.addEvents('save', 'close', 'delete', 'propertiesChanged');

      this.on('hide', this.propertiesPanel.onHide, this.propertiesPanel);
    },

    _adjustHeight: function (propertiesPanelVisibleHeight) {
      var newVisibleHeight = propertiesPanelVisibleHeight + this.getFrameHeight(),
        pageEditorHeight = Ext.getCmp('pageEditorIFrame').getHeight(),
        windowY = this.getPosition()[1],
        spaceBetweenWindowAndBottom = 4,
        maxHeight = pageEditorHeight - windowY - spaceBetweenWindowAndBottom,
        newHeight = Math.min(newVisibleHeight, maxHeight);
      if (this.getHeight() !== newHeight) {
        this.setHeight(newHeight);
      }
    },

    _resetFormStates: function() {
      this._formStates = {};
    },

    _onClientValidation: function (form, valid) {
      var disableSaveButton = true,
        count = 0,
        name = form.variant.variantName;

      this._formStates[name] = {
        name: name,
        valid: valid,
        dirty: form.isDirty()
      };

      // enable save if all forms are valid and exists a dirty one
      for (name in this._formStates) {
        if (!this._formStates[name].valid) {
          break;
        }
        count++;
      }
      if (count === Object.keys(this._formStates).length) {
        // check if a dirty form exists
        for (name in this._formStates) {
          if (this._formStates[name].dirty) {
            disableSaveButton = false;
            break;
          }
        }
      }

      this.saveButton.setDisabled(disableSaveButton);
    }

  });

}());
