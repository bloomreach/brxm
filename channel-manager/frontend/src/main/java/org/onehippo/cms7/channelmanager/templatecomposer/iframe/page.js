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
(function($) {

    var jQuery = $;
    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame.UI');

    var Main = Hippo.ChannelManager.TemplateComposer.IFrame.Main;
    var Factory = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory;

    var page = {
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

            onhostmessage(function(msg) {
                var facade = msg.data;
                this.createContainers(facade);
                return false;
            }, this, false, 'buildoverlay');

            onhostmessage(function(msg) {
                this.getOverlay().show();
                $('.empty-container-placeholder').show();
                this.requestSync();
                this.sync();
                return false;
            }, this, false, 'showoverlay');

            onhostmessage(function(msg) {
                this.getOverlay().hide();
                $('.empty-container-placeholder').hide();
                return false;
            }, this, false, 'hideoverlay');

            onhostmessage(function(msg) {
                console.log('onhostmessage select');
                this.select(msg.data.id);
                return false;
            }, this, false, 'select');

            onhostmessage(function(msg) {
                console.log('onhostmessage deselect');
                this.deselect();
                return false;
            }, this, false, 'deselect');

            onhostmessage(function(msg) {
                this.highlight(msg.data.groups);
                return false;
            }, this, false, 'highlight');

            onhostmessage(function(msg) {
                this.unhighlight(msg.data.groups);
                return false;
            }, this, false, 'unhighlight');

            onhostmessage(function(msg) {
                this.setParameters(msg.data.id, msg.data.parameters);
                return false;
            }, this, false, 'setParameters');

            onhostmessage(function(msg) {
                this.requestSync();
                this.sync();
                return false;
            }, this, false, 'resize');

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
            if (o == null) {
                Main.die(this.resources['manager-object-not-found'].format(id));
            }
            return o;
        },

        createContainers : function(facade) {
            try {
                //attach mouseover/mouseclick for components
                var self = this;
                $('.' + HST.CLASS.CONTAINER).each(function(index) {
                    var container = self.createContainer(this, self);
                    if (container !== null) {
                        container.updateSharedData(facade);
                    }
                });
            } catch(e) {
                sendMessage({msg: 'Error creating containers.', exception: e}, "iframeexception");
            }
        },

        select: function(id) {
            console.log('this.current = this.retrieve(element);');
            var selection = this.retrieve(id);
            if (this.current == selection) {
                return;
            } else if (this.current != null) {
                this.current.deselect();
            }
            this.current = selection;
            console.log('this.current.select();');
            this.current.select();
        },

        deselect : function() {
            if (this.current != null) {
                this.current.deselect();
                this.current = null;
                sendMessage({}, 'deselect');
            }
        },

        add: function(element, parentId) {
            if (typeof this.containers[parentId] !== 'undefined') {
                var container = this.containers[parentId];
                container.add(element);
                this.checkStateChanges();
            }
        },

        remove : function(element) {
            if (!element.hasAttribute(HST.ATTR.ID)) {
                element = $(element).parents('.'+HST.CLASS.CONTAINER)[0];
            }

            var type = element.getAttribute(HST.ATTR.TYPE);
            var id = element.getAttribute(HST.ATTR.ID);
            var xtype = element.getAttribute(HST.ATTR.XTYPE);

            if (type == HST.CONTAINERITEM) {
                var container = this.containers[id];
                if(!!container && container.removeItem(id)) {
                    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.deleteObjectRef(id);
                }
            } else if (type == HST.CONTAINER) {
                var container = this.containers[id];
                if (!!container) {
                    container.remove();
                    delete this.containers[id];
                    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.deleteObjectRef(id);
                }
            }
            this.checkStateChanges();
        },

        onDragStart : function(ui, container) {
            if(this.dropIndicator == null) {
                this.dropIndicator = $('<div id="hst-drop-indicator"/>').appendTo(document.body);
                this.dropIndicator.css('position', 'absolute');
            }
            this.onDrag(ui, container);
            $.each(this.containers, function(key, value) {
                value.beforeDrag();
            });
        },

        onDrag : function(ui, container) {
            var c = this.currentContainer == null ? container : this.currentContainer;
            c.drawDropIndicator(ui, this.dropIndicator);
        },

        onOver : function(ui, container) {
            this.currentContainer = container;
            this.currentContainer.drawDropIndicator(ui, this.dropIndicator);
        },

        onDragStop : function() {
            if(this.dropIndicator != null) {
                this.dropIndicator.remove();
                this.dropIndicator = null;
            }
            $.each(this.containers, function(key, value) {
                value.afterDrag();
            });
            this.currentContainer = null;
        },

        highlight : function(groups) {
            $.each(this.containers, function(key, value) {
                value.highlight();
            });
        },

        unhighlight : function(groups) {
            $.each(this.containers, function(key, value) {
                value.unhighlight();
            });
        },

        setParameters : function(id, parameters) {
            var o = this.retrieve(id);
            o.setParameters(parameters);
        },

        checkStateChanges : function() {
            var rearranges = [];
            $.each(this.containers, function(key, value) {
                var rearrange = value.checkState();
                if (rearrange) {
                    rearranges.push(rearrange);
                }
            });
            sendMessage(rearranges, 'rearrange');
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

        updateSharedData : function(facade) {
            $.each(this.containers, function(key, value) {
                value.updateSharedData(facade);
            });
        },

        getOverlay : function() {
            return this.overlay;
        }
    };

    Main.subscribe('initialize', page.init, page);

})(jQuery);