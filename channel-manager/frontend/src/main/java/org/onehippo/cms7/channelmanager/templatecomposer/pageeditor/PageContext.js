/*
 *  Copyright 2011 Hippo.
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
Ext.namespace('Hippo.ChannelManager.TemplateComposer');

Hippo.ChannelManager.TemplateComposer.PageContext = Ext.extend(Ext.util.Observable, {

    constructor: function(config, cache, oldContext, pageContainer) {

        if (oldContext != null) {
            this.ids = {
                pageUrl : oldContext.ids.pageUrl,
                pageId : oldContext.ids.pageId,
                mountId : oldContext.ids.mountId
            };

            this.stores = {
                toolkit : oldContext.stores.toolkit,
                pageModel : oldContext.stores.pageModel
            };
            this.hasPreviewHstConfig = oldContext.hasPreviewHstConfig;
        } else {
            this.ids = {
                pageUrl : null,
                pageId : null,
                mountId : null
            };

            this.stores = {
                toolkit : null,
                pageModel : null
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

        this.iframeResourceCache = cache;

        Hippo.ChannelManager.TemplateComposer.PageContext.superclass.constructor.call(this, config);

        this.addEvents('mountChanged',
                       'pageContextInitialized');

    },

    initialize: function(frm, canEdit) {
        this.frm = frm;

        this._requestHstMetaData(frm.getDocumentURI(), canEdit).when(function() {
            this._initializeIFrameHead(frm, this.previewMode).when(function() {
                if (canEdit) {
                    this._buildOverlay(frm);
                }
                console.info('pageContextInitialized');
                this.fireEvent('pageContextInitialized');
            }.createDelegate(this));
        }.createDelegate(this)).otherwise(function() {
            this.fireEvent('pageContextInitializationFailed');
        }.createDelegate(this));
    },

    getPageContainer: function() {
        return this.pageContainer;
    },

    selectVariant: function(id, variant) {
        this.frm.sendMessage({id: id, variant: variant}, 'selectVariant');
    },

    _initToolkitStore : function(mountId) {
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
                console.error('Server returned status '+response.status+" for the toolkit store.");
            } else if (type === 'remote') {
                console.error('Error handling the response of the server for the toolkit store. Response is:\n'+response.responseText);
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

    _initPageModelStore : function(mountId, pageId) {
        if (this.ids.pageId === pageId) {
            return new Hippo.Future(function(onSuccess) {
                onSuccess(this.stores.pageModel);
            }.createDelegate(this));
        }

        this.ids.pageId = pageId;
        this.stores.pageModel = this._createPageModelStore(mountId, pageId);
        this.stores.pageModel.on('exception', function(dataProxy, type, action, options, response) {
            if (type === 'response') {
                console.error('Server returned status '+response.status+" for the page store.");
            } else if (type === 'remote') {
                console.error('Error handling the response of the server for the page store. Response is:\n'+response.responseText);
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
        console.log('_requestHstMetaData ' + url);
        return new Hippo.Future(function(onSuccess, onFail) {
            var self = this;
            Ext.Ajax.request({
                method: "HEAD",
                url : url,
                success : function(responseObject) {
                    var pageId = responseObject.getResponseHeader('HST-Page-Id');
                    var mountId = responseObject.getResponseHeader('HST-Mount-Id');

                    self.hasPreviewHstConfig = self._getBoolean(responseObject.getResponseHeader('HST-Site-HasPreviewConfig'));
                    if (!self.hasPreviewHstConfig || !canEdit) {
                        self.previewMode = true;
                    }

                    console.log('hstMetaDataResponse: url:'+url+', pageId:'+pageId+', mountId:'+mountId);

                    if (canEdit) {
                        var futures = [
                            self._initToolkitStore.call(self, mountId),
                            self._initPageModelStore.apply(self, [mountId, pageId])
                        ];
                        Hippo.Future.join(futures).when(function() {
                            onSuccess();
                        }).otherwise(onFail);
                    } else {
                        onSuccess();
                    }
                },
                failure : function(responseObject) {
                    onFail();
                }
            });
        }.createDelegate(this));
    },

    _getBoolean: function(object) {
        if (typeof object === 'undefined' || object === null) {
            return null;
        }
        if (object === true || object === false) {
            return object;
        }
        var str = object.toString().toLowerCase();
        if (str === "true") {
            return true;
        } else if (str === "false") {
            return false
        }
        return null;
    },

    _initializeIFrameHead : function(frm, previewMode) {
        return new Hippo.Future(function(success, fail) {
            this.iframeResourceCache.when(function(iframeResources) {
                var resourceCache = iframeResources.cache;
                var iFrameCssHeadContributions = iframeResources.css;
                for (var i = 0, len = iFrameCssHeadContributions.length; i < len; i++) {
                    var src = iFrameCssHeadContributions[i];
                    var cssContent = resourceCache[src];
                    var frmDocument = frm.getFrameDocument();

                    if (Ext.isIE) {
                        var style = frmDocument.createStyleSheet().cssText = cssContent;
                    } else {
                        var headElements = frmDocument.getElementsByTagName("HEAD");
                        var head;
                        if (headElements.length == 0) {
                            head = frmDocument.createElement("HEAD");
                            frmDocument.appendChild(head);
                        } else {
                            head = headElements[0];
                        }

                        var styleElement = frmDocument.createElement("STYLE");
                        styleElement.setAttribute("type", "text/css");
                        var textNode = frmDocument.createTextNode(cssContent);
                        styleElement.appendChild(textNode);
                        styleElement.setAttribute("title", src);
                        head.appendChild(styleElement);
                    }
                }

                var iFrameJsHeadContributions = iframeResources.js;
                for (var i = 0, len = iFrameJsHeadContributions.length; i < len; i++) {
                    var src = iFrameJsHeadContributions[i];
                    var jsContent = resourceCache[src];
                    (function(src, responseText) {
                        window.setTimeout(function() {
                            frm.writeScript.apply(frm, [responseText, {type: "text/javascript", "title" : src}]);
                        }, 0);
                    })(src, jsContent);
                }

                // remove global jquery references and restore previous 'jQuery' and '$' objects on window scope
                var self = this;
                window.setTimeout(function() {
                    frm.execScript(' jQuery.noConflict(true); ', true);
                    frm.sendMessage({debug: self.debug,
                                     previewMode: previewMode,
                                     resources: self.resources}, 'init');
                    success();
                }, 0);
            }.createDelegate(this));

        }.createDelegate(this));
    },

    _createToolkitStore : function(mountId) {
        return new Hippo.ChannelManager.TemplateComposer.ToolkitStore({
            mountId : mountId,
            composerRestMountUrl : this.composerRestMountUrl
        });
    },

    _createPageModelStore : function(mountId, pageId) {
        return new Hippo.ChannelManager.TemplateComposer.PageModelStore({
            rootComponentIdentifier: this.rootComponentIdentifier,
            mountId: mountId,
            pageId: pageId,
            composerRestMountUrl: this.composerRestMountUrl,
            resources: this.resources
        });
    },

    _buildOverlay : function(frm) {
        console.log('_buildOverlay');
        var self = this;
        frm.sendMessage({
            getName : function(id) {
                var idx = self.stores.pageModel.findExact('id', id);
                if (idx == -1) {
                    return null;
                }
                var record = self.stores.pageModel.getAt(idx);
                return record.get('name');
            }
        }, 'buildoverlay');
        if (!this.previewMode) {
            frm.sendMessage({}, ('showoverlay'));
        }
    }

});