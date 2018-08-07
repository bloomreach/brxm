/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

(function($) {
  "use strict";

  Ext.namespace('Hippo.ChannelManager.ChannelEditor');

  Hippo.ChannelManager.ChannelEditor.ChannelEditor = Ext.extend(Hippo.IFramePanel, {

    constructor: function(config) {
      this.apiUrlPrefix = config.apiUrlPrefix;
      this.title = null;
      this.antiCache = config.ANTI_CACHE;

      Hippo.ChannelManager.ChannelEditor.Resources = config.resources;

      Ext.apply(config, {
        cls: 'qa-channel-editor',
        iframeConfig: Ext.apply({}, config, {
          antiCache: this.antiCache,
          cmsLocation: Ext.getDoc().dom.location
        })
      });
      Hippo.ChannelManager.ChannelEditor.ChannelEditor.superclass.constructor.call(this, config);

      this.iframeToHost.subscribe('set-breadcrumb', this.setTitle, this);
      this.iframeToHost.subscribe('channel-changed-in-angular', this._reloadChannels, this);
      this.iframeToHost.subscribe('show-component-properties', this._showComponentProperties, this);
      this.iframeToHost.subscribe('destroy-component-properties-window', this._destroyComponentPropertiesWindow, this);
      this.iframeToHost.subscribe('show-path-picker', this._showPathPicker, this);
      this.iframeToHost.subscribe('show-image-picker', this._showImagePicker, this);
      this.iframeToHost.subscribe('show-link-picker', this._showLinkPicker, this);
      this.iframeToHost.subscribe('show-rich-text-link-picker', this._showRichTextLinkPicker, this);
      this.iframeToHost.subscribe('show-rich-text-image-picker', this._showRichTextImagePicker, this);
      this.iframeToHost.subscribe('open-content', this._openContent, this);
      this.iframeToHost.subscribe('close-content', this._closeContent, this);
      this.iframeToHost.subscribe('show-mask', this._maskSurroundings, this);
      this.iframeToHost.subscribe('remove-mask', this._unmaskSurroundings, this);
      this.iframeToHost.subscribe('edit-alter-ego', this._showAlterEgoEditor, this);
      this.iframeToHost.subscribe('channel-deleted', this._onChannelDeleted, this);
      this.iframeToHost.subscribe('close-channel', this._onCloseChannel, this);

      this.addEvents('show-channel-overview');
    },

    initChannel: function(channelId, initialPath, branchId) {
      this.channelId = channelId;
      this.initialPath = initialPath;
      this.branchId = branchId;
    },

    loadChannel: function() {
      this._destroyComponentPropertiesWindow();
      if (this.channelId) {
        this._getContextPath(this.channelId)
          .when(function (contextPath) {
            var branchedChannelId = this._getBranchedChannelId(this.channelId, this.branchId);
            this.hostToIFrame.publish('load-channel', branchedChannelId, contextPath, this.branchId, this.initialPath);

            // reset the state; the state in the app is leading. When loadChannel is called again,
            // we'll send a reload-channel event instead that reloads the current app state.
            this.initChannel(null, null, null);
          }.bind(this))
          .otherwise(function () {
            console.error('Cannot determine context path of channel "' + this.channelId + '"');
          }.bind(this));
      } else {
        this.hostToIFrame.publish('reload-channel');
      }
    },

    _getContextPath: function(channelId) {
      return new Hippo.Future(function (success, fail) {
        if (this.initialConfig.contextPaths.length === 1) {
          // return the only choice
          success(this.initialConfig.contextPaths[0]);
        }
        this.channelStoreFuture.when(function (config) {
          var channelRecord = config.store.getById(channelId);
          if (!channelRecord) {
            // try the preview version of the channel
            channelRecord = config.store.getById(channelId + '-preview');
          }
          if (channelRecord) {
            success(channelRecord.json.contextPath);
          } else {
            fail();
          }
        }.bind(this));
      }.bind(this));
    },

    _getBranchedChannelId: function(channelId, branchId) {
      if (branchId === 'master') {
        return channelId;
      }
      if (channelId.endsWith('-preview')) {
        return channelId.replace(/-preview$/, '-' + branchId + '-preview');
      }
      return channelId + '-' + branchId;
    },

    /**
     * Called by ChannelEditor.java
     */
    killEditor: function(documentId) {
      this.hostToIFrame.publish('kill-editor', documentId);
    },

    _reloadChannels: function() {
      return new Hippo.Future(function(success) {
        this.channelStoreFuture.when(function (config) {
          config.store.on('load', success, this, { single: true });
          config.store.reload();
        });
      }.bind(this));
    },

    _syncChannel: function() {
      this.hostToIFrame.publish('channel-changed-in-extjs');
      this._reloadChannels();
    },

    _renderComponent: function(componentId, propertiesMap) {
      this.hostToIFrame.publish('render-component', componentId, propertiesMap);
    },

    _closeDialogAndNotifyReloadPage: function(data) {
      this._destroyComponentPropertiesWindow();
      this.hostToIFrame.publish('reload-page', data);
    },

    _destroyComponentPropertiesWindow: function() {
      if (this.componentPropertiesWindow) {
        this.componentPropertiesWindow.destroy();
      }
      delete this.componentPropertiesWindow;
    },

    _openContent: function(uuid, mode) {
      this.fireEvent('open-document', uuid, mode);
    },

    _closeContent: function(uuid) {
      this.fireEvent('close-document', uuid);
    },

    /**
     * Called by ChannelEditor.java
     */
    closeDocumentResult: function(uuid, isClosed) {
      this.hostToIFrame.publish('close-content-result', uuid, isClosed);
    },

    _createComponentPropertiesWindow: function(channel) {
      return new Hippo.ChannelManager.ChannelEditor.ComponentPropertiesWindow({
        id: 'componentPropertiesWindow',
        title: Hippo.ChannelManager.ChannelEditor.Resources['properties-window-default-title'],
        x: 10,
        y: 120,
        width: 525,
        height: 350,
        closable: true,
        collapsible: false,
        constrainHeader: true,
        renderTo: this.el,
        constrain: true,
        hidden: true,
        composerRestMountUrl: channel.contextPath + this.apiUrlPrefix,
        locale: this.locale,
        variantsUuid: this.variantsUuid,
        mountId: channel.mountId,
        listeners: {
          variantDeleted: this._syncChannel,
          deleteComponent: this._deleteComponent,
          propertiesChanged: this._renderComponent,
          componentChanged: this._syncChannel,
          loadFailed: this._closeDialogAndNotifyReloadPage,
          componentLocked: this._closeDialogAndNotifyReloadPage,
          hide: function() {
            this.hostToIFrame.publish('hide-component-properties');
          },
          close: this._destroyComponentPropertiesWindow,
          scope: this
        }
      });
    },

    _deleteComponent: function (componentId) {
      this.componentPropertiesWindow.hide();
      this.hostToIFrame.publish('delete-component', componentId);
    },

    _showComponentProperties: function(selected) {
      if (!this.componentPropertiesWindow) {
        this.componentPropertiesWindow = this._createComponentPropertiesWindow(selected.channel);
      }
      this.componentPropertiesWindow.showComponent(
        selected.component,
        selected.container,
        selected.page
      );
    },

    _showPathPicker: function(field, value, pickerConfig) {
      this.pathPickerField = field;
      Hippo.ChannelManager.ExtLinkPickerFactory.Instance.openPicker(
        value,
        pickerConfig,
        this._onPathPicked.bind(this),
        this._onPathCancelled.bind(this)
      );
    },

    _onPathPicked: function(path, displayValue) {
      this.hostToIFrame.publish('path-picked', this.pathPickerField, path, displayValue);
    },

    _onPathCancelled: function() {
      this.hostToIFrame.publish('path-cancelled', this.pathPickerField);
    },

    _showImagePicker: function(dialogConfig, selectedImage, successCallback, cancelCallback) {
      this._showPicker(dialogConfig, selectedImage, successCallback, cancelCallback, this.initialConfig.imagePickerWicketUrl);
    },

    _showLinkPicker: function(dialogConfig, selectedImage, successCallback, cancelCallback) {
      this._showPicker(dialogConfig, selectedImage, successCallback, cancelCallback, this.initialConfig.linkPickerWicketUrl);
    },

    _showRichTextImagePicker: function(fieldId, dialogConfig, selectedImage, successCallback, cancelCallback) {
      selectedImage.fieldId = fieldId;
      this._showPicker(dialogConfig, selectedImage, successCallback, cancelCallback, this.initialConfig.richTextImagePickerWicketUrl);
    },

    _showRichTextLinkPicker: function(fieldId, dialogConfig, selectedLink, successCallback, cancelCallback) {
      selectedLink.fieldId = fieldId;
      this._showPicker(dialogConfig, selectedLink, successCallback, cancelCallback, this.initialConfig.richTextLinkPickerWicketUrl);
    },

    _showPicker: function(dialogConfig, parameters, successCallback, cancelCallback, wicketUrl) {
      this.pickerSuccessCallback = successCallback;
      this.pickerCancelCallback = cancelCallback;

      parameters.dialogConfig = JSON.stringify(dialogConfig);

      Wicket.Ajax.post({
        u: wicketUrl,
        ep: parameters
      });
    },

    onPicked: function(selected) {
      if (this.pickerSuccessCallback) {
        this.pickerSuccessCallback(selected);
      }
    },

    onPickCancelled: function () {
      if(this.pickerCancelCallback) {
        this.pickerCancelCallback();
      }
    },

    _maskSurroundings: function() {
      $(document.body).append(
        '<div class="channel-editor-mask channel-editor-mask-left"></div>' +
        '<div class="channel-editor-mask channel-editor-mask-bottom"></div>'
      );
    },

    _unmaskSurroundings: function() {
      $(document.body).children('.channel-editor-mask')
        .addClass('channel-editor-mask-removing')
        .delay(400)
        .queue(function() {
          $(this).remove();
        });
    },

    _showAlterEgoEditor: function(mainToolbarHeight) {
      var alterEgoWindowConfig = Hippo.ExtWidgets.getConfig('Hippo.Targeting.AlterEgoWindow'),
        alterEgoWindow = Ext.create(alterEgoWindowConfig);

      // center the alter ego window below the main Angular Material toolbar
      alterEgoWindow.on('afterrender', function () {
        var centeredXY = alterEgoWindow.getEl().getAlignToXY(alterEgoWindow.container, 'c-c');
        alterEgoWindow.setPagePosition(centeredXY[0], mainToolbarHeight);
      }, this);

      alterEgoWindow.on('alterEgoChanged', function () {
        this.hostToIFrame.publish('alter-ego-changed');
      }, this);

      alterEgoWindow.show();
    },

    _onChannelDeleted: function() {
      this._reloadChannels().when(function () {
        this.hostToIFrame.publish('channel-removed-from-overview');
        this.fireEvent('show-channel-overview');
      }.bind(this));
    },

    closeChannel: function() {
      this.hostToIFrame.publish('close-channel');
    },

    _onCloseChannel: function() {
      this.fireEvent('show-channel-overview');
    },

    initComponent: function() {
      Hippo.ChannelManager.ChannelEditor.ChannelEditor.superclass.initComponent.call(this);

      this.channelStoreFuture.when(this._startApp.bind(this));
    },

    _startApp: function() {
      if (this._isDevMode()) {
        this._clearAppState();
      }

      var url = './angular/hippo-cm/index.html';
      url = Ext.urlAppend(url, 'antiCache=' + this.antiCache);
      this.setLocation(url);
    },

    _isDevMode: function() {
      return document.documentElement.classList.contains('wicket-development-mode');
    },

    _clearAppState: function() {
      // The app uses sessionStorage to store state in dev mode, so reloads by Webpack retain the current channel and
      // page. Clear that state so a reload of the CMS (or loading a different CMS) in the same tab won't reuse the
      // state and request non-existing channels or pages.
      delete sessionStorage.channelId;
      delete sessionStorage.channelPath;
      delete sessionStorage.channelBranch;
    }
  });

}(jQuery));
