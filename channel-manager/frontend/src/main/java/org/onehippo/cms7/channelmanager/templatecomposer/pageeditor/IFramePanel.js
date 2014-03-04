/*
 * Copyright 2013-2014 Hippo B.V. (http://www.onehippo.com)
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
(function() {
    "use strict";

    Ext.ns('Hippo.ChannelManager.TemplateComposer');

    Hippo.ChannelManager.TemplateComposer.IFramePanel = Ext.extend(Ext.Panel, {

        frameId: null,
        hostToIFrame: null,
        iframeToHost: null,
        currentLocation: null,
        previousLocation: null,
        resizeTask: null,

        constructor: function(config) {
            this.frameId = Ext.id();
            this.hostToIFrame = Hippo.ChannelManager.TemplateComposer.createMessageBus('host-to-iframe');
            this.iframeToHost = Hippo.ChannelManager.TemplateComposer.createMessageBus('iframe-to-host');

            this.addEvents(
                'locationchanged'
            );

            Hippo.ChannelManager.TemplateComposer.IFramePanel.superclass.constructor.call(this, Ext.apply(config, {
                border: false,
                layout: 'fit',
                items: {
                    xtype: 'box',
                    id: this.frameId,
                    autoEl: {
                        tag: 'iframe',
                        frameborder: 0,
                        src: config.url || 'about:blank'
                    },
                    listeners: {
                        'afterrender': {
                            fn: function(iframe) {
                                iframe.el.addListener('load', this._onFrameLoad, this);
                            },
                            scope: this,
                            single: true
                        }
                    }
                },
                listeners: {
                    'resize': this._onResize,
                    scope: this
                }
            }));
        },

        _onFrameLoad: function() {
            var newLocation = this._getFrameLocation();

            if (newLocation !== this.currentLocation) {
                this.previousLocation = this.currentLocation;

                if (this.currentLocation !== null) {
                    this._detachFrame();
                }

                this.currentLocation = newLocation;
                this.fireEvent('locationchanged');
            }
        },

        _getFrameLocation: function() {
            var frameDocument, href;

            frameDocument = this._getFrameDocument();

            if (frameDocument !== undefined && frameDocument.location !== undefined) {
                href = frameDocument.location.href;
                if (href !== undefined && href !== '' && href !== 'about:blank') {
                    return href;
                }
            }
            return this._getFrameDom().src;
        },

        _getFrameDocument: function() {
            return this._getFrame().contentDocument;
        },

        _getFrame: function() {
            return document.getElementById(this.frameId);
        },

        _getFrameDom: function() {
            return Ext.getDom(this.frameId);
        },

        _onResize: function() {
            // throttle the number of 'resize' events send to the iframe
            if (this.resizeTask === null) {
                this.resizeTask = new Ext.util.DelayedTask(this._doResize.createDelegate(this));
            }
            this.resizeTask.delay(25);
        },

        _doResize: function() {
            this.hostToIFrame.publish('resize');
        },

        setLocation: function(url) {
            this.previousLocation = this.currentLocation;
            this._detachFrame();
            this._getFrameDom().src = url;
        },

        _detachFrame: function() {
            this.currentLocation = null;
            this.hostToIFrame.unsubscribeAll();
        },

        getLocation: function() {
            return this._getFrameLocation();
        },

        goBack: function() {
            if (!Ext.isEmpty(this.previousLocation)) {
                this.setLocation(this.previousLocation);
                this.previousLocation = null;
                return true;
            }
            return false;
        },

        getElement: function(id) {
            return this._getFrameDocument().getElementById(id);
        },

        getFrameElement: function() {
            return Ext.getCmp(this.frameId).el;
        },

        reload: function() {
            this._detachFrame();
            this._getFrameDocument().location.reload(true);
        },

        createHeadFragment: function() {
            // create an object to add elements to the iframe head using a DOM document fragment when possible

            var self, frameDocument, documentFragment, api;

            self = this;
            frameDocument = this._getFrameDocument();

            function getHead() {
                var headElements, head;

                headElements = frameDocument.getElementsByTagName('head');

                if (Ext.isEmpty(headElements)) {
                    head = frameDocument.createElement('head');
                    frameDocument.getElementsByTagName('html')[0].appendChild(head);
                } else {
                    head = headElements[0];
                }
                return head;
            }

            function addElement(tagName, text, attributes) {
                var element, textNode;

                element = frameDocument.createElement(tagName);

                if (Ext.isIE8) {
                    element.text = text;
                } else {
                    textNode = frameDocument.createTextNode(text);
                    element.appendChild(textNode);
                }

                Ext.iterate(attributes, function(attribute, value) {
                    element[attribute] = value;
                });

                if (documentFragment === undefined) {
                    documentFragment = self._getFrameDocument().createDocumentFragment();
                }
                documentFragment.appendChild(element);
            }

            api = {

                addScript: function(text, title) {
                    addElement('script', text, {
                        type: 'text/javascript',
                        title: title || 'inline'
                    });
                    return api;
                },

                addStyleSheet: function(text, title) {
                    if (Ext.isIE8) {
                        frameDocument.createStyleSheet().cssText = text;
                    } else {
                        addElement('style', text, {
                            type: 'text/css',
                            title: title
                        });
                    }
                    return api;
                },

                flush: function() {
                    if (documentFragment !== undefined) {
                        getHead().appendChild(documentFragment);
                        documentFragment = undefined;
                    }
                }

            };

            return api;
        },

        mask: function() {
            this.el.mask();
            this.on('locationchanged', this.el.mask, this.el);
        },

        unmask: function() {
            this.el.unmask();
            this.un('locationchanged', this.el.mask, this.el);
        },

        getScrollPosition: function() {
            var frameWindow = this._getFrameWindow();
            return {
                x: frameWindow.pageXOffset,
                y: frameWindow.pageYOffset
            };
        },

        _getFrameWindow: function() {
            return this._getFrame().contentWindow;
        },

        scrollBy: function(x, y) {
            this._getFrameWindow().scrollBy(x, y);
        },

        isValidSession: function(sessionId) {
            var result = false;

            Ext.each(this._getFrameDocument().cookie.split(';'), function(keyValue) {
                var equalsIndex, key, value;

                equalsIndex = keyValue.indexOf('=');
                key = keyValue.substr(0, equalsIndex).trim();
                value = keyValue.substr(equalsIndex + 1).trim();

                if (key === 'HSTSESSIONID' && value === sessionId) {
                    result = true;
                    return false;
                }
            }, this);

            return result;
        }

    });

    Ext.reg('Hippo.ChannelManager.TemplateComposer.IFramePanel', Hippo.ChannelManager.TemplateComposer.IFramePanel);

}());
