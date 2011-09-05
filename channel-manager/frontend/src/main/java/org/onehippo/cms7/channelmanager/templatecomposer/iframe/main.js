/*
 *  Copyright 2010 Hippo.
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
"use strict";
(function($) {
    var jQuery = $;
    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame');

    var Main = function() {};

    Main.prototype = {

        init: function(options) {
            var manager = new Hippo.ChannelManager.TemplateComposer.IFrame.UI.Manager(options);

            onhostmessage(function(msg) {
                manager.getOverlay().show();
                $('.empty-container-placeholder').show();
                manager.requestSync();
                manager.sync();
                return false;
            }, this, false, 'showoverlay')

            onhostmessage(function(msg) {
                manager.getOverlay().hide();
                $('.empty-container-placeholder').hide();
                return false;
            }, this, false, 'hideoverlay');

            //register to listen to iframe-messages
            onhostmessage(function(msg) {
                manager.select(msg.data.element);
                return false;
            }, this, false, 'select');

            onhostmessage(function(msg) {
                manager.deselect(msg.data.element);
                return false;
            }, this, false, 'deselect');

            onhostmessage(function(msg) {
                manager.add(msg.data.element, msg.data.parentId);
                return false;
            }, this, false, 'add');

            onhostmessage(function(msg) {
                manager.remove(msg.data.element);
                return false;
            }, this, false, 'remove');

            onhostmessage(function(msg) {
                manager.highlight(msg.data.groups);
                return false;
            }, this, false, 'highlight');

            onhostmessage(function(msg) {
                manager.unhighlight(msg.data.groups);
                return false;
            }, this, false, 'unhighlight');

            onhostmessage(function(msg) {
                manager.createContainers();
                var facade = msg.data;
                manager.updateSharedData(facade);
                return false;
            }, this, false, 'sharedata');

            onhostmessage(function(msg) {
                manager.requestSync();
                manager.sync();
                return false;
            }, this, false, 'resize');

            this.manager = manager;
            sendMessage({preview: options.preview}, "afterinit");
        },

        isDebug: function() {
            return this.debug;
        },

       die: function(msg) {
            if(Hippo.ChannelManager.TemplateComposer.IFrame.Main.isDebug()) {//global reference for scope simplicity
                console.error(msg);
            } else {
                sendMessage({msg: msg}, "iframeexception");
            }
       }
    };

    Hippo.ChannelManager.TemplateComposer.IFrame.Main = new Main();
    onhostmessage(function(msg) {
        console.log('IFrame Init Message '+JSON.stringify(msg.data));
        Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.setResources(msg.data.resources);
        Hippo.ChannelManager.TemplateComposer.IFrame.UI.SurfAndEdit.setResources(msg.data.resources);
        Hippo.ChannelManager.TemplateComposer.IFrame.Main.init(msg.data);
    }, this, false, 'init');

})(jQuery);

// remove global jquery references and restore previous 'jQuery' and '$' objects on window scope
jQuery.noConflict(true);