/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

    Hippo.ChannelManager.TemplateComposer.PagesWindow = Ext.extend(Hippo.ChannelManager.TemplateComposer.IFrameWindow, {

        constructor: function(config) {
            Ext.apply(config, {
                title: config.resources['pages-window-title'],
                width: 587,
                height: 517,
                modal: true,
                resizable: false,
                iframeUrl: './angular/pages/index.html',
                iframeConfig: {
                    apiUrlPrefix: config.composerRestMountUrl,
                    debug: config.debug,
                    locale: config.locale,
                    mountId: config.mountId,
                    sitemapId: config.sitemapId,
                    userCanEdit: config.userCanEdit,
                    userIsEditing: config.userIsEditing,
                    antiCache: config.antiCache
                }
            });

            Hippo.ChannelManager.TemplateComposer.PagesWindow.superclass.constructor.call(this, config);
        }

    });

    Ext.reg('Hippo.ChannelManager.TemplateComposer.PagesWindow', Hippo.ChannelManager.TemplateComposer.PagesWindow);

}());
