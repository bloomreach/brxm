/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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

    Hippo.ChannelManager.TemplateComposer.PageContainer = Ext.extend(Ext.util.Observable, {

        constructor: function(config) {
            this.resources = config.resources;
            this.cmsUser = config.cmsUser;
            this.composerRestMountPath = config.composerRestMountPath;
            this.contextPath = config.contextPath;
            this.cmsPreviewPrefix = config.cmsPreviewPrefix;
            this.renderPathInfo = config.renderPathInfo;
            this.composerRestMountUrl = this.contextPath + this.composerRestMountPath;
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
                var iframe = Ext.getCmp('pageEditorIFrame');

                if (data.msg) {
                    this.on('afterIFrameDOMReady', function() {
                        var script = "setErrorMessage('" + data.msg + "');",
                            headFragment = iframe.createHeadFragment();
                        headFragment.addScript(script).flush();
                    }, this, {single: true});
                    iframe.setLocation(this.iFrameErrorPage);
                }
                Hippo.Msg.hide();
            }, this);

            this.ping = window.setInterval(function() {
                if (this.sessionId && this._hasFocus() && Ext.getCmp('pageEditorIFrame').isValidSession(this.sessionId)) {
                    Ext.Ajax.request({
                        url: this.composerRestMountUrl + '/cafebabe-cafe-babe-cafe-babecafebabe./keepalive/?FORCE_CLIENT_HOST=true',
                        failure: function() {
                            delete this.sessionId;

                            Hippo.Msg.hide();
                            Hippo.Msg.confirm(this.resources['hst-exception-title'], this.resources['hst-timeout-message'], function(id) {
                                if (id === 'yes') {
                                    this._initializeHstSession();
                                } else {
                                    this.fireEvent.apply(this, ['fatalIFrameException', {msg: this.resources['hst-exception']}]);
                                }
                            }.createDelegate(this));
                        }.createDelegate(this)
                    });
                }
            }.createDelegate(this), 20000);
        },

        destroy: function() {
            window.clearInterval(this.ping);
            delete this.ping;

            Hippo.ChannelManager.TemplateComposer.PageContainer.superclass.destroy.call(this);
        },

        _populateIFrameResourceCache: function() {
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

            Ext.each(resourceUrls, function(src) {
                futures.push(new Hippo.Future(function(success, failure) {
                    Ext.Ajax.request({
                        url: src,
                        method: 'GET',
                        success: function(result, request) {
                            iframeResources.cache[src] = result.responseText;
                            success();
                        },
                        failure: function(result, request) {
                            self.fireEvent.apply(self, ['fatalIFrameException', {msg: self.resources['pre-cache-iframe-resources-exception'].format(src)}]);
                            failure();
                        }
                    });
                }));
            });
            join = Hippo.Future.join(futures);
            join.set(iframeResources);
            return join;
        },

        // PUBLIC METHODS THAT CHANGE OR RELOAD THE iFrame

        initComposer: function(isEditMode) {
            return new Hippo.Future(function(success, failure) {
                if (typeof this.contextPath === 'undefined'
                        || typeof this.renderHost === 'undefined'
                        || this.renderHost.trim() === '') {
                    console.error(this.resources['error-init-composer']);
                    this.fireEvent('fatalIFrameException', { msg: this.resources['error-init-composer'] });
                    failure();
                    return;
                }

                this.previewMode = !isEditMode;

                this._lock();

                // do initial handshake with CmsSecurityValve of the composer mount and
                // go ahead with the actual host which we want to edit (for which we need to be authenticated)
                this._initializeHstSession(function() {
                    var iFrameUrl;

                    this._initIFrameListeners();

                    iFrameUrl = this.contextPath;
                    if (iFrameUrl === '/') {
                        iFrameUrl = '';
                    }
                    if (this.cmsPreviewPrefix) {
                        iFrameUrl += '/' + this.cmsPreviewPrefix;
                    }
                    if (this.renderPathInfo) {
                        iFrameUrl += this.renderPathInfo;
                    }
                    if (iFrameUrl === this.contextPath) {
                        // The best practice for proxy pass rules is to match on <context path>/ to delegate to the site webapp.
                        // The iframe url should therefore end with '/'.
                        iFrameUrl += '/';
                    }
                    Ext.getCmp('pageEditorIFrame').setLocation(iFrameUrl);

                    success();
                }.createDelegate(this));
            }.createDelegate(this));
        },

        _initializeHstSession: function(callback) {
            var retry = this.initialHstConnectionTimeout, self = this;
            Ext.Ajax.request({
                headers: {
                    'CMS-User': self.cmsUser,
                    'FORCE_CLIENT_HOST': 'true'
                },
                url: self.composerRestMountUrl + '/cafebabe-cafe-babe-cafe-babecafebabe./composermode/' + self.renderHost + '/?FORCE_CLIENT_HOST=true',
                success: function(response) {
                    var responseObj = Ext.util.JSON.decode(response.responseText);
                    this.canEdit = responseObj.data.canWrite;
                    this.sessionId = responseObj.data.sessionId;
                    if (callback) {
                        callback();
                    }
                }.createDelegate(this),
                failure: function(exceptionObject) {
                    if (exceptionObject.isTimeout) {
                        if (retry > 0) {
                            retry = retry - Ext.Ajax.timeout;
                            window.setTimeout(function() {
                                self._initializeHstSession(callback);
                            }, Ext.Ajax.timeout);
                        } else {
                            Hippo.Msg.hide();
                            Hippo.Msg.confirm(self.resources['hst-exception-title'], self.resources['hst-timeout-message'], function(id) {
                                if (id === 'yes') {
                                    retry = self.initialHstConnectionTimeout;
                                    self._initializeHstSession(callback);
                                } else {
                                    self.fireEvent.apply(self, ['fatalIFrameException', {msg: self.resources['hst-exception']}]);
                                }
                            });
                        }
                    } else {
                        console.error(exceptionObject);
                        console.error(self.resources['hst-exception'] + ' status: "' + exceptionObject.status + '", statusText: "' + exceptionObject.statusText + '"');
                        Hippo.Msg.alert(self.resources['hst-exception-title'], self.resources['hst-exception'], function() {
                            self.fireEvent.apply(self, ['fatalIFrameException', {msg: self.resources['hst-exception']}]);
                        });
                    }
                }
            });
        },

        refreshIframe: function() {
            var iframe, scrollSave;

            iframe = Ext.getCmp('pageEditorIFrame');
            scrollSave = iframe.getScrollPosition();

            this._lock(function() {
                iframe.scrollBy(scrollSave.x, scrollSave.y);
            });

            iframe.reload();
        },

        discardChanges: function() {
            var self, mountId, hasPreviewHstConfig;
            this._lock();
            self = this;
            mountId = this.pageContext.ids.mountId;
            hasPreviewHstConfig = this.pageContext.hasPreviewHstConfig;

            return new Hippo.Future(function(onSuccess) {
                if (hasPreviewHstConfig) {
                    Hippo.Msg.confirm(self.resources['discard-changes-title'], self.resources['discard-changes-message'], function(btn, text) {
                        if (btn === 'yes') {
                            Ext.Ajax.request({
                                method: 'POST',
                                headers: {
                                    'FORCE_CLIENT_HOST': 'true'
                                },
                                url: self.composerRestMountUrl + '/' + mountId + './discard?FORCE_CLIENT_HOST=true',
                                success: function() {
                                    // reset pageContext, the page and toolkit stores must be reloaded
                                    self.pageContext = null;
                                    self.refreshIframe.call(self, null);
                                    onSuccess();
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
            });
        },

        manageChanges: function() {
            var self = this;

            return new Hippo.Future(function(onSuccess) {
                var manageChangesWindow = new Hippo.ChannelManager.TemplateComposer.ManageChangesWindow({
                    cmsUser: self.cmsUser,
                    composerRestMountUrl: self.composerRestMountUrl,
                    mountId: self.pageContext.ids.mountId,
                    resources: self.resources,
                    onSuccess: function() {
                        self.refreshIframe();
                        onSuccess();
                    },
                    onFailure: function() {
                        Hippo.Msg.alert(
                            self.resources['manage-changes-failed-title'],
                            self.resources['manage-changes-failed-message'],
                            self.initComposer.createDelegate(self)
                        );
                    }
                });
                manageChangesWindow.show();
            });
        },

        toggleMode: function() {
            var self, hostToIFrame, mountId, hasPreviewHstConfig;

            self = this;
            hostToIFrame = Ext.getCmp('pageEditorIFrame').hostToIFrame;
            this._lock();

            this.previewMode = !this.previewMode;

            mountId = this.pageContext.ids.mountId;
            hasPreviewHstConfig = this.pageContext.hasPreviewHstConfig;

            if (this.previewMode) {
                hostToIFrame.publish('hideoverlay');
                hostToIFrame.publish('hide-edit-menu-buttons');
                hostToIFrame.publish('show-edit-content-buttons');
                self._complete();
            } else if (hasPreviewHstConfig) {
                // reset pageContext, the page and toolkit stores must be reloaded
                self.pageContext = null;
                // refresh iframe to get new hst config uuids or new lastModifiedTimestamsp.
                self.refreshIframe.call(self, null);
            } else {
                // create new preview hst configuration
                Ext.Ajax.request({
                    method: 'POST',
                    headers: {
                        'FORCE_CLIENT_HOST': 'true'
                    },
                    url: this.composerRestMountUrl + '/' + mountId + './edit?FORCE_CLIENT_HOST=true',
                    success: function() {
                        // reset pageContext, the page and toolkit stores must be reloaded
                        self.pageContext = null;
                        // refresh iframe to get new hst config uuids.
                        self.fireEvent('previewCreated');
                    },
                    failure: function(result) {
                        if (result.isTimeout) {
                            console.error(self.resources['preview-hst-config-creation-timeout-title']);
                            Hippo.Msg.alert(self.resources['preview-hst-config-creation-timeout-title'],
                                    self.resources['preview-hst-config-creation-timeout'] + self.resources['increase-timeout-location-helper-message'],function() {
                                        self.initComposer.call(self);
                                    });
                        } else {
                            var jsonData = Ext.util.JSON.decode(result.responseText);
                            console.error(self.resources['preview-hst-config-creation-failed'] + ' ' + jsonData.message);
                            Hippo.Msg.alert(self.resources['preview-hst-config-creation-failed-title'], self.resources['preview-hst-config-creation-failed'], function() {
                                self.initComposer.call(self);
                            });
                        }
                    }
                });
            }
        },

        publishHstConfiguration: function() {
            this._lock();
            var self = this;

            return new Hippo.Future(function(onSuccess) {
                Ext.Ajax.request({
                    method: 'POST',
                    headers: {
                        'FORCE_CLIENT_HOST': 'true'
                    },
                    url: self.composerRestMountUrl + '/' + self.pageContext.ids.mountId + './publish?FORCE_CLIENT_HOST=true',
                    success: function() {
                        self.refreshIframe.call(self, null);
                        onSuccess();
                    },
                    failure: function(result) {
                        var jsonData = Ext.util.JSON.decode(result.responseText);
                        if (result.isTimeout) {
                            console.error(self.resources['published-hst-config-timeout-message']);
                            Hippo.Msg.alert(self.resources['published-hst-config-timeout-message-title'],
                                    self.resources['published-hst-config-timeout-message'] + self.resources['increase-timeout-location-helper-message'],function() {
                                self.initComposer.call(self);
                            });
                        } else {
                            console.error(self.resources['published-hst-config-failed-message'] + ' ' + jsonData.message);
                            Hippo.Msg.alert(self.resources['published-hst-config-failed-message-title'], self.resources['published-hst-config-failed-message'], function() {
                                self.initComposer.call(self);
                            });
                        }

                    }
                });
            });
        },

        deselectComponents: function() {
            Ext.getCmp('pageEditorIFrame').hostToIFrame.publish('deselect');
        },

        // END PUBLIC METHODS THAT CHANGE THE iFrame

        _lock: function(cb) {
            if (this.iframeCompletion.length === 0) {
                if (typeof(this.maskTask) === 'undefined' || this.maskTask === null) {
                    this.maskTask = new Ext.util.DelayedTask(function() {
                        Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').mask();
                    });
                    this.maskTask.delay(800);
                }
                this.fireEvent('lock');
            }
            if (typeof cb === 'function') {
                this.iframeCompletion.unshift(cb);
            }
        },

        _complete: function() {
            while (this.iframeCompletion.length > 0) {
                var cb = this.iframeCompletion.shift();
                cb.call(this);
            }
            this.fireEvent('unlock', this.pageContext);

            if (typeof(this.maskTask) !== 'undefined' && this.maskTask !== null) {
                this.maskTask.cancel();
                this.maskTask = null;
            }
            Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').unmask();
        },

        _fail: function() {
            this.iframeCompletion = [];
            this.fireEvent('unlock', null);
            Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').unmask();
            if (typeof(this.maskTask) !== 'undefined' && this.maskTask !== null) {
                this.maskTask.cancel();
                this.maskTask = null;
            }
            Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').unmask();
        },

        _initIFrameListeners: function() {
            var iframe = Ext.getCmp('pageEditorIFrame');

            iframe.iframeToHost.unsubscribeAll();
            iframe.un('locationchanged', this._onIframeDOMReady, this);

            this.subscribeToIFrameMessages();
            iframe.on('locationchanged', this._onIframeDOMReady, this);
        },

        _onIframeDOMReady: function() {
            this._lock();

            this.selectedRecord = null;

            // disable old page context
            if (this.pageContext !== null) {
                this.pageContext.suspendEvents();
            }

            this.pageContext = new Hippo.ChannelManager.TemplateComposer.PageContext(
                    this.iframeResourceCache, this.pageContext, this);
            this.relayEvents(this.pageContext, [
                'mountChanged',
                'fatalIFrameException'
            ]);
            this.pageContext.on('pageContextInitialized', function() {
                this.previewMode = this.pageContext.previewMode;
                this._complete();
            }, this);
            this.pageContext.on('pageContextInitializationFailed', function(error) {
                var iframe = Ext.getCmp('pageEditorIFrame');

                this.previewMode = this.pageContext.previewMode;

                if (!iframe.isValidSession(this.sessionId)) {
                    this._initializeHstSession(this._complete.createDelegate(this));
                } else {
                    console.error(this.resources['page-context-initialization-failed-message']);
                    console.error(error);
                    if (this._hasFocus()) {
                        Hippo.Msg.alert(
                            this.resources['iframe-page-error-message-title'],
                            this.resources['iframe-page-error-message-message'],
                            function() {
                                if (!iframe.goBack()) {
                                    Ext.getCmp('rootPanel').showChannelManager();
                                }
                            }
                        );
                    }
                    this._complete();
                }
            }, this);
            this.pageContext.initialize(this.canEdit);
        },

        _onRearrangeContainer: function(rearranges) {
            var self, futures;
            self = this;
            futures = [];

            Ext.each(rearranges, function(rearrange) {
                futures.push(new Hippo.Future(function(onSuccess, onFailure) {
                    window.setTimeout(function() {
                        try {
                            var recordIndex, record, writeListener;
                            recordIndex = self.pageContext.stores.pageModel.findExact('id', rearrange.id); //should probably do this through the selectionModel
                            record = self.pageContext.stores.pageModel.getAt(recordIndex);
                            record.set('children', rearrange.children);
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
            });

            Hippo.Future.join(futures).when(function() {
                this.refreshIframe();
            }.createDelegate(this)).otherwise(function() {
                console.error('rearranging containers failed');
            }.createDelegate(this));
        },

        _handleEditDocument: function(uuid) {
            this.fireEvent('edit-document', uuid);
        },

        _handleDocuments: function(documents) {
            this.fireEvent('documents', documents);
        },

        _handleEditMenu: function(uuid) {
            this.fireEvent('edit-menu', uuid);
        },

        _onClick: function(data) {
            var id, forcedVariant, containerDisabled, record;
            id = data.elementId;
            forcedVariant = data.forcedVariant;
            containerDisabled = data.containerDisabled;
            record = this.pageContext.stores.pageModel.getById(id);

            if (!record) {
                console.warn('Handling onClick for element[id=' + id + '] with no record in component store');
                return;
            }

            if (this.selectedRecord !== record) {
                Ext.getCmp('pageEditorIFrame').hostToIFrame.publish('select', record.data.id);
                this.selectedRecord = record;
                this.fireEvent('selectItem', record, forcedVariant, containerDisabled);
            }
        },

        _onDeselect: function() {
            this.selectedRecord = null;
        },

        _removeByRecord: function(record) {
            var store, self;
            store = this.pageContext.stores.pageModel;
            self = this;
            Hippo.Msg.confirm(this.resources['delete-message-title'], this.resources['delete-message'].format(record.get('name')), function(btn, text) {
                if (btn === 'yes') {
                    store.on('write', self.refreshIframe, self, {single: true});
                    store.remove(record);
                }
            });
        },

        _removeByElement: function(element) {
            var store, index;
            store = this.pageContext.stores.pageModel;
            index = store.findExact('id', Ext.fly(element).getAttribute(HST.ATTR.ID));
            this._removeByRecord(store.getAt(index));
        },

        _hasFocus: function() {
            var node = Ext.getCmp('pageEditorIFrame').el.dom;
            while (node && node.style) {
                if (node.style.visibility === 'hidden' || node.style.display === 'none') {
                    return false;
                }
                node = node.parentNode;
            }
            return true;
        },

        _showError: function(msg, exception) {
            var self = this, errorMsg = this.resources['iframe-event-exception-message-message'];
            if (msg) {
                errorMsg += ' ' + msg;
            }
            if (exception) {
                errorMsg += "\n" + exception;
            }
            console.error(errorMsg);
            Hippo.Msg.alert(this.resources['iframe-event-exception-message-title'], this.resources['iframe-event-exception-message-message'], function() {
                self.initComposer.apply(self);
            });
        },

        subscribeToIFrameMessages: function() {
            var self, iframeToHost, tryOrReinitialize;

            self = this;
            iframeToHost = Ext.getCmp('pageEditorIFrame').iframeToHost;

            tryOrReinitialize = function(callback, scope) {
                return function() {
                    try {
                        callback.apply(scope, arguments);
                    } catch (e) {
                        console.error(self.resources['iframe-event-handle-error'] + ' Failed callback: ' + callback.name + '. ' + e);
                        Hippo.Msg.alert(self.resources['iframe-event-handle-error-title'], self.resources['iframe-event-handle-error'], function() {
                            self.initComposer.call(self);
                        });
                        console.error(e);
                    }
                };
            };

            iframeToHost.subscribe('rearrange', tryOrReinitialize(this._onRearrangeContainer, this));
            iframeToHost.subscribe('onclick', tryOrReinitialize(this._onClick, this));
            iframeToHost.subscribe('deselect', tryOrReinitialize(this._onDeselect, this));
            iframeToHost.subscribe('remove', tryOrReinitialize(this._removeByElement, this));
            iframeToHost.subscribe('refresh', tryOrReinitialize(this.refreshIframe, this));
            iframeToHost.subscribe('exception', tryOrReinitialize(this._showError, this));
            iframeToHost.subscribe('edit-document', tryOrReinitialize(this._handleEditDocument, this));
            iframeToHost.subscribe('edit-menu', tryOrReinitialize(this._handleEditMenu, this));
            iframeToHost.subscribe('documents', tryOrReinitialize(this._handleDocuments, this));
        }

    });

}());