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

        this.resources = config.resources;

        this.cmsUser = config.cmsUser;
        this.templateComposerContextPath = config.templateComposerContextPath;
        this.composerRestMountPath = config.composerRestMountPath;
        this.contextPath = config.contextPath;
        this.cmsPreviewPrefix = config.cmsPreviewPrefix;
        this.renderPathInfo = config.renderPathInfo;
        this.composerRestMountUrl = this.templateComposerContextPath + this.composerRestMountPath;

        this.iFrameErrorPage = config.iFrameErrorPage;
        this.initialHstConnectionTimeout = config.initialHstConnectionTimeout;
        this.iFrameJsHeadContributions = config.iFrameJsHeadContributions;
        this.iFrameCssHeadContributions = config.iFrameCssHeadContributions;

        this.previewMode = true;
        this.canEdit = false;

        this.iframeCompletion = [];

        this.iframeResourceCache = this._populateIFrameResourceCache();

        this.addEvents(
                'edit-document',
                'documents',
                'lock',
                'unlock'
        );

        Hippo.ChannelManager.TemplateComposer.PageContainer.superclass.constructor.call(this, config);

        // initialized on domready
        this.pageContext = null;

        this.on('fatalIFrameException', function(data) {
            var iFrame, frm;
            iFrame = Ext.getCmp('Iframe');
            frm = iFrame.getFrame();
            if (frm !== null && data.msg) {
                this.on('afterIFrameDOMReady', function () {
                    frm.execScript('setErrorMessage(\''+data.msg+'\');', true);
                }, this, {single : true});
                frm.isReset = false;
                frm.setSrc(this.iFrameErrorPage);
            }
            Hippo.Msg.hide();
        }, this);
    },

    _populateIFrameResourceCache : function() {
        var iframeResources, self, resourceUrls, futures, join, i;
        iframeResources = {
            cache: {},
            css: this.iFrameCssHeadContributions,
            js: this.iFrameJsHeadContributions
        };
        self = this;
        // clone array with concat()
        resourceUrls = this.iFrameCssHeadContributions.concat().concat(this.iFrameJsHeadContributions);
        futures = [];

        for (i = 0; i < resourceUrls.length; i++) {
            futures[i] = new Hippo.Future(function(success, failure) {
                (function(src) {
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
                })(resourceUrls[i]);
            });
        }
        join = Hippo.Future.join(futures);
        join.set(iframeResources);
        return join;
    },

    // PUBLIC METHODS THAT CHANGE OR RELOAD THE iFrame

    initComposer : function() {
        var retry, self;

        if (typeof this.contextPath === 'undefined'
                || typeof this.renderHost === 'undefined'
                || this.renderHost.trim() === '') {
            console.error(this.resources['error-init-composer']);
            this.fireEvent('fatalIFrameException', { msg : this.resources['error-init-composer'] });
            return;
        }

        this.previewMode = true;

        this._lock();

        retry = this.initialHstConnectionTimeout;
        self = this;
        // do initial handshake with CmsSecurityValve of the composer mount and
        // go ahead with the actual host which we want to edit (for which we need to be authenticated)
        var composerMode = function(callback) {
            Ext.Ajax.request({
                headers: {
                    'CMS-User': self.cmsUser,
                    'FORCE_CLIENT_HOST': 'true'
                },
                url: self.composerRestMountUrl+'/cafebabe-cafe-babe-cafe-babecafebabe./composermode/'+self.renderHost+'/?FORCE_CLIENT_HOST=true',
                success: callback,
                failure: function(exceptionObject) {
                    if (exceptionObject.isTimeout) {
                        if (retry > 0) {
                                retry = retry - Ext.Ajax.timeout;
                                window.setTimeout(function() {
                                    composerMode(callback);
                                }, Ext.Ajax.timeout);
                        } else {
                            Hippo.Msg.hide();
                            Hippo.Msg.confirm(self.resources['hst-exception-title'], self.resources['hst-timeout-message'], function(id) {
                                if (id === 'yes') {
                                    retry = self.initialHstConnectionTimeout;
                                    composerMode(callback);
                                } else {
                                    self.fireEvent.apply(self, ['fatalIFrameException', {msg : self.resources['hst-exception']}]);
                                }
                            });
                        }
                    } else {
                        console.error(exceptionObject);
                        console.error(self.resources['hst-exception']+' status: "'+exceptionObject.status+'", statusText: "'+exceptionObject.statusText+'"');
                        Hippo.Msg.alert(self.resources['hst-exception-title'], self.resources['hst-exception'], function() {
                            self.fireEvent.apply(self, ['fatalIFrameException', {msg : self.resources['hst-exception']}]);
                        });
                    }
                }
            });
        };
        composerMode(function(response) {
            var responseObj, iFrame, iFrameUrl;
            responseObj = Ext.util.JSON.decode(response.responseText);
            iFrame = Ext.getCmp('Iframe');
            this.canEdit = responseObj.data;

            iFrame.frameEl.isReset = false; // enable domready get's fired workaround, we haven't set defaultSrc on the first place

            this._initIFrameListeners();
            iFrameUrl = this.contextPath;
            if (iFrameUrl === '/') {
                iFrameUrl = '';
            }
            if (this.cmsPreviewPrefix) {
                iFrameUrl += '/'+this.cmsPreviewPrefix;
            }
            if (this.renderPathInfo) {
                iFrameUrl += this.renderPathInfo;
            }
            if (iFrameUrl === this.contextPath) {
                // The best practice for proxy pass rules is to match on <context path>/ to delegate to the site webapp.
                // The iframe url should therefore end with '/'.
                iFrameUrl += '/';
            }
            iFrame.setSrc(iFrameUrl);

        }.createDelegate(this));
    },

    refreshIframe : function() {
        var iframe, frame, window, scrollSave;
        console.log('refreshIframe');
        iframe = Ext.getCmp('Iframe');
        frame = iframe.getFrame();
        window = frame.getWindow();
        scrollSave = {x: window.pageXOffset, y: window.pageYOffset};

        this._lock(function() {
            window.scrollBy(scrollSave.x, scrollSave.y);
        });

        iframe.getFrameDocument().location.reload(true);
    },

    unlockMount: function () {
        var self, mountId;
        this._lock();
        self = this;
        mountId = this.pageContext.ids.mountId;
        Hippo.Msg.confirm(self.resources['unlock-channel-title'], self.resources['unlock-channel-message'], function(btn, text) {
            if (btn == 'yes') {
                Ext.Ajax.request({
                    method: 'POST',
                    headers: {
                        'FORCE_CLIENT_HOST': 'true'
                    },
                    url: self.composerRestMountUrl + '/' + mountId + './unlock?FORCE_CLIENT_HOST=true',
                    success: function () {
                        self.pageContext = null;
                        self.refreshIframe.call(self, null);
                    },
                    failure: function(result) {
                        var jsonData = Ext.util.JSON.decode(result.responseText);
                        console.error('Unlocking failed ' + jsonData.message);
                        Hippo.Msg.alert(self.resources['unlocking-failed-title'], self.resources['unlocking-failed-message'], function() {
                        self.initComposer.call(self);
                        });
                    }
                });
            } else {
                self._complete();
            }
        });
    },

    discardChanges : function () {
        var self, mountId, hasPreviewHstConfig;
        this._lock();
        self = this;
        mountId = this.pageContext.ids.mountId;
        hasPreviewHstConfig = this.pageContext.hasPreviewHstConfig;

        if (hasPreviewHstConfig) {
            Hippo.Msg.confirm(self.resources['discard-changes-title'], self.resources['discard-changes-message'], function(btn, text) {
                if (btn == 'yes') {
                    Ext.Ajax.request({
                        method: 'POST',
                        headers: {
                            'FORCE_CLIENT_HOST': 'true'
                        },
                        url: self.composerRestMountUrl + '/' + mountId + './discard?FORCE_CLIENT_HOST=true',
                        success: function () {
                            // reset pageContext, the page and toolkit stores must be reloaded
                            self.pageContext = null;
                            self.refreshIframe.call(self, null);
                        },
                        failure: function(result) {
                            var jsonData = Ext.util.JSON.decode(result.responseText);
                            console.error('Discarding changes failed ' + jsonData.message);
                            Hippo.Msg.alert(self.resources['discard-changes-failed-title'], self.resources['discard-changes-failed-message'], function() {
                                self.initComposer.call(self);
                            });
                        }
                    });
                } else {
                    self._complete();
                }
            });
        } else {
            this._complete();
        }
    },

    toggleMode: function () {
        var self, mountId, hasPreviewHstConfig;
        self = this;
        this._lock();

        this.previewMode = !this.previewMode;

        mountId = this.pageContext.ids.mountId;
        hasPreviewHstConfig = this.pageContext.hasPreviewHstConfig;

        console.log('hasPreviewHstConfig:' + hasPreviewHstConfig);
        if (this.previewMode) {
            var iFrame = Ext.getCmp('Iframe');
            iFrame.getFrame().sendMessage({}, 'hideoverlay');
            iFrame.getFrame().sendMessage({}, 'showlinks');
            self._complete();
        } else {
            if (hasPreviewHstConfig) {
                var doneCallback = function() {
                    var iFrame = Ext.getCmp('Iframe');
                    iFrame.getFrame().sendMessage({}, 'showoverlay');
                    iFrame.getFrame().sendMessage({}, 'hidelinks');
                    self._complete();
                }.createDelegate(this);

                /**
                 * There is a preview hst configuration. Try to acquire a lock. When this succeeds, show the overlay,
                 * otherwise, show a failure message.
                 */
                Ext.Ajax.request({
                    url:  this.composerRestMountUrl + '/' + mountId + './lock?FORCE_CLIENT_HOST=true',
                    method: 'POST',
                    headers: {
                        'FORCE_CLIENT_HOST': 'true'
                    },
                    success : function(response) {
                        var lockState = Ext.decode(response.responseText).data;
                        console.warn('Is it locked: ' + lockState);
                        if (lockState == 'lock-acquired') {
                            if (self.pageContext.renderedVariant !== 'hippo-default') {
                                Ext.Ajax.request({
                                    url: self.composerRestMountUrl+'/cafebabe-cafe-babe-cafe-babecafebabe./setvariant/?FORCE_CLIENT_HOST=true',
                                    method: 'POST',
                                    headers: {
                                        'FORCE_CLIENT_HOST': 'true'
                                    },
                                    params: {
                                        'variant': 'hippo-default'
                                    },
                                    success : function() {
                                        self.pageContext = null;
                                        self.refreshIframe.call(self, null);
                                    },
                                    failure : function() {
                                        console.error('Error setting the rendered page variant back to hippo-default');
                                        doneCallback();
                                    }
                                });
                            } else {
                                doneCallback();
                            }
                        } else {
                            console.error('The mount is already locked.');
                            Hippo.Msg.alert(self.resources['mount-locked-title'], self.resources['mount-locked-message'], function() {
                                self.initComposer.call(self, null);
                            });
                        }
                    },
                    failure : function() {
                        console.error('Failed to acquire the lock.');
                    }
                });
            } else {
                // create new preview hst configuration
                Ext.Ajax.request({
                    method: 'POST',
                    headers: {
                        'FORCE_CLIENT_HOST': 'true'
                    },
                    url: this.composerRestMountUrl + '/' + mountId + './edit?FORCE_CLIENT_HOST=true',
                    success: function () {
                        // reset pageContext, the page and toolkit stores must be reloaded
                        self.pageContext = null;
                        // refresh iframe to get new hst config uuids. previewMode=false will initialize
                        // the editor for editing with the refresh
                        self.refreshIframe.call(self, null);
                    },
                    failure: function(result) {
                        var jsonData = Ext.util.JSON.decode(result.responseText);
                        if (jsonData.data == 'locked') {
                            Hippo.Msg.alert(self.resources['mount-locked-title'], self.resources['mount-locked-message'], function() {
                                self.initComposer.call(self, null);
                            });
                        } else {
                            console.error(self.resources['preview-hst-config-creation-failed'] + ' ' + jsonData.message);
                            Hippo.Msg.alert(self.resources['preview-hst-config-creation-failed-title'], self.resources['preview-hst-config-creation-failed'], function() {
                                self.initComposer.call(self, null);
                            });
                        }
                    }
                });
            }
        }
    },

    publishHstConfiguration : function() {
        this._lock();
        var self = this;
        Ext.Ajax.request({
            method: 'POST',
            headers: {
                'FORCE_CLIENT_HOST': 'true'
            },
            url: this.composerRestMountUrl +'/'+ this.pageContext.ids.mountId + './publish?FORCE_CLIENT_HOST=true',
            success: function () {
                self.refreshIframe.call(self, null);
            },
            failure: function(result) {
                var jsonData = Ext.util.JSON.decode(result.responseText);
                console.error(self.resources['published-hst-config-failed-message']+' '+jsonData.message);
                Hippo.Msg.alert(self.resources['published-hst-config-failed-message-title'], self.resources['published-hst-config-failed-message'], function() {
                    self.initComposer.call(self);
                });
            }
        });
    },

    deselectComponents : function() {
        this.sendFrameMessage({}, 'deselect');
    },

    // END PUBLIC METHODS THAT CHANGE THE iFrame

    _lock : function(cb) {
        if (this.iframeCompletion.length == 0) {
            Ext.getCmp('Iframe').body.mask();
            this.fireEvent('lock');
        }
        if (typeof cb === 'function') {
            this.iframeCompletion.unshift(cb);
        }
    },

    _complete : function() {
        console.log('_complete');
        while (this.iframeCompletion.length > 0) {
            var cb = this.iframeCompletion.shift();
            cb.call(this);
        }
        this.fireEvent('unlock', this.pageContext);
        Ext.getCmp('Iframe').body.unmask();
    },

    _fail : function() {
        console.log('_fail');
        this.iframeCompletion = [];
        this.fireEvent('unlock', null);
        Ext.getCmp('Iframe').body.unmask();
    },

    _initIFrameListeners : function() {
        var iFrame = Ext.getCmp('Iframe');
        iFrame.purgeListeners();
        iFrame.on('message', this.handleFrameMessages, this);
        iFrame.on('unload', function () {
            this._lock();
        }, this);
        iFrame.on('documentloaded', function(frm) {
            var uri = frm.getDocumentURI();
            if (!frm.domFired) {
                this.initComposer();
                return;
            }
            if ((Ext.isSafari || Ext.isChrome) && uri !== '' && uri !== 'about:blank') {
                this._onIframeDOMReady(frm);
            }
        }, this);
        iFrame.on('domready', function(frm) {
            // Safari && Chrome report a DOM ready event, but js is not yet fully loaded, resulting
            // in 'undefined' errors.
            var uri = frm.getDocumentURI();
            if ((Ext.isGecko || Ext.isIE) && uri !== '' && uri !== 'about:blank') {
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
        this._lock();

        this.frm = frm;
        this.selectedRecord = null;

        // disable old page context
        if (this.pageContext != null) {
            this.pageContext.suspendEvents();
        }

        var config = {
            templateComposerContextPath: this.templateComposerContextPath,
            composerRestMountPath: this.composerRestMountPath,
            renderPath: this.renderPath,
            previewMode: this.previewMode,
            resources: this.resources
        };
        this.pageContext = new Hippo.ChannelManager.TemplateComposer.PageContext(
                config, this.iframeResourceCache, this.pageContext, this);
        this.relayEvents(this.pageContext, [
           'mountChanged',
           'fatalIFrameException'
        ]);
        this.pageContext.on('pageContextInitialized', function() {
            this.previewMode = this.pageContext.previewMode;
            this._complete();
        }, this);
        this.pageContext.on('pageContextInitializationFailed', function(error) {
            this.previewMode = this.pageContext.previewMode;
            console.error(this.resources['page-context-initialization-failed-message']);
            console.error(error);
            if (this._hasFocus()) {
                Hippo.Msg.alert(this.resources['page-context-initialization-failed-title'], this.resources['page-context-initialization-failed-message'], this);
            }
            this._fail();
        }, this);
        this.pageContext.initialize(frm, this.canEdit);
    },

    _onRearrangeContainer: function(rearranges) {
        var self, futures;
        self = this;
        futures = [];

        for (var i=0; i<rearranges.length; i++) {
            (function(rearrange) {
                futures.push(new Hippo.Future(function(onSuccess, onFailure) {
                    window.setTimeout(function() {
                        try {
                            var recordIndex, record, writeListener;
                            recordIndex = self.pageContext.stores.pageModel.findExact('id', rearrange.id); //should probably do this through the selectionModel
                            record = self.pageContext.stores.pageModel.getAt(recordIndex);
                            record.set('children', rearrange.children);
                            console.log('_onRearrangeContainer ' + rearrange.id + ', children: ' + rearrange.children);
                            writeListener = function(store, action, result, res, rec) {
                                if (rec.id === record.id) {
                                    self.pageContext.stores.pageModel.un('write', writeListener, self);
                                    onSuccess();
                                }
                            };
                            self.pageContext.stores.pageModel.on('write', writeListener, self);
                            record.commit();
                        } catch (exception) {
                            console.error('_onRearrangeContainer ' + exception);
                            onFailure();
                        }
                    }, 0);
                }));
            })(rearranges[i]);
        }

        Hippo.Future.join(futures).when(function() {
            console.log('refresh iframe due to rearranging of containers');
            this.refreshIframe();
        }.createDelegate(this)).otherwise(function() {
            console.error('rearranging containers failed');
        }.createDelegate(this));
    },

    _handleEdit : function(uuid) {
        this.fireEvent('edit-document', uuid);
    },

    _handleDocuments : function(documents) {
        this.fireEvent('documents', documents);
    },

    _handleReceivedItem : function(containerId, element) {
        //we reload for now so no action here, children value update of containers will take care of it
    },

    sendFrameMessage : function(data, name) {
        Ext.getCmp('Iframe').getFrame().sendMessage(data, name);
    },

    _onClick : function(data) {
        var id, variant, inherited, record;
        id = data.elementId;
        variant = data.variant;
        inherited = data.inherited;
        record = this.pageContext.stores.pageModel.getById(id);

        if (!record) {
            console.warn('Handling onClick for element[id=' + id + '] with no record in component store');
            return;
        }

        if (this.selectedRecord !== record) {
            this.sendFrameMessage({id: record.data.id}, 'select');
            this.selectedRecord = record;
            this.fireEvent('selectItem', record, variant, inherited);
        }
    },

    _onDeselect : function() {
        this.selectedRecord = null;
    },

    _removeByRecord: function(record) {
        var store, self;
        store = this.pageContext.stores.pageModel;
        self = this;
        Hippo.Msg.confirm(this.resources['delete-message-title'], this.resources['delete-message'].format(record.get('name')), function(btn, text) {
            if (btn == 'yes') {
                store.on('write', self.refreshIframe, self, {single: true});
                store.remove(record);
            }
        });
    },

    _removeByElement : function(element) {
        var store, index;
        store = this.pageContext.stores.pageModel;
        index = store.findExact('id', Ext.fly(element).getAttribute(HST.ATTR.ID));
        this._removeByRecord(store.getAt(index))
    },

    _hasFocus : function() {
        var node = Ext.getCmp('Iframe').el.dom;
        while (node) {
            if (node.style.visibility === 'hidden' || node.style.display === 'none') {
                return false;
            }
            node = node.parentNode;
        }
        return true;
    },


    /**
     * It's not possible to register message:afterselect style listeners..
     * This should work and I'm probably doing something stupid, but I could not
     * get it to work.. So do like this instead.....
     */
    handleFrameMessages : function(frm, msg) {
        var self, errorMsg;
        self = this;
        try {
            if (msg.tag == 'rearrange') {
                this._onRearrangeContainer(msg.data);
            } else if (msg.tag == 'onclick') {
                this._onClick(msg.data);
            } else if (msg.tag == 'deselect') {
                this._onDeselect();
            } else if (msg.tag == 'receiveditem') {
                this._handleReceivedItem(msg.data.id, msg.data.element);
            } else if (msg.tag == 'remove') {
                this._removeByElement(msg.data.element);
            } else if (msg.tag == 'refresh') {
                this.refreshIframe();
            } else if (msg.tag == 'iframeexception') {
                errorMsg = this.resources['iframe-event-exception-message-message'];
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
            } else if (msg.tag == 'documents') {
                this._handleDocuments(msg.data);
            }
        } catch(e) {
            console.error(this.resources['iframe-event-handle-error']+' Message tag: '+msg.tag+'. '+e);
            Hippo.Msg.alert(this.resources['iframe-event-handle-error-title'], this.resources['iframe-event-handle-error'], function() {
                self.initComposer.call(self);
            });
            console.error(e);
        }
    }

});
