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
Ext.namespace('Hippo.ChannelManager.TemplateComposer');

Hippo.ChannelManager.TemplateComposer.PageContainer = Ext.extend(Ext.util.Observable, {

    constructor : function(config) {

        this.ids = {
            pageUrl : null,
            pageId : null,
            mountId : null
        };

        this.stores = {
            toolkit : null,
            pageModel : null
        };

        this.resources = config.resources;

        if (config.composerMountUrl.lastIndexOf('/') !== config.composerMountUrl.length - 1) {
            config.composerMountUrl = config.composerMountUrl + '/';
        }
        this.composerMountUrl = config.composerMountUrl;
        this.composerRestMountUrl = config.composerRestMountUrl;

        if (config.renderHostSubMountPath && config.renderHostSubMountPath.indexOf('/') === 0) {
            config.renderHostSubMountPath = config.renderHostSubMountPath.substr(1);
        }
        this.restHostSubMountPath = config.renderHostSubMountPath;
        this.renderHost = config.renderHost;

        this.ignoreRenderHostParameterName = config.ignoreRenderHostParameterName;
        this.hasPreviewHstConfig = false;
        this.composerInitialized = false;
        this.pageModelStoreLoaded = false;
        this.iframeInitialized = false;
        this.iframeDOMReady = false;

        this.iFrameErrorPage = config.iFrameErrorPage;
        this.initialHstConnectionTimeout = config.initialHstConnectionTimeout;
        this.iFrameJsHeadContributions = config.iFrameJsHeadContributions;
        this.iFrameCssHeadContributions = config.iFrameCssHeadContributions;

        this.addEvents('afterBuildOverlay',
                       'beforePageIdChange', 'beforeMountIdChange',
                       'beforeIFrameDOMReady', 'afterIFrameDOMReady',
                       'beforePageModelStoreLoad', 'pageModelStoreLoaded',
                       'beforeRequestHstMetaData', 'beforeHstMetaDataResponse', 'afterHstMetaDataResponse',
                       'iFrameInitialized',
                       'hasPreviewHstConfigChanged',
                       'iFrameException');

        this.iframeResourceCache = new Hippo.Future(function(success, fail) {
            this._populateIFrameResourceCache(success, fail);
        }.createDelegate(this));

        Hippo.ChannelManager.TemplateComposer.PageContainer.superclass.constructor.call(this, config);

        Hippo.ChannelManager.TemplateComposer.Container = this;

        this.on('beforePageModelStoreLoad', function(data) {
            console.log('beforePageModelStoreLoad');
            this.pageModelStoreLoaded = false;
        }, this);

        this.on('pageModelStoreLoaded', function(data) {
            console.log('pageModelStoreLoaded');
            this.pageModelStoreLoaded = true;
        }, this);

        this.on('beforeInitComposer', function() {
            this.pageModelStoreLoaded = false;
            this.hasPreviewHstConfig = false;
            this.previewMode = true;
            this.ids.pageId = null;
            this.ids.mountId = null;
            this.ids.pageUrl = null;
            this._resetIFrameState();
        }, this);

        this.on('afterInitComposer', function() {
            this.composerInitialized = true;
        }, this);

        this.on('beforeIFrameDOMReady', function() {
            console.log('beforeIFrameDOMReady');
            this.iframeDOMReady = false;
            this.iframeInitialized = false;
        }, this);

        this.on('afterIFrameDOMReady', function() {
            console.log('afterIFrameDOMReady');
            this.iframeDOMReady = true;
        }, this);

        this.on('afterHstMetaDataResponse', function() {
            console.log('afterHstMetaDataResponse');
        }, this);

        this.on('beforeInitializeIFrameHead', function() {
            this.initializingIFrameHead = true;
            console.log('beforeInitializeIFrameHead');
        }, this);

        this.on('iFrameInitialized', function() {
            console.log('onIFrameInitialized');
            this.initializingIFrameHead = false;
            this.iframeInitialized = true;
        }, this);

        this.on('fatalIFrameException', function(data) {
            var iFrame = Ext.getCmp('Iframe');
            var frm = iFrame.getFrame();
            if (frm !== null && data.msg) {
                this.on('afterIFrameDOMReady', function () {
                    frm.execScript('setErrorMessage(\''+data.msg+'\');', true);
                }, this, {single : true});
                frm.isReset = false;
                frm.setSrc(this.iFrameErrorPage);
            }
        }, this);

    },

    //Keeps the session alive every minute
    _keepAlive : function() {
        Ext.Ajax.request({
            url: this.composerRestMountUrl + 'cafebabe-cafe-babe-cafe-babecafebabe./keepalive?'+this.ignoreRenderHostParameterName+'=true',
            success: function () {
                // Do nothing
            }
        });
    },

    _initToolkitStore : function(mountId) {
        this.stores.toolkit = this._createToolkitStore(mountId);
        this.stores.toolkit.on('exception', function(dataProxy, type, action, options, response, arg) {
            if (type === 'response') {
                console.error('Server returned status '+response.status+" for the toolkit store.");
            } else if (type === 'remote') {
                console.error('Error handling the response of the server for the toolkit store. Response is:\n'+response.responseText);
            }
            Hippo.Msg.alert(this.resources['toolkit-store-error-message-title'], this.resources['toolkit-store-error-message'], function(id) {
                this.initComposer();
            }, this);
        }, this);
        this.stores.toolkit.load();

        this.fireEvent('toolkitLoaded', mountId, this.stores.toolkit);
    },

    _initPageModelStore : function(mountId, pageId) {
        this.stores.pageModel = this._createPageModelStore(mountId, pageId);
        this.stores.pageModel.on('exception', function(dataProxy, type, action, options, response, arg) {
            if (type === 'response') {
                console.error('Server returned status '+response.status+" for the page store.");
            } else if (type === 'remote') {
                console.error('Error handling the response of the server for the page store. Response is:\n'+response.responseText);
            }
            Hippo.Msg.alert(this.resources['page-store-error-message-title'], this.resources['page-store-error-message'], function(id) {
                this.initComposer();
            }, this);
        }, this);
        this.stores.pageModel.load();
    },

    _populateIFrameResourceCache : function(resourcesLoaded, loadFailed) {
        var cache = [];
        var self = this;
        // clone array with concat()
        var queue = this.iFrameCssHeadContributions.concat().concat(this.iFrameJsHeadContributions);
        var futures = [];
        for (var i = 0; i < queue.length; i++) {
            var src = queue.shift();
            futures[i] = new Hippo.Future(function(success, failure) {
                Ext.Ajax.request({
                    url : src,
                    method : 'GET',
                    success : function(result, request) {
                        cache[src] = result.responseText;
                        success();
                    },
                    failure : function(result, request) {
                        self.fireEvent.apply(self, ['fatalIFrameException', {msg : self.resources['pre-cache-iframe-resources-exception'].format(src)}]);
                        failure();
                    }
                });
            })
        }
        Hippo.Future.join(futures).when(
                function() {
                    resourcesLoaded(cache);
                }).otherwise(function() {
                    loadFailed();
                });
    },

    // FIXME: static method should be extracted to separate singleton
    browseTo : function (renderHostSubMountPath, renderHost) {
        var instance = Hippo.ChannelManager.TemplateComposer.Container;
        if (renderHostSubMountPath && renderHostSubMountPath.indexOf('/') === 0) {
            instance.renderHostSubMountPath = renderHostSubMountPath.substr(1);
        } else {
            instance.renderHostSubMountPath = renderHostSubMountPath;
        }
        instance.renderHost = renderHost;
        instance.initComposer.call(instance);
    },

    initComposer : function() {
        if (!(this.renderHostSubMountPath && this.renderHost)) {
            return;
        }

        this.fireEvent('beforeInitComposer');
        this.on('iFrameInitialized', function() {
            this.fireEvent('afterInitComposer');
        }, this, {single : true});

        this._initIFrameListeners();

        var retry = this.initialHstConnectionTimeout;
        var me = this;
        // do initial handshake with CmsSecurityValve of the composer mount and
        // go ahead with the actual host which we want to edit (for which we need to be authenticated)
        var composerMode = function(callback) {
            Ext.Ajax.request({
                url: me.composerRestMountUrl + 'cafebabe-cafe-babe-cafe-babecafebabe./composermode/'+me.renderHost+'/?'+me.ignoreRenderHostParameterName+'=true',
                success: callback,
                failure: function(exceptionObject) {
                    if (exceptionObject.isTimeout) {
                        retry = retry - Ext.Ajax.timeout;
                    }
                    if (retry > 0) {
                        retry = retry - Ext.Ajax.timeout;
                        window.setTimeout(function() {
                            composerMode(callback);
                        }, Ext.Ajax.timeout);
                    } else {
                        Hippo.Msg.hide();
                        Hippo.Msg.confirm(me.resources['hst-timeout-message-title'], me.resources['hst-timeout-message'], function(id) {
                            if (id === 'yes') {
                                retry = me.initialHstConnectionTimeout;
                                Hippo.Msg.wait(me.resources['loading-message']);
                                composerMode(callback);
                            } else {
                                me.fireEvent.apply(me, ['fatalIFrameException', {msg : me.resources['hst-timeout-iframe-exception']}]);
                            }
                        });
                    }
                }
            });
        };
        composerMode(function() {
            var iFrame = Ext.getCmp('Iframe');
            iFrame.frameEl.isReset = false; // enable domready get's fired workaround, we haven't set defaultSrc on the first place
            iFrame.setSrc(me.composerMountUrl + me.renderHostSubMountPath);

            // keep session active
            Ext.TaskMgr.start({
                run: me._keepAlive,
                interval: 60000,
                scope: me
            });
        });
    },

    _initIFrameListeners : function() {
        var iFrame = Ext.getCmp('Iframe');
        iFrame.purgeListeners();
        iFrame.on('message', this.handleFrameMessages, this);
        iFrame.on('documentloaded', function(frm) {
            if (Ext.isSafari || Ext.isChrome) {
                this._onIframeDOMReady(frm);
            }
        }, this);
        iFrame.on('domready', function(frm) {
            // Safari && Chrome report a DOM ready event, but js is not yet fully loaded, resulting
            // in 'undefined' errors.
            if (Ext.isGecko || Ext.isIE) {
                this._onIframeDOMReady(frm);
            }
        }, this);
        iFrame.on('exception', function(frm, e) {
            console.error(e);
        }, this);
        iFrame.on('resize', function() {
            this.sendFrameMessage({}, 'resize');
        }, this);
    },

    refreshIframe : function() {
        console.log('refreshIframe');
        this._resetIFrameState();
        var iframe = Ext.getCmp('Iframe');
        var frame = iframe.getFrame();
        var window = frame.getWindow();
        var scrollSave = {x: window.pageXOffset, y: window.pageYOffset};
        this.on('beforeIFrameDOMReady', function() {
            window.scrollBy(scrollSave.x, scrollSave.y);
        }, this, {single : true});
        iframe.setSrc(iframe.getFrameDocument().location.href); //following links in the iframe doesn't set iframe.src..
    },

    _resetIFrameState : function() {
        this.iframeDOMReady = false;
        this.iframeInitialized = false;
    },

    _onIframeDOMReady : function(frm) {
        this.fireEvent('beforeIFrameDOMReady');
        this.frm = frm;

        // TODO implement pattern similar to promises to remove the boilerplate code

        // snapshot of previewMode for the closures
        var previewMode = this.previewMode;

        this.on('beforeMountIdChange', function(data) {
            console.log('beforeMountIdChange, previewMode: '+previewMode);
            if (!previewMode) {
                this._initToolkitStore(data.mountId);
            } else {
                var initToolkitStoreClosure = function() {
                    this._initToolkitStore(data.mountId);
                };
                this.on('beforeMountIdChange', function() {
                    this.removeListener('beforeChangeModeTo', initToolkitStoreClosure, this);
                }, this, {single: true});
                this.on('beforeChangeModeTo', initToolkitStoreClosure, this, {single : true});
            }
        }, this, {single: true});

        this.on('beforePageIdChange', function(data) {
            console.log('beforePageIdChange, previewMode: '+previewMode);
            this.pageModelStoreLoaded = false;
            if (!previewMode) {
                this._initPageModelStore(data.mountId, data.pageId);
            } else {
               var initPageModelStoreClosure = function(changeModeData) {
                   if (!changeModeData.previewMode && !changeModeData.hasPreviewHstConfig) {
                       // if iframe gets refreshed due to creation of hst preview config,
                       // prevent loading. Store gets loaded afterwards with refresh of iframe which
                       // triggers beforePageIdChange again because the page uuid changed
                       return;
                   }
                   this._initPageModelStore(data.mountId, data.pageId);
               };
               this.on('beforePageIdChange', function() {
                   this.removeListener('beforeChangeModeTo', initPageModelStoreClosure, this);
               }, this, {single: true});
               this.on('beforeChangeModeTo', initPageModelStoreClosure, this, {single : true});
            }
        }, this, {single: true});

        var buildOverlayIfIFrameInitializedAndPageModelStoreLoaded = function() {
            var buildOverlayIfPageModelStoreLoaded = function() {
                console.log('buildOverlayIfPageModelStoreLoaded');
                if (this.pageModelStoreLoaded) {
                    console.log('pageModelStoreLoaded true, buildOverlay');
                    this.buildOverlay.call(this);
                } else {
                    console.log('pageModelStoreLoaded false, schedule buildOverlay with pageModelStoreLoad');
                    this.on('beforeIFrameDOMReady', function() {
                        this.removeListener('pageModelStoreLoaded', this.buildOverlay, this);
                    }, this, {single: true});
                    this.on('pageModelStoreLoaded', this.buildOverlay, this, {single: true});
                }
            };

            this.on('beforeIFrameDOMReady', function() {
                this.removeListener('afterHstMetaDataResponse', buildOverlayIfPageModelStoreLoaded, this);
            }, this, {single: true});
            this.on('afterHstMetaDataResponse', buildOverlayIfPageModelStoreLoaded, this, {single: true});
        };

        this.on('beforeIFrameDOMReady', function() {
            this.removeListener('iFrameInitialized', buildOverlayIfIFrameInitializedAndPageModelStoreLoaded, this);
        }, this, {single : true});
        this.on('iFrameInitialized', buildOverlayIfIFrameInitializedAndPageModelStoreLoaded, this, {single : true});

        this._requestHstMetaData( { url: frm.getDocumentURI() } );

        this._initializeIFrameHead(frm, previewMode);

        this.fireEvent('afterIFrameDOMReady');
    },

    _initializeIFrameHead : function(frm, previewMode) {
        this.fireEvent('beforeInitializeIFrameHead');

        var func = function(resourceCache) {
            for (var i = 0, len = this.iFrameCssHeadContributions.length; i < len; i++) {
                var src = this.iFrameCssHeadContributions[i];
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

            for (var i = 0, len = this.iFrameJsHeadContributions.length; i < len; i++) {
                var src = this.iFrameJsHeadContributions[i];
                var jsContent = resourceCache[src];
                (function(src, responseText) {
                    window.setTimeout(function() {
                        frm.writeScript.apply(frm, [responseText, {type: "text/javascript", "title" : src}]);
                    }, 0);
                })(src, jsContent);
            }

            // remove global jquery references and restore previous 'jQuery' and '$' objects on window scope
            window.setTimeout(function() {
                frm.execScript(' jQuery.noConflict(true); ', true);
            }, 0);

            var self = this;
            window.setTimeout(function() {
                frm.sendMessage({debug: self.debug,
                                 previewMode: previewMode,
                                 resources: self.resources}, 'init');
                self.fireEvent.call(self, 'afterInitializeIFrameHead');
            }, 0);
        };

        this.iframeResourceCache.when(function(cache) { func.call(this, cache) }.createDelegate(this));
    },

    _requestHstMetaData: function(options) {
        this.fireEvent('beforeRequestHstMetaData');
        console.log('_requestHstMetaData' + JSON.stringify(options));
        var self = this;
        Ext.Ajax.request({
            method: "HEAD",
            url : options.url,
            success : function(responseObject) {
                if (options.url !== self.frm.getDocumentURI()) {
                    // response is out of date
                    return;
                }
                var data = {
                    url : options.url,
                    oldUrl : self.ids.pageUrl,
                    pageId : responseObject.getResponseHeader('HST-Page-Id'),
                    oldPageId : self.ids.pageId,
                    mountId : responseObject.getResponseHeader('HST-Mount-Id'),
                    oldMountId : self.ids.oldMountId,
                    siteId : responseObject.getResponseHeader('HST-Site-Id'),
                    oldSiteId : self.ids.oldSiteId,
                    hasPreviewHstConfig : self._getBoolean(responseObject.getResponseHeader('HST-Site-HasPreviewConfig'))
                };

                self.fireEvent.apply(self, ['beforeHstMetaDataResponse', data]);
                console.log('hstMetaDataResponse '+JSON.stringify(data));

                if (data.mountId != self.ids.mountId) {
                    if (self.fireEvent.apply(self, ['beforeMountIdChange', data])) {
                        self.ids.mountId = data.mountId;
                    }
                }

                if (data.pageId != self.ids.pageId) {
                    if (self.fireEvent.apply(self, ['beforePageIdChange', data])) {
                        self.ids.pageId = data.pageId;
                    }
                }

                if (data.url !== self.ids.pageUrl) {
                    if (self.fireEvent.apply(self, ['beforePageUrlChange', data])) {
                        console.log('set pageUrl to '+data.url);
                        self.ids.pageUrl = data.url;
                    }
                }

                if (self.hasPreviewHstConfig !== data.hasPreviewHstConfig) {
                    self.hasPreviewHstConfig = data.hasPreviewHstConfig;
                    self.fireEvent.apply(self, ['hasPreviewHstConfigChanged', data]);
                }

                self.fireEvent.apply(self, ['afterHstMetaDataResponse', data]);
            },
            failure : function(responseObject) {
                self.fireEvent.apply(self, ['fatalIFrameException', { msg: self.resources['hst-meta-data-request-failed']}]);
            }
        });
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

    isHstMetaDataLoaded : function() {
        console.log('isHstMetaDataLoaded '+this.ids.pageUrl+', '+this.frm.getDocumentURI());
        return this.ids.pageUrl === this.frm.getDocumentURI();
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
                            store.reload();
                            this.isReloading = true;
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
                        this.fireEvent('pageModelStoreLoaded');
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

    buildOverlay : function() {
        if (!this.fireEvent('beforeBuildOverlay')) {
            return;
        }
        console.log('buildOverlay');
        var self = this;
        this.sendFrameMessage({
            getName : function(id) {
                var idx = self.stores.pageModel.findExact('id', id);
                if (idx == -1) {
                    return null;
                }
                var record = self.stores.pageModel.getAt(idx);
                return record.get('name');
            }
        }, 'buildoverlay');
        this.fireEvent('afterBuildOverlay');
    },

    findElement: function(id) {
        var frameDoc = Ext.getCmp('Iframe').getFrameDocument();
        var el = frameDoc.getElementById(id);
        return el;
    },

    _onRearrangeContainer: function(id, children) {
        var self = this;
        window.setTimeout(function() {
            try {
                var recordIndex = self.stores.pageModel.findExact('id', id); //should probably do this through the selectionModel
                var record = self.stores.pageModel.getAt(recordIndex);
                record.set('children', children);
                console.log('_onRearrangeContainer '+id+', children: '+children);
                record.commit();
            } catch (exception) {
                console.error('_onRearrangeContainer '+exception);
            }
        }, 0);
    },

    /**
     * ContextMenu provider
     */
    _getMenuActions : function(record, selected) {
        var actions = [];
        var store = this.stores.pageModel;
        var type = record.get('type');
        if (type == HST.CONTAINERITEM) {
            actions.push(new Ext.Action({
                text: this.resources['context-menu-action-delete'],
                handler: function() {
                    this._removeByRecord(record)
                },
                scope: this
            }));
        }
        var children = record.get('children');
        if (type == HST.CONTAINER && children.length > 0) {
            actions.push(new Ext.Action({
                text: this.resources['context-menu-delete-items'],
                handler: function() {
                    var msg = this.resources['context-menu-delete-items-message'].format(children.length);
                    Hippo.Msg.confirm(this.resources['context-menu-delete-items-message-title'], msg, function(btn, text) {
                        if (btn == 'yes') {
                            var r = [children.length];
                            Ext.each(children, function(c) {
                                r.push(store.getAt(store.findExact('id', c)));
                            });
                            //it seems that calling store.remove(r) will end up re-calling the destroy api call for
                            //all previous items in r.. maybe a bug, for now do a loop
                            Ext.each(r, store.remove, store);
                            //store.remove(r);
                        }
                    });
                },
                scope: this
            }));
        }
        return actions;
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

    _handleEdit : function(uuid) {
        this.fireEvent('edit-document', uuid);
    },

    _handleReceivedItem : function(containerId, element) {
        //we reload for now so no action here, children value update of containers will take care of it
    },

    sendFrameMessage : function(data, name) {
        Ext.getCmp('Iframe').getFrame().sendMessage(data, name);
    },

    _onClick : function(element) {
        if (element.getAttribute(HST.ATTR.INHERITED)) {
            return;
        }
        var id = element.getAttribute('id');
        var recordIndex = this.stores.pageModel.findExact('id', id);

        if (recordIndex < 0) {
            console.warn('Handling onClick for element[id=' + id + '] with no record in component store');
            return;
        }
        this.fireEvent('pageClick', recordIndex);
    },

    /**
     * It's not possible to register message:afterselect style listeners..
     * This should work and I'm probably doing something stupid, but I could not
     * get it to work.. So do like this instead.....
     */
    handleFrameMessages : function(frm, msg) {
        var self = this;
        try {
            if (msg.tag == 'rearrange') {
                this._onRearrangeContainer(msg.data.id, msg.data.children);
            } else if (msg.tag == 'onclick') {
                this._onClick(msg.data.element);
            } else if (msg.tag == 'receiveditem') {
                this._handleReceivedItem(msg.data.id, msg.data.element);
            } else if (msg.tag == 'remove') {
                this._removeByElement(msg.data.element);
            } else if (msg.tag == 'afterinit') {
                this.fireEvent('iFrameInitialized', msg.data);
            } else if (msg.tag == 'refresh') {
                this.refreshIframe();
            } else if (msg.tag == 'iframeexception') {
                var errorMsg = this.resources['iframe-event-exception-message-message'];
                if (msg.data.msg) {
                    errorMsg += msg.data.msg;
                }
                if (msg.data.exception) {
                    errorMsg += "\n" + msg.data.exception;
                }
                console.error(errorMsg);
                Hippo.Msg.alert(this.resources['iframe-event-exception-message-title'], this.resources['iframe-event-exception-message-message'], function() {
                    self.initComposer.apply(self);
                });
            } else if (msg.tag == 'edit-document') {
                this._handleEdit(msg.data.uuid);
            }
        } catch(e) {
            Hippo.Msg.alert(this.resources['iframe-event-handle-error-title'], this.resources['iframe-event-handle-error'].format(msg.tag)+' '+e, function() {
                self.initComposer.call(self);
            });
            console.error(e);
        }
    }
});
