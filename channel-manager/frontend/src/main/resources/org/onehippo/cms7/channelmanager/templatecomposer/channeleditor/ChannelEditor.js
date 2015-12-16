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
        iframeConfig: Ext.apply({}, config, {
          antiCache: this.antiCache
        })
      });
      Hippo.ChannelManager.TemplateComposer.ChannelEditor.superclass.constructor.call(this, config);
    },

    loadChannel: function(channelId) {

      this.channelStoreFuture.when(function(config) {
        var channelRecord = config.store.getById(channelId);
        this.setTitle(channelRecord.get('name'));
        this.hostToIFrame.publish('load-channel', channelRecord.json);
      }.bind(this));
    },

    initComponent: function() {
      Hippo.ChannelManager.TemplateComposer.ChannelEditor.superclass.initComponent.call(this);

      this.channelStoreFuture.when(function(config) {

        // start NG app
        var url = './angular/hippo-cm/index.html';
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
