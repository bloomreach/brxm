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

    constructor: function(config, cache, oldContext) {

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

        this.resources = config.resources;
        this.previewMode = config.previewMode;
        this.composerMountUrl = config.composerMountUrl;
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.renderHostSubMountPath = config.renderHostSubMountPath;
        this.ignoreRenderHostParameterName = config.ignoreRenderHostParameterName;

        this.iframeResourceCache = cache;

        Hippo.ChannelManager.TemplateComposer.PageContext.superclass.constructor.call(this, config);

        this.addEvents('afterBuildOverlay',
                       'mountChanged',
                       'iFrameInitialized',
                       'iFrameException');

    },

    initialize: function(frm) {
        this._requestHstMetaData( frm.getDocumentURI() ).when(function() {
            this._initializeIFrameHead(frm, this.previewMode).when(function() {
                this._buildOverlay(frm);
                console.info('iFrameInitialized');
                this.fireEvent('iFrameInitialized');
            }.createDelegate(this));
        }.createDelegate(this));
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
                this.refreshIframe();
            }, this);
        }, this);

        return new Hippo.Future(function(onSuccess, onFail) {
            this.stores.toolkit.on('load', function() {
                onSuccess(this.stores.toolkit);
                this.fireEvent('toolkitLoaded', mountId, this.stores.toolkit);
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
                this.refreshIframe();
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

    _requestHstMetaData: function(url) {
        console.log('_requestHstMetaData ' + url);
        return new Hippo.Future(function(onSuccess, onFail) {
            var self = this;
            Ext.Ajax.request({
                method: "HEAD",
                url : url,
                success : function(responseObject) {
                    var data = {
                        url : url,
                        pageId : responseObject.getResponseHeader('HST-Page-Id'),
                        mountId : responseObject.getResponseHeader('HST-Mount-Id'),
                        oldMountId : self.ids.oldMountId,
                        oldSiteId : self.ids.oldSiteId
                    };
                    self.hasPreviewHstConfig = self._getBoolean(responseObject.getResponseHeader('HST-Site-HasPreviewConfig'));
                    if (!self.hasPreviewHstConfig && !self.previewMode) {
                        self.previewMode = true;
                        self.fireEvent('modeChanged', self.previeMode);
                    }

                    console.log('hstMetaDataResponse '+JSON.stringify(data));

                    var futures = [
                        self._initToolkitStore(data.mountId),
                        self._initPageModelStore(data.mountId, data.pageId)
                    ];
                    Hippo.Future.join(futures).when(function() {
                        onSuccess();

                        self.ids.pageUrl = data.url;

                    }).otherwise(onFail);
                },
                failure : function(responseObject) {
                    onFail();
                    self.fireEvent.apply(self, ['fatalIFrameException', { msg: self.resources['hst-meta-data-request-failed']}]);
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
            composerRestMountUrl : this.composerRestMountUrl,
            ignoreRenderHostParameterName: this.ignoreRenderHostParameterName
        });
    },

    _createPageModelStore : function(mountId, pageId) {
        return new Hippo.ChannelManager.TemplateComposer.PageModelStore({
            rootComponentIdentifier: this.rootComponentIdentifier,
            mountId: mountId,
            pageId: pageId,
            composerRestMountUrl: this.composerRestMountUrl,
            ignoreRenderHostParameterName: this.ignoreRenderHostParameterName,
            resources: this.resources,
            listeners: {
                write : {
                    fn: function(store, action, result, res, records) {
                        if (action == 'create') {
                            records = Ext.isArray(records) ? records : [records];
                            for (var i = 0; i < records.length; i++) {
                                var record = records[i];
                                if (record.get('type') == HST.CONTAINERITEM) {
                                    //add id to parent children map
                                    var parentId = record.get('parentId');
                                    var parentIndex = store.findExact('id', parentId);
                                    var parentRecord = store.getAt(parentIndex);
                                    var children = parentRecord.get('children');
                                    children.push(record.get('id'));
                                    parentRecord.set('children', children);
                                }
                            }
                        } else if (action == 'update') {
                            if (!this.isReloading) {
                                this.isReloading = true;
                                store.reload();
                            }
                        }
                    },
                    scope: this
                },
                beforeload: {
                    fn : function(store, options) {
                        this.fireEvent('beforePageModelStoreLoad');
                    },
                    scope: this
                },
                load :{
                    fn : function(store, records, options) {
                        this.isReloading = false;
                    },
                    scope: this
                },
                remove : {
                    fn : function(store, record, index) {

                        if (record.get('type') == HST.CONTAINER) {
                            //remove all children as well
                            Ext.each(record.get('children'), function(id) {
                                var childIndex = store.findExact('id', id);
                                if (childIndex > -1) {
                                    store.removeAt(childIndex);
                                }
                            });
                        } else {
                            //containerItem: unregister from parent
                            var parentRecord = store.getAt(store.findExact('id', record.get('parentId')));
                            if (typeof parentRecord !== 'undefined') {
                                var children = parentRecord.get('children');
                                children.remove(record.get('id'));
                                parentRecord.set('children', children);
                            }
                        }
                        this.fireEvent('componentRemoved', record);
                    },
                    scope : this
                }
            }
        });
    },

    _removeByRecord: function(record) {
        var store = this.stores.pageModel;
        Hippo.Msg.confirm(this.resources['delete-message-title'], this.resources['delete-message'].format(record.get('name')), function(btn, text) {
            if (btn == 'yes') {
                store.remove(record);
            }
        });
    },

    _removeByElement : function(element) {
        var store = this.stores.pageModel;
        var index = store.findExact('id', Ext.fly(element).getAttribute(HST.ATTR.ID));
        this._removeByRecord(store.getAt(index))
    },

    _buildOverlay : function(frm) {
        if (!this.fireEvent('beforeBuildOverlay')) {
            return;
        }
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
        this.fireEvent('afterBuildOverlay');
    }

});