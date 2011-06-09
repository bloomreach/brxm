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
jQuery.noConflict();
(function($) {

    $.namespace('Hippo.PageComposer.UI');

    Hippo.PageComposer.UI.Manager = function() {
        this.current = null;
        this.containers = {};
        this.dropIndicator = null;
        this.syncRequested = false;

        this.init();
    };


    //TODO: looks more like a UI.Page component
    Hippo.PageComposer.UI.Manager.prototype = {
        init: function() {

            this.overlay = $('<div/>').addClass('hst-overlay-root').appendTo(document.body);

            //do try/catch because else errors will disappear
            try {
                //attach mouseover/mouseclick for components
                var self = this;
                $('div.componentContentWrapper').each(function(index) {
                    if(Hippo.PageComposer.UI.Factory.isContainer(this)) {
                        self._createContainer(this);
                    }
                });
            } catch(e) {
                console.error(e);
            }
        },

        _createContainer : function(element) {
            var container = Hippo.PageComposer.UI.Factory.createOrRetrieve(element);
            this.containers[container.id] = container;
            container.render(this);
        },

        _retrieve : function(element) {
            var factory = Hippo.PageComposer.UI.Factory;
            var data = factory.verify(element);
            var o = factory.getById(data.id);
            if(o == null) {
                Hippo.PageComposer.Main.die('Object with id ' + data.id + ' not found in registry');
            }
            return o;
        },

        select: function(element) {
            if (this.current != null && this.current.element == element) {
                return;
            }

            this.current = this._retrieve(element);
            this.current.select();
        },

        deselect : function(element) {
            if (this.current != null) {
                this.current.deselect();
                this.current = null;
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
                element = $(element).parents('.componentContentWrapper')[0];
            }

            var d = Hippo.PageComposer.UI.Factory.verify(element);
            if (d.type == HST.CONTAINERITEM) {
                var containerId = $(element).parents('.componentContentWrapper').attr(HST.ATTR.ID);
                var container = this.containers[containerId];
                if(!!container && container.removeItem(d.id)) {
                    Hippo.PageComposer.UI.Factory.deleteObjectRef(d.id);
                }
            } else if (d.type == HST.CONTAINER) {
                var container = this.containers[d.id];
                if (!!container) {
                    container.remove();
                    delete this.containers[id];
                    Hippo.PageComposer.UI.Factory.deleteObjectRef(d.id);
                }
            }
            this.checkStateChanges();
        },

        onDragStart : function(ui, container) {
            this.onDrag(ui, container);
            $.each(this.containers, function(key, value) {
                value.beforeDrag();
            });
        },

        onDrag : function(ui, container) {
            if(this.dropIndicator == null) {
                this.dropIndicator = $('<div id="hst-drop-indicator"/>').appendTo(document.body);
                this.dropIndicator.css('position', 'absolute');
            }
            container.drawDropIndicator(ui, this.dropIndicator);
        },

        onDragStop : function() {
            if(this.dropIndicator != null) {
                this.dropIndicator.remove();
                this.dropIndicator = null;
            }
            $.each(this.containers, function(key, value) {
                value.afterDrag();
            });
        },

        //TODO: implement group handling
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

        checkStateChanges : function() {
            $.each(this.containers, function(key, value) {
                value.checkState();
            });
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

})(jQuery);



