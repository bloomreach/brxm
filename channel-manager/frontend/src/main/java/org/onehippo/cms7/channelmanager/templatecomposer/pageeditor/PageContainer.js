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

        this.ignoreRenderHostParameterName = config.ignoreRenderHostParameterName;
        this.resources = config.resources;

        if (config.composerMountUrl.lastIndexOf('/') !== config.composerMountUrl.length - 1) {
            config.composerMountUrl = config.composerMountUrl + '/';
        }
        this.composerMountUrl = config.composerMountUrl;

        this.composerRestMountUrl = config.composerRestMountUrl;
        if (config.renderHostSubMountPath && config.renderHostSubMountPath.indexOf('/') === 0) {
            config.renderHostSubMountPath = config.renderHostSubMountPath.substr(1);
        }

        this.iFrameErrorPage = config.iFrameErrorPage;
        this.initialHstConnectionTimeout = config.initialHstConnectionTimeout;
        this.iFrameJsHeadContributions = config.iFrameJsHeadContributions;
        this.iFrameCssHeadContributions = config.iFrameCssHeadContributions;

        this.composerInitialized = false;

        this.iframeResourceCache = new Hippo.Future(function(success, fail) {
            this._populateIFrameResourceCache(success, fail);
        }.createDelegate(this));

        Hippo.ChannelManager.TemplateComposer.PageContainer.superclass.constructor.call(this, config);

        this.addEvents('beforeIFrameDOMReady',
                       'iFrameException',
                       'edit-document',
                       'beforeInitComposer',
                       'afterInitComposer');

        Hippo.ChannelManager.TemplateComposer.Container = this;

        // initialized on domready
        this.pageContext = null;

        this.on('beforeInitComposer', function() {
            this.previewMode = true;
        }, this);

        this.on('afterInitComposer', function() {
            this.composerInitialized = true;
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
            url: this.composerRestMountUrl + 'cafebabe-cafe-babe-cafe-babecafebabe./keepalive?' + this.ignoreRenderHostParameterName + '=true',
            success: function () {
                // Do nothing
            }
        });
    },

    _populateIFrameResourceCache : function(resourcesLoaded, loadFailed) {
        var iframeResources = {
            cache: {},
            css: this.iFrameCssHeadContributions,
            js: this.iFrameJsHeadContributions
        };
        var self = this;
        // clone array with concat()
        var queue = this.iFrameCssHeadContributions.concat().concat(this.iFrameJsHeadContributions);
        var futures = [];
        for (var i = 0; i < queue.length; i++) {
            futures[i] = new Hippo.Future(function(success, failure) {
                var src = queue[i];
                Ext.Ajax.request({
                    url : src,
                    method : 'GET',
                    success : function(result, request) {
                        iframeResources.cache[src] = result.responseText;
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
                    resourcesLoaded(iframeResources);
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
        instance.previewMode = true;
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

    refreshIframe : function() {
        console.log('refreshIframe');
        var iframe = Ext.getCmp('Iframe');
        var frame = iframe.getFrame();
        var window = frame.getWindow();
        var scrollSave = {x: window.pageXOffset, y: window.pageYOffset};
        this.on('beforeIFrameDOMReady', function() {
            window.scrollBy(scrollSave.x, scrollSave.y);
        }, this, {single : true});
        iframe.setSrc(iframe.getFrameDocument().location.href); //following links in the iframe doesn't set iframe.src..
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

    _onIframeDOMReady : function(frm) {
        this.fireEvent('beforeIFrameDOMReady');
        this.frm = frm;

        // disable old page context
        if (this.pageContext != null) {
            this.pageContext.suspendEvents();
        }

        var config = {
            composerMountUrl: this.composerMountUrl,
            composerRestMountUrl: this.composerRestMountUrl,
            renderHostSubMountPath: this.renderHostSubMountPath,
            ignoreRenderHostParameterName: this.ignoreRenderHostParameterName,
            previewMode: this.previewMode,
            resources: this.resources
        };
        this.pageContext = new Hippo.ChannelManager.TemplateComposer.PageContext(
                config, this.iframeResourceCache, this.pageContext);
        this.relayEvents(this.pageContext, [
           'afterBuildOverlay',
           'mountChanged',
           'iFrameInitialized',
           'iFrameException'
        ]);
        this.pageContext.initialize(frm);
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
                var recordIndex = self.pageContext.stores.pageModel.findExact('id', id); //should probably do this through the selectionModel
                var record = self.pageContext.stores.pageModel.getAt(recordIndex);
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
    getMenuActions : function(record, selected) {
        var actions = [];
        var store = this.pageContext.stores.pageModel;
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
        var recordIndex = this.pageContext.stores.pageModel.findExact('id', id);

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
