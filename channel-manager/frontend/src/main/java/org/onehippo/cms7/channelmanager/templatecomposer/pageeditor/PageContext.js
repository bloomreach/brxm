/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

    Hippo.ChannelManager.TemplateComposer.PageContext = Ext.extend(Ext.util.Observable, {

        constructor: function(config, cache, oldContext, pageContainer) {

            if (oldContext !== null) {
                this.ids = {
                    pageUrl: oldContext.ids.pageUrl,
                    pageId: oldContext.ids.pageId,
                    mountId: oldContext.ids.mountId
                };

                this.stores = {
                    toolkit: oldContext.stores.toolkit,
                    pageModel: oldContext.stores.pageModel
                };
                this.hasPreviewHstConfig = oldContext.hasPreviewHstConfig;
            } else {
                this.ids = {
                    pageUrl: null,
                    pageId: null,
                    mountId: null
                };

                this.stores = {
                    toolkit: null,
                    pageModel: null
                };
                this.hasPreviewHstConfig = false;
            }

            this.pageContainer = pageContainer;
            this.resources = config.resources;
            this.previewMode = config.previewMode;
            this.contextPath = config.contextPath;
            this.templateComposerContextPath = config.templateComposerContextPath;
            this.composerRestMountUrl = config.templateComposerContextPath + config.composerRestMountPath;
            this.renderPath = config.renderPath;
            this.locked = false;
            this.lockedBy = "";
            this.lockedOn = 0;

            this.iframeResourceCache = cache;

            Hippo.ChannelManager.TemplateComposer.PageContext.superclass.constructor.call(this, config);

            this.addEvents('mountChanged',
                    'pageContextInitialized');

        },

        initialize: function(canEdit) {
            var iframeLocation = Hippo.ChannelManager.TemplateComposer.IFramePanel.Instance.getLocation();

            this._requestHstMetaData(iframeLocation, canEdit).when(function() {
                this._initializeIFrameHead(this.previewMode).when(function() {
                    if (canEdit) {
                        this._buildOverlay();
                    }
                    console.info('pageContextInitialized');
                    this.fireEvent('pageContextInitialized');
                }.createDelegate(this));
            }.createDelegate(this)).otherwise(function(error) {
                this.fireEvent('pageContextInitializationFailed', error);
            }.createDelegate(this));
        },

        getPageContainer: function() {
            return this.pageContainer;
        },

        selectVariant: function(id, variant) {
            Hippo.ChannelManager.TemplateComposer.IFramePanel.Instance.hostToIFrame.publish('selectVariant', id, variant);
        },

        _initToolkitStore: function(mountId) {
            if (this.ids.mountId === mountId) {
                return new Hippo.Future(function(onSuccess) {
                    onSuccess(this.stores.toolkit);
                }.createDelegate(this));
            }

            this.fireEvent('mountChanged', {
                oldMountId: this.mountId,
                mountId: mountId,
                previewMode: this.previewMode
            });

            this.ids.mountId = mountId;
            this.ids.pageId = null;

            this.stores.toolkit = this._createToolkitStore(mountId);
            this.stores.toolkit.on('exception', function(dataProxy, type, action, options, response) {
                if (type === 'response') {
                    console.error('Server returned status ' + response.status + " for the toolkit store.");
                } else if (type === 'remote') {
                    console.error('Error handling the response of the server for the toolkit store. Response is:\n' + response.responseText);
                }
                Hippo.Msg.alert(this.resources['toolkit-store-error-message-title'], this.resources['toolkit-store-error-message'], function(id) {
                    this.pageContainer.refreshIframe();
                }, this);
            }, this);

            return new Hippo.Future(function(onSuccess, onFail) {
                this.stores.toolkit.on('load', function() {
                    onSuccess(this.stores.toolkit);
                }, this, { single: true });
                this.stores.toolkit.on('exception', function() {
                    onFail();
                }, this, { single: true });
                this.stores.toolkit.load();
            }.createDelegate(this));
        },

        _initPageModelStore: function(mountId, pageId) {
            if (this.ids.pageId === pageId) {
                return new Hippo.Future(function(onSuccess) {
                    onSuccess(this.stores.pageModel);
                }.createDelegate(this));
            }

            this.ids.pageId = pageId;
            this.stores.pageModel = this._createPageModelStore(mountId, pageId);
            this.stores.pageModel.on('exception', function(dataProxy, type, action, options, response) {
                if (type === 'response') {
                    console.error('Server returned status ' + response.status + " for the page store.");
                } else if (type === 'remote') {
                    console.error('Error handling the response of the server for the page store. Response is:\n' + response.responseText);
                }
                Hippo.Msg.alert(this.resources['page-store-error-message-title'], this.resources['page-store-error-message'], function(id) {
                    this.pageContainer.refreshIframe();
                }, this);
            }, this);

            return new Hippo.Future(function(onSuccess, onFail) {
                this.stores.pageModel.on('load', function() {
                    onSuccess(this.stores.pageModel);
                }, this, { single: true });
                this.stores.pageModel.on('exception', function() {
                    onFail();
                }, this, { single: true });
                this.stores.pageModel.load();
            }.createDelegate(this));
        },

        _requestHstMetaData: function(url, canEdit) {
            // IE stores document.location.href unencoded, which causes the Ajax call to fail when the URL contains
            // special unicode characters. Encode the URL to avoid this.
            var encodedUrl = Ext.isIE ? encodeURI(url) : url;
            console.log('_requestHstMetaData ' + encodedUrl);

            return new Hippo.Future(function(onSuccess, onFail) {
                var self = this;
                Ext.Ajax.request({
                    method: "HEAD",
                    url: encodedUrl,
                    success: function(responseObject) {
                        var pageId, mountId, lockedBy, futures;
                        pageId = responseObject.getResponseHeader('HST-Page-Id');
                        mountId = responseObject.getResponseHeader('HST-Mount-Id');
                        if (pageId === undefined || mountId === undefined) {
                            onFail('No page and/or mount information found');
                            return;
                        }

                        self.renderedVariant = responseObject.getResponseHeader('HST-Render-Variant');
                        self.hasPreviewHstConfig = self._getBoolean(responseObject.getResponseHeader('HST-Site-HasPreviewConfig'));
                        if (!self.hasPreviewHstConfig || !canEdit) {
                            self.previewMode = true;
                        }

                        lockedBy = responseObject.getResponseHeader('HST-Mount-LockedBy');
                        if (lockedBy !== undefined) {
                            self.locked = self.pageContainer.cmsUser !== lockedBy;
                            self.lockedBy = lockedBy;
                            self.lockedOn = parseInt(responseObject.getResponseHeader('HST-Mount-LockedOn'), 10);
                            self.unlockable = responseObject.getResponseHeader('HST-Mount-Unlockable') === 'true' ? true : false;
                        } else {
                            self.locked = false;
                            self.lockedBy = "";
                            self.lockedOn = 0;
                            self.unlockable = false;
                        }

                        console.log('hstMetaDataResponse: url:' + encodedUrl + ', pageId:' + pageId + ', mountId:' + mountId);

                        if (canEdit) {
                            futures = [
                                self._initToolkitStore.call(self, mountId),
                                self._initPageModelStore.apply(self, [mountId, pageId])
                            ];
                            Hippo.Future.join(futures).when(function() {
                                onSuccess();
                            }).otherwise(function() {
                                onFail("Failed to initialize page model for url '" + encodedUrl + "'");
                            });
                        } else {
                            onSuccess();
                        }
                    },
                    failure: function() {
                        onFail("HST-Meta-Data request failed for URL '" + encodedUrl + "'");
                    }
                });
            }.createDelegate(this));
        },

        _getBoolean: function(object) {
            var str;
            if (typeof object === 'undefined' || object === null) {
                return null;
            }
            if (object === true || object === false) {
                return object;
            }
            str = object.toString().toLowerCase();
            if (str === "true") {
                return true;
            } else if (str === "false") {
                return false;
            }
            return null;
        },

        _initializeIFrameHead: function(previewMode) {
            var iframe = Hippo.ChannelManager.TemplateComposer.IFramePanel.Instance;

            return new Hippo.Future(function(success, fail) {
                this.iframeResourceCache.when(function(iframeResources) {
                    var resourceCache, headFragment, self;

                    resourceCache = iframeResources.cache;
                    headFragment = iframe.createHeadFragment();
                    self = this;

                    iframe.iframeToHost.subscribeOnce('iframeloaded', function() {
                        iframe.hostToIFrame.publish('init', {
                            debug: self.debug,
                            previewMode: previewMode,
                            resources: self.resources
                        });
                        success();
                    }, self);

                    Ext.each(iframeResources.css, function(src) {
                        headFragment.addStyleSheet(resourceCache[src], src);
                    });
                    Ext.each(iframeResources.js, function(src) {
                        var jsContent = resourceCache[src];
                        headFragment.addScript(jsContent, src);
                    });
                    headFragment.flush();

                }.createDelegate(this));

            }.createDelegate(this));
        },

        _createToolkitStore: function(mountId) {
            return new Hippo.ChannelManager.TemplateComposer.ToolkitStore({
                mountId: mountId,
                composerRestMountUrl: this.composerRestMountUrl
            });
        },

        _createPageModelStore: function(mountId, pageId) {
            return new Hippo.ChannelManager.TemplateComposer.PageModelStore({
                rootComponentIdentifier: this.rootComponentIdentifier,
                mountId: mountId,
                pageId: pageId,
                composerRestMountUrl: this.composerRestMountUrl,
                resources: this.resources
            });
        },

        _buildOverlay: function() {
            console.log('_buildOverlay');
            Hippo.ChannelManager.TemplateComposer.IFramePanel.Instance.hostToIFrame.publish('buildoverlay');
        }

    });

}());