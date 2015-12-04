/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

  Hippo.ChannelManager.TemplateComposer.ChannelEditor = Ext.extend(Hippo.IFramePanel, {

    constructor: function(config) {
      this.title = null;
      this.antiCache = new Date().getTime();

      Ext.apply(config, {
        cls: 'qa-channel-editor',
        iframeConfig: {
          debug: config.debug,
          locale: config.locale
        }
      });
      Hippo.ChannelManager.TemplateComposer.ChannelEditor.superclass.constructor.call(this, config);
    },

    browseTo: function(data) {
      this.channelStoreFuture.when(function(config) {
        var isEditMode, record, renderHost;

        if (Ext.isDefined(data.isEditMode)) {
          isEditMode = data.isEditMode;
        } else if (Ext.isDefined(data.channelId) && data.channelId !== this.channelId) {
          isEditMode = false;
        } else {
          isEditMode = this.pageContainer ? !this.pageContainer.previewMode : false;
        }

        this.channelId = data.channelId || this.channelId;
        record = config.store.getById(this.channelId);
        this.title = record.get('name');
        this.channel = record.data;
        this.hstMountPoint = record.get('hstMountPoint');
        this.contextPath = record.get('contextPath') || data.contextPath || this.contextPath;
        this.cmsPreviewPrefix = record.get('cmsPreviewPrefix') || data.cmsPreviewPrefix || this.cmsPreviewPrefix;
        this.renderPathInfo = data.renderPathInfo || this.renderPathInfo || record.get('mountPath');
        renderHost = record.get('hostname');
        console.log('Show iframe for channel "%s", contextPath: %s, cmsPreviewPrefix: %s, renderPathInfo: %s, ' +
          'renderHost: %s, editMode: %s',
          this.title, this.contextPath, this.cmsPreviewPrefix, this.renderPathInfo, renderHost, isEditMode);

        this.initialConfig.iframeConfig = {
          debug: this.debug,
          locale: this.locale,
          antiCache: this.antiCache
        };

        //this.initComposer(isEditMode);
      }.bind(this));
    },

    initComponent: function() {
      Hippo.ChannelManager.TemplateComposer.ChannelEditor.superclass.initComponent.call(this);

      this.channelStoreFuture.when(function(config) {

        // start NG app
        var url = './angular/cmng/index.html';
        url = Ext.urlAppend(url, 'parentExtIFramePanelId=' + this.getId());
        url = Ext.urlAppend(url, 'antiCache=' + this.antiCache);
        this.setLocation(url);

        config.store.on('load', function() {
          if (this.channelId) {
            var channelRecord = config.store.getById(this.channelId);

            this.channel = channelRecord.data;
            // TODO: more?
            console.log('update this.channel to', this.channel);

          }
        }, this);
      }.bind(this));
    }
  });

}());
