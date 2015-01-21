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
(function($) {
    "use strict";

    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame.UI');
    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame.PageHostMessageHandler');

    var hostToIFrame, iframeToHost, Factory, page;

    hostToIFrame = window.parent.Ext.getCmp('pageEditorIFrame').hostToIFrame;
    iframeToHost = window.parent.Ext.getCmp('pageEditorIFrame').iframeToHost;

    $(window).unload(function() {
        hostToIFrame = null;
        iframeToHost = null;
    });

    Factory = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory;

    function startsWith(str, prefix) {
        if (str === undefined || prefix === undefined) {
            return false;
        }
        var start = str.slice(0, prefix.length);
        return start === prefix;
    }

    page = {
        overlay : null,
        current : null,
        containers : {},
        currentContainer: null,
        dropIndicator : null,
        syncRequested : false,
        resources : [],
        preview : false,
        scopeId : 'Page',

        init: function(data) {
            this.overlay = $('<div/>').addClass('hst-overlay-root').hide().appendTo(document.body);
            this.preview = data.previewMode;
            this.resources = data.resources;

            var subscribeIntercepted = function(topic, func, scope) {
                hostToIFrame.subscribe(topic, function() {
                    var value;
                    try {
                        if (typeof Hippo.ChannelManager.TemplateComposer.IFrame.PageHostMessageHandler['pre'+topic] === 'function') {
                            Hippo.ChannelManager.TemplateComposer.IFrame.PageHostMessageHandler['pre'+topic]();
                        }
                    } catch (preCallHandlerException) {
                        console.log('Error calling pre-hostmessage handler. '+preCallHandlerException);
                    }
                    value = func.apply(scope, arguments);
                    try {
                        if (typeof Hippo.ChannelManager.TemplateComposer.IFrame.PageHostMessageHandler['post'+topic] === 'function') {
                            Hippo.ChannelManager.TemplateComposer.IFrame.PageHostMessageHandler['post'+topic](value);
                        }
                    } catch (postCallHandlerException) {
                        console.log('Error calling post-hostmessage handler. '+postCallHandlerException);
                    }
                    return value;
                }, scope);
            };

            subscribeIntercepted('buildoverlay', function() {
                this.createContainers();
                return false;
            }, this);

            subscribeIntercepted('showoverlay', function() {
                this.getOverlay().show();
                $('.empty-container-placeholder').show();
                this.requestSync();
                this.sync();
                return false;
            }, this);

            subscribeIntercepted('hideoverlay', function() {
                this.getOverlay().hide();
                $('.empty-container-placeholder').hide();
                return false;
            }, this);

            hostToIFrame.subscribe('select', function(id) {
                this.select(id);
                return false;
            }, this);

            hostToIFrame.subscribe('deselect', function() {
                this.deselect();
                return false;
            }, this);

            hostToIFrame.subscribe('highlight', function() {
                this.highlight();
                return false;
            }, this);

            hostToIFrame.subscribe('unhighlight', function() {
                this.unhighlight();
                return false;
            }, this);

            hostToIFrame.subscribe('selectVariant', function(id, variant) {
                this.selectVariant(id, variant);
                return false;
            }, this);

            subscribeIntercepted('resize', function() {
                this.requestSync();
                this.sync();
                return false;
            }, this);

            // intercept all clicks on external links: open them in a new tab if confirmed by the user
            $('a').each(function() {
                var link = $(this),
                    url = link.prop('href');
                if (!startsWith(url, data.internalLinkUrlPrefix)) {
                    link.attr('target', '_blank');
                    link.click(function(event) {
                        var ok = confirm(data.resources['confirm-open-external-url']);
                        if (!ok) {
                            event.preventDefault();
                        }
                    });
                }
            });
        },

        createContainer : function(element, page) {
            var container = Factory.createOrRetrieve.call(Factory, element);
            if (container === null) {
                return null;
            }
            this.containers[container.id] = container;

            container.render(page);
            return container;
        },

        retrieve : function(id) {
            var o = Factory.getById.call(Factory, id);
            if (o === null) {
                iframeToHost.exception(this.resources['manager-object-not-found'].format(id));
            }
            return o;
        },

        createContainers : function() {
            var self = this, container;
            try {
                $('.' + HST.CLASS.CONTAINER).each(function() {
                    container = self.createContainer(this, self);
                });
            } catch(e) {
                iframeToHost.exception('Error creating containers.', e);
            }
        },

        select: function(id) {
            var selection = this.retrieve(id);
            if (this.current === selection) {
                return;
            } else if (this.current !== null) {
                this.current.deselect();
            }
            this.current = selection;
            this.current.select();
        },

        deselect : function() {
            if (this.current !== null) {
                this.current.deselect();
                this.current = null;
                iframeToHost.publish('deselect');
            }
        },

        add: function(element, parentId) {
            if (this.containers[parentId] !== undefined) {
                var container = this.containers[parentId];
                container.add(element);
                this.checkStateChanges();
            }
        },

        remove : function(element) {
            var type, id, container;
            if (!element.hasAttribute(HST.ATTR.ID)) {
                element = $(element).parents('.' + HST.CLASS.CONTAINER)[0];
            }

            type = element.getAttribute(HST.ATTR.TYPE);
            id = element.getAttribute(HST.ATTR.ID);

            if (type === HST.CONTAINERITEM) {
                container = this.containers[id];
                if(!!container && container.removeItem(id)) {
                    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.deleteObjectRef(id);
                }
            } else if (type === HST.CONTAINER) {
                container = this.containers[id];
                if (!!container) {
                    container.remove();
                    delete this.containers[id];
                    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.deleteObjectRef(id);
                }
            }
            this.checkStateChanges();
        },

        onDragStart : function(ui, container) {
            if (this.dropIndicator === null) {
                this.dropIndicator = $('<div id="hst-drop-indicator"/>').appendTo(document.body);
                this.dropIndicator.css('position', 'absolute');
            }
            this.onDrag(ui, container);
            $.each(this.containers, function(key, value) {
                value.beforeDrag();
            });
        },

        onDrag : function(ui, container) {
            var c = this.currentContainer === null ? container : this.currentContainer;
            c.drawDropIndicator(ui, this.dropIndicator);
        },

        onOver : function(ui, container) {
            this.currentContainer = container;
            this.currentContainer.drawDropIndicator(ui, this.dropIndicator);
        },

        onDragStop : function() {
            if (this.dropIndicator !== null) {
                this.dropIndicator.remove();
                this.dropIndicator = null;
            }
            $.each(this.containers, function(key, value) {
                value.afterDrag();
            });
            this.currentContainer = null;
        },

        highlight : function() {
            $.each(this.containers, function(key, value) {
                value.highlight();
            });
        },

        unhighlight : function() {
            $.each(this.containers, function(key, value) {
                value.unhighlight();
            });
        },

        selectVariant : function(id, variant) {
            var o = this.retrieve(id),
                self = this;
            o.selectVariant(variant, function() {
                self.requestSync();
                self.sync();
            });
        },

        checkStateChanges : function() {
            var rearranges = [];
            $.each(this.containers, function(key, value) {
                var rearrange = value.checkState();
                if (rearrange) {
                    rearranges.push(rearrange);
                }
            });
            iframeToHost.publish('rearrange', rearranges);
            this.sync();
        },

        requestSync : function() {
            this.syncRequested = true;
        },

        sync : function() {
            if(this.syncRequested) {
                $.each(this.containers, function(key, value) {
                    value.sync();
                });
            }
            this.syncRequested = false;
        },

        getOverlay : function() {
            return this.overlay;
        }
    };

    hostToIFrame.subscribe('init', page.init, page);

}(jQuery));