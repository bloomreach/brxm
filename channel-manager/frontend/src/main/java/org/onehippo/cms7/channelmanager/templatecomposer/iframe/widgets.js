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

    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame.UI', 'Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container', 'Hippo.ChannelManager.TemplateComposer.IFrame.UI.ContainerItem');

    var hostToIFrame, iframeToHost;

    hostToIFrame = window.parent.Ext.getCmp('pageEditorIFrame').hostToIFrame;
    iframeToHost = window.parent.Ext.getCmp('pageEditorIFrame').iframeToHost;

    $(window).unload(function() {
        hostToIFrame = null;
        iframeToHost = null;
    });

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
                    disabled : 'hst-overlay-disabled',
                    locked : 'hst-overlay-locked',
                    inner: 'hst-overlay-inner',
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
            var parentOverlay, overlay, self = this, formattedDate;
            if (this.rendered) {
                return;
            }
            this.parent = parent;

            parentOverlay = $.isFunction(parent.getOverlay) ? parent.getOverlay() : document.body;
            overlay = $('<div/>').addClass(this.cls.overlay.base).appendTo(parentOverlay);
            if (this.cls.overlay.mark !== null) {
                overlay.addClass(this.cls.overlay.mark);
            }
            if (this.cls.overlay.custom !== null) {
                overlay.addClass(this.cls.overlay.custom);
            }
            if (this.el.attr(HST.ATTR.HST_CONTAINER_DISABLED) === "true") {
                 overlay.addClass(this.cls.overlay.disabled);
                if (this.el.attr(HST.ATTR.TYPE) === HST.CONTAINER) {
                    if (this.el.attr(HST.ATTR.HST_LOCKED_BY) &&
                           (this.el.attr(HST.ATTR.HST_LOCKED_BY_CURRENT_USER) === "false")) {
                       overlay.addClass(this.cls.overlay.locked);
                        if (this.el.attr(HST.ATTR.HST_LOCKED_ON)) {
                            formattedDate = new Date(parseInt(this.el.attr(HST.ATTR.HST_LOCKED_ON), 10));
                        }
                        if (formattedDate) {
                            overlay.attr("title", "Locked by  '" + this.el.attr(HST.ATTR.HST_LOCKED_BY) + "' on " + formattedDate);
                        } else {
                            overlay.attr("title", "Locked by  '" + this.el.attr(HST.ATTR.HST_LOCKED_BY));
                        }
                    }
                }
            }

            overlay.css('position', 'absolute');
            overlay.attr(HST.ATTR.ID, this.id);
            overlay.attr('id', this.overlayId);

            overlay.hover(function() {
                self.onMouseOver(this);
            }, function() {
                self.onMouseOut(this);
            });
            overlay.click(function() {
                self.onClick();
            });
            this.overlay = overlay;
            this._syncOverlay();

            this.onRender();

            this.rendered = true;
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
            var el, elOffset, overlay, border, borderWidth, data;
            el = this.getOverlaySource();
            elOffset = el.offset();

            overlay = this.overlay;

            //test for single border and assume it all around.
            //TODO: test all borders
            border = overlay.css('border-left-width');
            borderWidth = 0;
            if (border === 'thin') {
                borderWidth = 1;
            } else if (border === 'medium') {
                borderWidth = 2;
            } else if (border === 'thick') {
                borderWidth = 4;
            } else if (border && border.length > 2) {
                borderWidth = parseFloat(border.substring(0, border.length - 2));
            }

            data = {
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
            this.cls.locked         = 'hst-container-locked';

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
            this.isEmpty = $(this.sel.itemWrapper).size() === 0;

            var self = this;
            $(this.sel.itemWrapper).each(function() {
                self._insertNewItem(this, true);
            });

            this.state = new Hippo.ChannelManager.TemplateComposer.IFrame.UI.DDState(this.items.keySet());
        },

        onRender : function() {
            this._super();
            this._renderItems();
            this._createSortable();
            this._checkEmpty();
            this.sync();
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
            var item, itemElement;

            item = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.createOrRetrieve(element);
            if (item !== null) {
                this.items.put(item.id, item, index);
                if(!exists) {
                    itemElement = this.createItemElement(item.element);
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
            var tmpCls, item;

            if(this.items.size() === 0) {
                if(!this.el.hasClass(this.cls.emptyContainer)) {
                    this.el.addClass(this.cls.emptyContainer);
                    this.overlay.addClass(this.cls.emptyContainer);
                    tmpCls = this.cls.item;
                    this.cls.item = this.cls.emptyItem;
                    item = this.createItemElement($('<div class="empty-container-placeholder">Drop Component Here</div>').hide()[0]);
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
                    console.warn('ContainerItem with id=' + this.id + ' is not found in active map.');
                }
            });
        },

        _createSortable : function() {
            //instantiate jquery.UI sortable
            if (this.el.attr(HST.ATTR.HST_CONTAINER_DISABLED) === "true") {
                return;
            }
            $(this.sel.sortable).sortable({
                items: this.sel.sort.itemsRel,
                connectWith: '.' + this.cls.overlay.base,
                start   : $.proxy(this.ddOnStart, this),
                stop    : $.proxy(this.ddOnStop, this),
                update  : $.proxy(this.ddOnUpdate, this),
                receive : $.proxy(this.ddOnReceive, this),
                remove  : $.proxy(this.ddOnRemove, this),
                over    : $.proxy(this.ddOnOver, this),
                change  : $.proxy(this.ddOnChange, this),
                tolerance : this.ddTolerance
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

        _disableClick: function(uiItem) {
            // workaround for CMS7-7271: disable the click event since FF >= 15 triggers it on drag
            uiItem.unbind("click");
            uiItem.bind("click", function(event) {
                event.stopImmediatePropagation();
            });
        },

        ddOnStart : function(event, ui) {
            var id, item;
            id = $(ui.item).attr(HST.ATTR.ID);
            item = this.items.get(id);
            this.parent.onDragStart(ui, this);
            item.onDragStart(event, ui);
            this._disableClick(ui.item);
        },

        ddOnStop: function(event, ui) {
            var id, item;
            id = $(ui.item).attr(HST.ATTR.ID);
            if(this.items.containsKey(id)) {
                item = this.items.get(id);
                item.onDragStop(event, ui);
            }
            this.parent.onDragStop(ui);
            this._syncAll();
        },

        ddOnUpdate : function(event, ui) {
            this.state.syncItemsWithOverlayOrder = true;
            this._disableClick(ui.item);
        },

        ddOnOver : function(event, ui) {
            this.parent.onOver(ui, this);
        },

        ddOnReceive : function(event, ui) {
            var id, item, self, itemId;
            id = ui.item.attr(HST.ATTR.ID);
            item = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.getById(id);
            self = this;
            $(this.sel.sort.items).each(function(index) {
                itemId = $(this).attr(HST.ATTR.ID);
                if (itemId === id) {
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
            var prev, next, getEl, original;
            if (ui.placeholder.siblings().length === 0) {
                //draw indicator inside empty container
                this.draw.inside(this.el, el, this.direction);
            } else {
                prev = ui.placeholder.prev();
                next = ui.placeholder.next();
                getEl = function(_el) {
                    return Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.getById(_el.attr(HST.ATTR.ID)).el;
                };
                original = ui.item[0];
                if(prev[0] === original || (next.length > 0 && next[0] === original)) {
                    this.draw.inside(getEl(ui.item), el, this.direction);
                } else {
                    if(prev.length === 0) {
                        this.draw.before(getEl(next), el, this.direction);
                    } else if (next.length === 0) {
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
                data.width -= total;
                data.height -= total;
                this.parentMargin = data.overlayBorder;
            }
            return data;
        },

        highlight : function() {
            if (this.el.attr(HST.ATTR.HST_CONTAINER_DISABLED)) {
                this.overlay.removeClass(this.cls.overlay.disabled);
            } else {
                this.overlay.addClass(this.cls.highlight);
            }
        },

        unhighlight : function() {
            if (this.el.attr(HST.ATTR.HST_CONTAINER_DISABLED)) {
                this.overlay.addClass(this.cls.overlay.disabled);
            } else {
                this.overlay.removeClass(this.cls.highlight);
            }
        },

        checkState : function() {
            var lookup, items, self, order, container, rearrange, currentOrder;
            if (this.state.checkEmpty) {
                this._checkEmpty();
            }

            if (this.state.syncOverlaysWithItemOrder) {
                lookup = this.items.getIndexMap();
                items = $(this.sel.sort.items).get();
                items.sort(function(a, b) {
                    a = lookup[$(a).attr(HST.ATTR.ID)];
                    b = lookup[$(b).attr(HST.ATTR.ID)];
                    return (a < b) ? -1 : (a > b) ? 1 : 0;
                });
                self = this;
                $.each(items, function(idx, itm) {
                    self.overlay.append(itm);
                });

            } else if (this.state.syncItemsWithOverlayOrder) {
                order = [];
                $(this.sel.sort.items).each(function() {
                    var id = $(this).attr(HST.ATTR.ID);
                    order.push(id);
                });
                this.items.updateOrder(order);

                lookup = this.items.getIndexMap();
                container = $(this.sel.container);
                items = $(this.sel.itemWrapper).get();
                items.sort(function(a, b) {
                    a = lookup[$(a).attr(HST.ATTR.ID)];
                    b = lookup[$(b).attr(HST.ATTR.ID)];
                    return (a < b) ? -1 : (a > b) ? 1 : 0;
                });
                $.each(items, function(idx, itm) {
                    container.append(itm);
                });
            }

            rearrange = null;

            currentOrder = this.items.keySet();
            if (this.state.orderChanged(currentOrder)) {
                this.state.previousOrder = currentOrder;
                this.parent.requestSync();
                rearrange = {id: this.id, children: currentOrder};
            }
            this.state.reset();

            return rearrange;
        },

        toggleNoHover : function() {
            this._super();
            this.eachItem(function(k, item) {
                item.toggleNoHover();
            });
        },

        sync : function() {
            this._syncOverlay();
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
            var item;
            if(this.items.containsKey(id)) {
                item = this.items.remove(id);
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
            if ($(this.sel.append.item).size() === 0) {
                $(this.sel.append.container).append(item);
            } else {
                if(index > -1) {
                    if(index === 0) {
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

            this.direction = HST.DIR.VERTICAL;
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

            this.direction = HST.DIR.VERTICAL;
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

            this.direction = HST.DIR.VERTICAL;
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

            this.direction = HST.DIR.VERTICAL;
        },

        createItemElement : function(element) {
            return $('<div class="' + this.cls.item + '"></div>').append(element);
        }

    });
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.register('HST.vBox', Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.VerticalBox);

    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Span = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Base.extend({

        init : function(id, element, resources) {
            this._super(id, element, resources);

            this.sel.append.item = this.sel.container + ' > span.' + this.cls.item;
            this.sel.append.container = this.sel.container;
            this.sel.append.insertAt = this.sel.container + ' > span';

            this.direction = HST.DIR.HORIZONTAL;
        },

        createItemElement : function(element) {
            return $('<span class="' + this.cls.item + '"></span>').append(element);
        }

    });
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.register('HST.Span', Hippo.ChannelManager.TemplateComposer.IFrame.UI.Container.Span);


    //Container items
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.ContainerItem.Base = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Widget.extend({
        init : function(id, element, resources) {
            var el, tmp;
            this._super(id, element, resources);

            this.scopeId = 'ContainerItem';

            this.cls.selected = this.cls.selected + '-containerItem';
            this.cls.activated = this.cls.activated + '-containerItem';
            this.cls.overlay.mark = 'hst-overlay-container-item';

            // FIXME: is this used?
            el = $(element);
            tmp = el.attr("hst:temporary");
            this.isTemporary = typeof tmp !== 'undefined';
            if(this.isTemporary) {
                el.html(this.resources['base-container-item-temporary']);
            }
        },

        onRender : function() {
            var element, deleteButton;
            this.overlay.append($('<div/>').addClass(this.cls.overlay.inner));

            this.menu = $('<div/>').addClass('hst-overlay-menu');

            element = this.element;
            if (!this.el.attr(HST.ATTR.HST_CONTAINER_DISABLED)) {
                deleteButton = $('<div/>').addClass('hst-overlay-menu-button');
                deleteButton.click(function(e) {
                    e.stopPropagation();
                    iframeToHost.publish('remove', element);
                });
                this.menu.append(deleteButton);
            }
            this.overlay.append(this.menu);
         },

        selectVariant: function(variant, callback) {
            var data = {};
            if (this.el.attr(HST.ATTR.URL)) {
                this.el.attr(HST.ATTR.VARIANT, variant);
                data[HST.ATTR.VARIANT] = variant;
                $.ajax({
                    url: this.el.attr(HST.ATTR.URL),
                    context: this,
                    data: data,
                    dataType: 'html',
                    success: function(response) {
                        var emptyElementHeight, intervalCounter, interval, containerElement, componentElement;
                        containerElement = this.el;
                        containerElement.html('');
                        emptyElementHeight = containerElement.height();

                        componentElement = $(response);
                        /*
                         When response contains hst-container-item class element, the content of that
                         element is copied to the container item element and all attributes will
                         be copied. By default the full response will be place in the container
                         element.
                          */
                        if(componentElement.hasClass(HST.CLASS.ITEM)) {
                            // Copy attributes to container item element
                            this.copyAttributes(componentElement, containerElement);
                            // Set the inner html of component's container item to container HTML
                            containerElement.html(componentElement.html());
                        } else {
                            // Set the plain response to the container HTML
                            containerElement.html(response);
                        }

                        // poll for five seconds to check the component is rendered
                        intervalCounter = 0;
                        interval = window.setInterval(function() {
                            if (intervalCounter > 50 || containerElement.height() !== emptyElementHeight) {
                                window.clearInterval(interval);
                                callback();
                            }
                            intervalCounter++;
                        }, 100);
                    }
                });
            }
        },

        copyAttributes: function(sourceElement, targetElement) {
            var attributes = sourceElement.prop("attributes");
            $.each(attributes, function(i, attribute){
                targetElement.attr(attribute.name, attribute.value);
            });
        },

        sync: function() {
            this._super();
        },

        getOverlayData: function(data) {
            var parentOffset = this.parent.overlay.offset();
            data.position = 'inherit';
            data.left -= (parentOffset.left + this.parent.parentMargin);
            data.top -= (parentOffset.top + this.parent.parentMargin);
            data.width  -= data.overlayBorder*2;
            data.height -= data.overlayBorder*2;

            return data;
        },

        getOverlaySource: function() {
            return $(this.element);
        },

        onClick : function() {
            var id, forcedVariant, containerDisabled;
            if (this.isTemporary) {
                iframeToHost.publish('refresh');
            } else {
                id = this.element.getAttribute('id');
                forcedVariant = this.el.attr(HST.ATTR.VARIANT);
                containerDisabled = Hippo.Util.getBoolean(this.el.attr(HST.ATTR.HST_CONTAINER_DISABLED));
                iframeToHost.publish('onclick', {
                    elementId: id,
                    forcedVariant: forcedVariant,
                    containerDisabled: containerDisabled
                });
            }
        },               

        onDragStart: function(event, ui) {
            $(this.element).addClass('hst-item-ondrag');
            this.menu.hide();
        },

        onDragStop: function(event, ui) {
            $(this.element).removeClass('hst-item-ondrag');
            this.menu.show();
        },

        onDestroy: function() {
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

        orderChanged: function(test) {
            var i, len;

            len = test.length;

            if (len !== this.previousOrder.length) {
                return true;
            }

            for (i = 0; i < len; i++) {
                if (test[i] !== this.previousOrder[i]) {
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

}(jQuery));
