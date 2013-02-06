/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
(function () {
    "use strict";

    function createMessageBus(name) {
        var subscriptions = {};
        return {
            exception: function(msg) {
                this.publish('exception', msg);
            },

            publish: function(topic) {
                var i, len, subscription;
                if (subscriptions[topic] === undefined) {
                    return true;
                }
                len = subscriptions[topic].length;
                console.log(name + "[" + len + "] " + topic);
                for (i = 0; i < len; i++) {
                    subscription = subscriptions[topic][i];
                    if (subscription.callback.apply(subscription.scope, Array.prototype.slice.call(arguments, 1)) === false) {
                        return false;
                    }
                }
                return true;
            },

            subscribe: function(topic, callback, scope) {
                var scopeParameter = scope || window;
                if (subscriptions[topic] === undefined) {
                    subscriptions[topic] = [];
                }
                subscriptions[topic].push({callback: callback, scope: scopeParameter});
            },

            unsubscribe: function(topic, callback, scope) {
                var scopeParameter, i, len, subscription;
                if (subscriptions[topic] === undefined) {
                    return false;
                }
                scopeParameter = scope || window;
                for (i = 0, len = subscriptions[topic].length; i < len; i++) {
                    subscription = subscriptions[topic][i];
                    if (subscription.callback === callback && subscription.scope === scopeParameter) {
                        subscriptions[topic].splice(i, 1);
                        return true;
                    }
                }
                return false;
            },

            unsubscribeAll: function() {
                subscriptions = {};
            }

        };
    }

    Ext.ns('Hippo.ChannelManager.TemplateComposer');

    Hippo.ChannelManager.TemplateComposer.IFramePanel = Ext.extend(Ext.Panel, (function () {
        // private variables
        var frameName, frameId, lastLocation, instance;

        frameName = Ext.id();
        frameId = Ext.id();

        // private methods
        function getFrame() {
            return window.frames[frameName];
        }

        function getFrameDom() {
            return Ext.getDom(frameId);
        }

        function getFrameDocument() {
            var frame = getFrame(),
                result;

            if (Ext.isDefined(frame)) {
                result = frame.document;
                if (!Ext.isDefined(result)) {
                    result = frame.contentDocument;
                }
                if (!Ext.isDefined(result) && Ext.isDefined(frame.contentWindow)) {
                    result = frame.contentWindow.document;
                }
            }
            return result;
        }

        function getFrameLocation() {
            var frameDocument, href;

            frameDocument = getFrameDocument();

            if (frameDocument !== undefined) {
                href = frameDocument.location.href;
                if (href !== '' && href !== 'about:blank') {
                    return href;
                }
            }
            return getFrameDom().src;
        }

        function detachFrame() {
            lastLocation = undefined;
            instance.hostToIFrame.unsubscribeAll();
        }

        function onFrameLoad() {
            var frameLocation = getFrameLocation();

            if (frameLocation !== lastLocation) {
                detachFrame();
                lastLocation = frameLocation;
                instance.fireEvent('locationchanged');
            }
        }

        // public methods
        return {

            hostToIFrame: null,
            iframeToHost: null,

            constructor: function (config) {
                // global singleton
                Hippo.ChannelManager.TemplateComposer.IFramePanel.Instance = instance = this;

                this.hostToIFrame = createMessageBus('host-to-iframe');
                this.iframeToHost = createMessageBus('iframe-to-host');

                this.addEvents(
                    'locationchanged'
                );

                Hippo.ChannelManager.TemplateComposer.IFramePanel.superclass.constructor.call(this, Ext.apply(config, {
                    border: false,
                    layout: 'fit',
                    items: {
                        xtype: 'box',
                        id: frameId,
                        autoEl: {
                            tag: 'iframe',
                            name: frameName,
                            frameborder: 0
                        }
                    }
                }));
            },

            afterRender: function() {
                Hippo.ChannelManager.TemplateComposer.IFramePanel.superclass.afterRender.apply(this, arguments);

                var frameElement = Ext.getCmp(frameId).el;
                frameElement.addListener('load', onFrameLoad, this);

                this.on('resize', function() {
                    this.hostToIFrame.publish('resize');
                }, this);
            },

            setLocation: function(url) {
                detachFrame();
                getFrameDom().src = url;
            },

            getLocation: function() {
                return getFrameLocation();
            },

            getElement: function(id) {
                return getFrameDocument().getElementById(id);
            },

            reload: function() {
                detachFrame();
                getFrame().location.reload(true);
            },

            createHeadFragment: function() {
                // create an object to add elements to the iframe head using a DOM document fragment when possible

                var frameDocument, documentFragment, api;

                frameDocument = getFrameDocument();

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

                    Ext.iterate(attributes, function (attribute, value) {
                        element[attribute] = value;
                    });

                    if (documentFragment === undefined) {
                        documentFragment = getFrameDocument().createDocumentFragment();
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
            },

            unmask: function() {
                this.el.unmask();
            },

            getScrollPosition: function() {
                var frame = getFrame();
                return {
                    x: frame.pageXOffset,
                    y: frame.pageYOffset
                };
            },

            scrollBy: function(x, y) {
                getFrame().scrollBy(x, y);
            },

            isValidSession: function(sessionCookie) {
                var result = false;

                Ext.each(getFrameDocument().cookie.split(';'), function(keyValue) {
                    var equalsIndex, key, value;

                    equalsIndex = keyValue.indexOf('=');
                    key = keyValue.substr(0, equalsIndex).trim();
                    value = keyValue.substr(equalsIndex + 1).trim();

                    if (key === 'JSESSIONID' && value === sessionCookie) {
                        result = true;
                        return false;
                    }
                }, this);

                return result;
            }

        };
    }()));
    Ext.reg('Hippo.ChannelManager.TemplateComposer.IFramePanel', Hippo.ChannelManager.TemplateComposer.IFramePanel);

}());
