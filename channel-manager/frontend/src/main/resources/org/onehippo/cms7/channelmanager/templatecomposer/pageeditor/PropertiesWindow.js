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

    constructor: function (config) {
      var windowWidth = config.width;

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
          // TODO: handle 'clientvalidation' to enable/disable 'Save' button
          scope: this
        }
      });

      if (Ext.isDefined(this.variantsUuid)) {
        windowWidth += this.propertiesPanel.tabWidth;
      }

      this.saveButton = new Ext.Button({
        xtype: 'button',
        cls: 'btn btn-default',
        text: 'Save',
        listeners: {
          click: function () {
            console.log("save clicked");
            // TODO: loop through all dirty forms & save

            // TODO: fired the event upon complete all saving such as
            // this.propertiesPanel[dirty].editor.onAfterSavedProperties();
          }
        }
      });

      var buttons = [];
      buttons.push(this.saveButton);
      buttons.push({
        xtype: 'button',
        cls: 'btn btn-default',
        text: 'Close'
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
    }
  });

}());