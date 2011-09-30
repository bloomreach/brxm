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
    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame.UI', 'Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container', 'Hippo.ChannelManager.TemplateComposer.IFrame.UI.ContainerItem');

    var Main = Hippo.ChannelManager.TemplateComposer.IFrame.Main;

    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Widget = Class.extend({
        init : function(id, element, resources) {
            this.id = id;
            this.scopeId = 'Widget';
            this.overlayId = id + '-overlay';
            this.element = element;
            this.el = $(element);
            this.resources = resources;

            this.parent = null;

            this.noHover = true;

            //selector shortcuts
            this.sel = {
                self : '#' + element.id,
                overlay : '#' + this.overlayId
            };

            //class values
            this.cls = {
                selected: 'hst-selected',
                activated: 'hst-activated',
                overlay : {
                    base: 'hst-overlay',
                    hover : 'hst-overlay-hover',
                    inherited : 'hst-overlay-inherited',
                    mark : null,
                    custom : null
                }
            };

            this.rendered = false;
        },

        select : function() {
            $(this.element).addClass(this.cls.selected);
            $(this.overlay).addClass(this.cls.selected);
        },

        deselect : function() {
            $(this.element).removeClass(this.cls.selected);
            $(this.overlay).removeClass(this.cls.selected);
        },

        activate : function() {
            $(this.element).addClass(this.cls.activated);
        },

        deactivate : function() {
            $(this.element).addClass(this.cls.activated);
        },

        render : function(parent) {
            console.log('render to '+parent);
            if (this.rendered) {
                return;
            }
            this.parent = parent;

            var parentOverlay = $.isFunction(parent.getOverlay) ? parent.getOverlay() : document.body;
            var overlay = $('<div/>').addClass(this.cls.overlay.base).appendTo(parentOverlay);
            if(this.cls.overlay.mark != null) {
                overlay.addClass(this.cls.overlay.mark);
            }
            if(this.cls.overlay.custom != null) {
                overlay.addClass(this.cls.overlay.custom);
            }
            if (this.el.attr(HST.ATTR.INHERITED)) {
                overlay.addClass(this.cls.overlay.inherited);
            }
            overlay.css('position', 'absolute');
            overlay.attr(HST.ATTR.ID, this.id);
            overlay.attr('id', this.overlayId);

            var self = this;
            overlay.hover(function() {
                Main.publish('mouseOverWidget', this);
                self.onMouseOver(this);
            }, function() {
                Main.publish('mouseOutWidget', this);
                self.onMouseOut(this);
            });
            overlay.click(function() {
                self.onClick();
            });
            this.overlay = overlay;
            this._syncOverlay();

            this.onRender();

            this.rendered = true;
            console.log('after render to '+parent);
        },

        updateSharedData: function(facade) {
            this.items.each(function(key, item) {
                item.updateSharedData(facade);
            });
        },

        toggleNoHover : function() {
            this.noHover = !this.noHover;
        },

        sync: function() {
            this._syncOverlay();
        },

        destroy : function() {
            this.onDestroy();
            this.rendered = false;
        },

        getOverlay : function() {
            return this.overlay;
        },

        onClick : function() {
        },

        onMouseOver : function(element) {
            if (!this.noHover) {
                this.getOverlay().addClass(this.cls.overlay.hover);
            }
        },

        onMouseOut : function(element) {
            if (!this.noHover) {
                this.getOverlay().removeClass(this.cls.overlay.hover);
            }
        },

        onRender  : function() {
        },

        onDestroy : function() {
        },

        _syncOverlay : function() {
            var el = this.getOverlaySource();
            var elOffset = el.offset();

            var overlay = this.overlay;

            //test for single border and assume it all around.
            //TODO: test all borders
            var border = overlay.css('border-left-width');
            var borderWidth = 0;
            if (border === 'thin') {
                borderWidth = 1;
            } else if (border === 'medium') {
                borderWidth = 2;
            } else if (border === 'thick') {
                borderWidth = 4;
            } else if (border && border.length > 2) {
                borderWidth = parseFloat(border.substring(0, border.length - 2));
            }

            var data = {
                left: elOffset.left,
                top: elOffset.top,
                width : el.outerWidth(),
                height: el.outerHeight(),
                position: 'absolute',
                overlayBorder: borderWidth
            };

            data = this.getOverlayData(data);
            this.overlay.
                    css('left', data.left).
                    css('top', data.top).
                    css('position', data.position).
                    width(data.width).
                    height(data.height);

            this._cachedOverlayData = data;
            this.onSyncOverlay();
        },

        getOverlayData : function(data) {
        },

        getOverlaySource : function() {
            return $(this.element);
        },

        onSyncOverlay : function() {
        }
    });

    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Widget.extend({
        init : function(id, element, resources) {
            this._super(id, element, resources);

            this.items = new Hippo.Util.OrderedMap();

            this.ddTolerance = 'intersect';
            this.dropIndicator = null;
            this.direction = HST.DIR.VERTICAL;
            this.draw = new Hippo.Util.Draw({min: 3, thresholdLow: 0});

            this.parentMargin = 0; //margin of overlay

            this.cls.selected       = this.cls.selected + '-container';
            this.cls.activated      = this.cls.activated + '-container';
            this.cls.highlight      = 'hst-highlight';
            this.cls.overlay.custom = 'hst-overlay-container';

            this.cls.container      = HST.CLASS.CONTAINER;
            this.cls.item           = HST.CLASS.ITEM;
            this.cls.emptyContainer = 'hst-empty-container';
            this.cls.emptyItem      = 'hst-empty-container-item';
            this.cls.overlay.item   = 'hst-overlay-container-item';

            this.sel.container      = this.sel.self;
            this.sel.itemWrapper    = this.sel.self + ' .' + this.cls.item;

            this.sel.sortable       = this.sel.overlay;
            this.sel.sort = {
                items : this.sel.sortable + ' .' + this.cls.overlay.item,
                itemsRel : '.' + this.cls.overlay.item
            };

            this.sel.append = {
                item    : '',
                container : '',
                insertAt : ''
            };

            //workaround: set to opposite to evoke this.sync() to render an initially correct UI
            this.isEmpty = $(this.sel.itemWrapper).size() == 0;

            var self = this;
            $(this.sel.itemWrapper).each(function() {
                self._insertNewItem(this, true);
            });

            this.state = new Hippo.ChannelManager.TemplateComposer.IFrame.UI.DDState(this.items.keySet());
        },

        onRender : function() {
            this._super();
            console.log('render items');
            this._renderItems();
            console.log('create sortable');
            this._createSortable();
            console.log('checkEmpty');
            this._checkEmpty();
            console.log('sync');
            this.sync();
            console.log('after sync');
        },

        onDestroy: function() {
            this._destroySortable();
        },

        _renderItems : function() {
            this.eachItem(function(key, item) {
                this._renderItem(item);
            });
        },

        _renderItem : function(item) {
            item.render(this);
        },

        _insertNewItem : function(element, exists, index) {
            var item = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.createOrRetrieve(element);
            if (item !== null) {
                this.items.put(item.id, item, index);
                if(!exists) {
                    var itemElement = this.createItemElement(item.element);
                    this.appendItem(itemElement, index);
                }
            }
            return item;
        },

        _syncAll : function() {
            this.parent.checkStateChanges();
        },

        //if container is empty, make sure it still has a size so items form a different container can be dropped
        _checkEmpty : function() {
            if(this.items.size() == 0) {
                if(!this.el.hasClass(this.cls.emptyContainer)) {
                    this.el.addClass(this.cls.emptyContainer);
                    this.overlay.addClass(this.cls.emptyContainer);
                    var tmpCls = this.cls.item;
                    this.cls.item = this.cls.emptyItem;
                    var item = this.createItemElement($('<div class="empty-container-placeholder">Drop Component Here</div>').hide()[0]);
                    this.appendItem(item);
                    this.cls.item = tmpCls;
                }
            } else if(this.el.hasClass(this.cls.emptyContainer)) {
                this.el.removeClass(this.cls.emptyContainer);
                this.overlay.removeClass(this.cls.emptyContainer);
                $(this.sel.container + ' .' + this.cls.emptyItem).remove();
            }
        },

        _syncItems : function(quiet) {
            this.eachItem(function(key, item) {
                if (typeof item !== 'undefined') {
                    item.sync.call(item);
                } else if(!quiet) {
                    console.warn('ContainerItem with id=' + id + ' is not found in active map.');
                }
            });
            console.log('after _syncItems');
        },

        _createSortable : function() {
            //instantiate jquery.UI sortable
            if (this.el.attr(HST.ATTR.INHERITED)) {
                return;
            }
            $(this.sel.sortable).sortable({
                //revert: 100,
                items: this.sel.sort.itemsRel,
                connectWith: '.' + this.cls.overlay.base,
                start   : $.proxy(this.ddOnStart, this),
                stop    : $.proxy(this.ddOnStop, this),
                update  : $.proxy(this.ddOnUpdate, this),
                receive : $.proxy(this.ddOnReceive, this),
                remove  : $.proxy(this.ddOnRemove, this),
                tolerance : this.ddTolerance,
                change : $.proxy(this.ddOnChange, this)
            }).disableSelection();
        },

        _destroySortable : function() {
            $(this.sel.sortable).sortable('destroy');
        },

        beforeDrag : function() {
            this.toggleNoHover();
        },

        afterDrag : function() {
            this.toggleNoHover();
            this.draw.reset();
        },

        ddOnStart : function(event, ui) {
            var id = $(ui.item).attr(HST.ATTR.ID);
            var item = this.items.get(id);
            this.parent.onDragStart(ui, this);
            item.onDragStart(event, ui);
        },

        ddOnStop: function(event, ui) {
            var id = $(ui.item).attr(HST.ATTR.ID);
            if(this.items.containsKey(id)) {
                var item = this.items.get(id);
                item.onDragStop(event, ui);
            }
            this.parent.onDragStop(ui);
            this._syncAll();
        },

        ddOnUpdate : function(event, ui) {
            this.state.syncItemsWithOverlayOrder = true;
        },

        ddOnReceive : function(event, ui) {
            var id = ui.item.attr(HST.ATTR.ID);
            var item = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.getById(id);
            var self = this;
            $(this.sel.sort.items).each(function(index) {
                var itemId = $(this).attr(HST.ATTR.ID);
                if(itemId == id) {
                    item.onDragStop(event, ui);
                    item.destroy();
                    self.add(item.element, index);

                    self.state.syncOverlaysWithItemOrder = true;
                    return false;
                }
            });
        },

        ddOnRemove : function(event, ui) {
            var id = $(ui.item).attr(HST.ATTR.ID);
            this.removeItem(id, true);
        },

        ddOnChange : function(event, ui) {
            this.parent.onDrag(ui, this);
        },

        drawDropIndicator : function(ui, el) {
            if(ui.placeholder.siblings().length == 0) {
                //draw indicator inside empty container
                this.draw.inside(this.el, el, this.direction);
            } else {
                var prev = ui.placeholder.prev();
                var next = ui.placeholder.next();
                var getEl = function(_el) {
                    return Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.getById(_el.attr(HST.ATTR.ID)).el;
                };
                var original = ui.item[0];
                if(prev[0] == original || (next.length > 0 && next[0] == original)) {
                    this.draw.inside(getEl(ui.item), el, this.direction);
                } else {
                    if(prev.length == 0) {
                        this.draw.before(getEl(next), el, this.direction);
                    } else if (next.length == 0) {
                        this.draw.after(getEl(prev), el, this.direction);
                    } else {
                        this.draw.between(getEl(prev), getEl(next), el, this.direction);
                    }
                }
            }
        },

        getOverlayData : function(data) {
            if(data.overlayBorder > 0) {
                var total = data.overlayBorder * 2;
                data.left -= total;
                data.top -= total;
                data.width += total;
                data.height += total;

                this.parentMargin = data.overlayBorder;
            }
            return data;
        },

        highlight : function() {
            if (this.el.attr(HST.ATTR.INHERITED)) {
                this.overlay.removeClass(this.cls.overlay.inherited);
            } else {
                this.overlay.addClass(this.cls.highlight);
            }
        },

        unhighlight : function() {
            if (this.el.attr(HST.ATTR.INHERITED)) {
                this.overlay.addClass(this.cls.overlay.inherited);
            } else {
                this.overlay.removeClass(this.cls.highlight);
            }
        },

        checkState : function() {
            console.log('checkState of '+this.id);
            if (this.state.checkEmpty) {
                this._checkEmpty();
            }

            if (this.state.syncOverlaysWithItemOrder) {
                var lookup = this.items.getIndexMap();
                var items = $(this.sel.sort.items).get();
                items.sort(function(a, b) {
                    a = lookup[$(a).attr(HST.ATTR.ID)];
                    b = lookup[$(b).attr(HST.ATTR.ID)];
                    return (a < b) ? -1 : (a > b) ? 1 : 0;
                });
                var self = this;
                $.each(items, function(idx, itm) {
                    self.overlay.append(itm);
                });

            } else if (this.state.syncItemsWithOverlayOrder) {
                var order = [];
                $(this.sel.sort.items).each(function() {
                    var id = $(this).attr(HST.ATTR.ID);
                    order.push(id);
                });
                this.items.updateOrder(order);

                var lookup = this.items.getIndexMap();
                var container = $(this.sel.container);
                var items = $(this.sel.itemWrapper).get();
                items.sort(function(a, b) {
                    a = lookup[$(a).attr(HST.ATTR.ID)];
                    b = lookup[$(b).attr(HST.ATTR.ID)];
                    return (a < b) ? -1 : (a > b) ? 1 : 0;
                });
                $.each(items, function(idx, itm) {
                    container.append(itm);
                });
            }

            var currentOrder = this.items.keySet();
            if (this.state.orderChanged(currentOrder)) {
                this.state.previousOrder = currentOrder;
                this.parent.requestSync();
                sendMessage({id: this.id, children: currentOrder}, 'rearrange');
            }
            this.state.reset();
        },

        toggleNoHover : function() {
            this._super();
            this.eachItem(function(k, item) {
                item.toggleNoHover();
            });
        },

        sync : function() {
            console.log('suncOverlay');
            this._syncOverlay();
            console.log('syncItems');
            this._syncItems(true);

        },

        add : function(element, index) {
            var item = this._insertNewItem(element, false, index);
            if (item !== null) {
                this._renderItem(item);
                $(this.sel.sortable).sortable('refresh');
            } else {
                console.warn("Internal error: item {0} inserted at index {1} is null".format(Hippo.Util.getElementPath(element), index));
            }
            this.state.checkEmpty = true;
        },

        remove: function(id) {
        },

        removeItem : function(id, quite) {
            if(this.items.containsKey(id)) {
                var item = this.items.remove(id);
                //remove item wrapper elements
                $(item.element).parents('.' + this.cls.item).remove();
                if(!quite) {
                    item.destroy();
                }

                this.state.checkEmpty = true;
                return true;
            }
            return false;
        },

        hasItem : function(id) {
            return this.items.containsKey(id);
        },

        //Param f is a function with signature function(key, value)
        eachItem : function(f, scope) {
            this.items.each(f, scope || this);
        },

        appendItem : function(item, index) {
            if ($(this.sel.append.item).size() == 0) {
                $(this.sel.append.container).append(item);
            } else {
                if(index > -1) {
                    if(index == 0) {
                        item.insertBefore(this.sel.append.insertAt + ':eq(0)');
                    } else {
                        item.insertAfter(this.sel.append.insertAt + ':eq(' + (index-1) + ')');
                    }
                } else {
                    item.insertAfter(this.sel.append.insertAt + ':last');
                }
            }

        },

        /**
         * Template method for wrapping a containerItem element in a new item element
         */
        createItemElement : function(element) {
        }

    });
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.register('HST.BaseContainer', Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base);

    //Container implementations
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Table = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base.extend({

        init : function(id, element, resources) {
            this._super(id, element, resources);

            this.sel.append.item = this.sel.container + ' > tbody > tr.' + this.cls.item;
            this.sel.append.container = this.sel.container + ' > tbody';
            this.sel.append.insertAt = this.sel.container + ' > tbody > tr';
        },

        createItemElement : function(element) {
            var td = $('<td></td>').append(element);
            return $('<tr class="' + this.cls.item + '"></tr>').append(td);
        }

    });
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.register('HST.Table', Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Table);

    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.UnorderedList = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base.extend({

        init : function(id, element, resources) {
            this._super(id, element, resources);

            this.sel.append.item = this.sel.container + ' > li.' + this.cls.item;
            this.sel.append.container = this.sel.container;
            this.sel.append.insertAt = this.sel.container + ' > li';
        },

        createItemElement : function(element) {
            return $('<li class="' + this.cls.item + '"></li>').append(element);
        }

    });
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.register('HST.UnorderedList', Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.UnorderedList);

    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.OrderedList = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base.extend({

        init : function(id, element, resources) {
            this._super(id, element, resources);

            this.sel.append.item = this.sel.container + ' > li.' + this.cls.item;
            this.sel.append.container = this.sel.container;
            this.sel.append.insertAt = this.sel.container + ' > li';
        },

        createItemElement : function(element) {
            return $('<li class="' + this.cls.item + '"></li>').append(element);
        }

    });
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.register('HST.OrderedList', Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.OrderedList);

    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.VerticalBox = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base.extend({

        init : function(id, element, resources) {
            this._super(id, element, resources);

            this.sel.append.item = this.sel.container + ' > div.' + this.cls.item;
            this.sel.append.container = this.sel.container;
            this.sel.append.insertAt = this.sel.container + ' > div';
        },

        createItemElement : function(element) {
            return $('<div class="' + this.cls.item + '"></div>').append(element);
        }

    });
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.register('HST.vBox', Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.VerticalBox);

    //Container items
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.ContainerItem.Base = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Widget.extend({
        init : function(id, element, resources) {
            console.log('ContainerItem init');
            this._super(id, element, resources);

            this.scopeId = 'ContainerItem';

            this.cls.selected = this.cls.selected + '-containerItem';
            this.cls.activated = this.cls.activated + '-containerItem';
            this.cls.overlay.mark = 'hst-overlay-container-item';

            var el = $(element);
            var tmp = el.attr("hst:temporary");
            this.isTemporary = typeof tmp !== 'undefined';
            if(this.isTemporary) {
                el.html(this.resources['base-container-item-temporary']);
            }

            this.data = {
                name : this.resources['base-container-item-label-loading']
            };
        },

        onRender : function() {
            //var background = $('<div/>').addClass('hst-overlay-background');
            //this.overlay.append(background);

            this.menu = $('<div/>').addClass('hst-overlay-menu'); //.appendTo(document.body);

            var data = {element: this.element};
            if (!this.el.attr(HST.ATTR.INHERITED)) {
                var deleteButton = $('<div/>').addClass('hst-overlay-menu-button').html('X');
                deleteButton.click(function(e) {
                    e.stopPropagation();
                    sendMessage(data, 'remove');
                });
                this.menu.append(deleteButton);
            }

            var nameLabel = $('<div/>').addClass('hst-overlay-name-label');
            this.menu.append(nameLabel);
            this.nameLabel = nameLabel;

            this.renderLabelContents();
          
            this.overlay.append(this.menu);
         },

        sync: function() {
            console.log('ContainerItemBase sync');
            this._super();
            console.log('ContainerItemBase sync menu.position overlay '+ this.overlay +', scope id '+ this.scopeId);
            this.menu.position.call(this.menu, {
                my : 'right top',
                at : 'right top',
                of : this.overlay,
                offset : '-2 2'
            });
            console.log('ContainerItemBase sync after menu.position');
        },

        getOverlayData : function(data) {
            data.position = 'inherit';
            var parentOffset = this.parent.overlay.offset();
            data.left -= (parentOffset.left + this.parent.parentMargin);
            data.top -= (parentOffset.top + this.parent.parentMargin);
            data.width  -= data.overlayBorder*2;
            data.height -= data.overlayBorder*2;

            return data;
        },

        updateSharedData : function(facade) {
            this.data.name = facade.getName(this.id);

            this.renderLabelContents();
        },

        renderLabelContents : function() {
            this.nameLabel.html(this.data.name);
        },

        getOverlaySource : function() {
            return $(this.element);
    //        return $(this.element).parents('.hst-container-item')
        },

        onClick : function() {
            if(this.isTemporary) {
                sendMessage({}, 'refresh');
            } else {
                sendMessage({element: this.element}, 'onclick');
            }
        },

        onDragStart : function(event, ui) {
            $(this.element).addClass('hst-item-ondrag');
            this.menu.hide();
        },

        onDragStop : function(event, ui) {
            $(this.element).removeClass('hst-item-ondrag');
            this.menu.show();
        },

        onDestroy : function() {
            this.overlay.remove();
            this.menu.remove();
        }

    });
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.register('HST.Item', Hippo.ChannelManager.TemplateComposer.IFrame.UI.ContainerItem.Base);


    Hippo.ChannelManager.TemplateComposer.IFrame.UI.DDState = function(initialOrder) {
        this.checkEmpty = false;

        this.syncItemsWithOverlayOrder = false;
        this.syncOverlaysWithItemOrder = false;

        this.previousOrder = initialOrder;

    };

    Hippo.ChannelManager.TemplateComposer.IFrame.UI.DDState.prototype = {

        orderChanged : function(test) {
            if(test.length != this.previousOrder.length) {
                return true;
            }

            for (var i=0; i<test.length; i++) {
                if(test[i] != this.previousOrder[i]) {
                    return true;
                }
            }
            return false;
        },

        reset : function() {
            this.checkEmpty = false;

            this.syncItemsWithOverlayOrder = false;
            this.syncOverlaysWithItemOrder = false;
        }
    };

})(jQuery);