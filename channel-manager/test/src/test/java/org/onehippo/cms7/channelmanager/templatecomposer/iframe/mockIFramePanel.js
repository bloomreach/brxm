/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
Ext.namespace('Hippo.ChannelManager.TemplateComposer.PageEditor.IFrame');

Hippo.ChannelManager.TemplateComposer.PageEditor.IFrame.hostToIFrame = Hippo.ChannelManager.TemplateComposer.createMessageBus('host-to-iframe');
Hippo.ChannelManager.TemplateComposer.PageEditor.IFrame.iframeToHost = Hippo.ChannelManager.TemplateComposer.createMessageBus('iframe-to-host');

Hippo.ChannelManager.TemplateComposer.PageEditor.IFrame.getPosition = function() {
    return [0, 0];
};

Hippo.ChannelManager.TemplateComposer.PageEditor.IFrame.getTopToolbar = function() {
    return {
        getHeight: function() {
            return 0;
        }
    };
};

Ext.namespace('Ext.dd.DragDropMgr');

Ext.dd.DragDropMgr.handleMouseMove = function() {};
Ext.dd.DragDropMgr.handleMouseUp = function() {};
