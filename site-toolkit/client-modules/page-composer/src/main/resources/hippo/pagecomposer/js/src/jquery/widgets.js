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

$.namespace('Hippo.PageComposer.UI', 'Hippo.PageComposer.UI.Container', 'Hippo.PageComposer.UI.ContainerItem');

Hippo.PageComposer.UI.Widget = Class.extend({
    init : function(id, element) {

        this.id = id;
        this.overlayId = id + '-overlay';
        this.element = element;

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
                custom : null
            }
        };

        this.rendered = false;
    },

    select : function() {
        $(this.element).addClass(this.cls.selected);
    },

    deselect : function() {
        $(this.element).removeClass(this.cls.selected);
    },

    activate : function() {
        $(this.element).addClass(this.cls.activated);
    },

    deactivate : function() {
        $(this.element).addClass(this.cls.activated);
    },

    render : function(_parent) {
        if (this.rendered) {
            return;
        }
        this.parent = _parent;

        var self = this;
        var parent = _parent || document.body;
        var overlay = $('<div/>').addClass(this.cls.overlay.base).appendTo(parent);
        if(this.cls.overlay.custom != null) {
            overlay.addClass(this.cls.overlay.custom);
        }
        overlay.css('position', 'absolute');
        overlay.attr('hst:id', this.id);
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
        this.getOverlay().addClass(this.cls.overlay.hover);
    },

    onMouseOut : function(element) {
        this.getOverlay().removeClass(this.cls.overlay.hover);
    },

    onRender  : function() {
    },

    onDestroy : function() {
    },

    _syncOverlay : function() {
        var d = this.getOverlayData();
        this.overlay.
                css('left', d.left).
                css('top', d.top).
                css('position', d.position).
                width(d.width).
                height(d.height);

        this.onSyncOverlay();
    },

    getOverlayData : function() {
        var overlay = this.overlay;
        var el = this.getOverlaySource();
        var elOffset = el.offset();
        var left = (elOffset.left + el.outerWidth()) - overlay.width();
        var top = elOffset.top;
        var width = el.outerWidth();
        var height = el.outerHeight();
        var position = 'absolute';

        if (this.parent) {
            var pOffset = this.parent.offset();
            left = left - pOffset.left;
            top = top - pOffset.top;
            position = 'inherit';
        }

        return {
            left: left,
            top: top,
            width : width,
            height: height,
            position: position
        };
    },

    getOverlaySource : function() {
        return $(this.element);
    },

    onSyncOverlay : function() {
    }
});

Hippo.PageComposer.UI.Container.Base = Hippo.PageComposer.UI.Widget.extend({
    init : function(id, element) {
        this._super(id, element);

        this.state = new Hippo.PageComposer.UI.DDState();
        this.items = new Hippo.Util.OrderedMap();

        this.cls.selected       = this.cls.selected + '-container';
        this.cls.activated      = this.cls.activated + '-container';
        this.cls.overlay.custom = 'hst-overlay-container';

        this.cls.container      = 'hst-container';
        this.cls.item           = 'hst-container-item';
        this.cls.emptyContainer = 'hst-empty-container';
        this.cls.emptyItem      = 'hst-empty-container-item';

        this.sel.container      = this.sel.self + ' .' + this.cls.container;
        this.sel.item           = this.sel.self + ' div.componentContentWrapper';

        this.sel.sortable       = this.sel.overlay;
        this.sel.sortableItems  = '.hst-overlay-container-item';

        //workaround: set to opposite to evoke this.sync() to render an initially correct UI
        this.isEmpty = $(this.sel.item).size() > 0;

        var self = this;
        $(this.sel.item).each(function() {
            self._insertNewItem(this, true);
        });
    },

    onRender : function() {
        this._super();
        this._renderItems();
        this._createSortable();
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
        item.render(this.getOverlay());
    },

    _insertNewItem : function(element, exists, index) {
        var item = Hippo.PageComposer.UI.Factory.createOrRetrieve(element);
        this.items.put(item.id, item, index);
        if(!exists) {
            var itemElement = this.createItemElement(item.element);
            this.appendItem(itemElement, index);
        }
        return item;
    },

    _checkState : function() {
        if(this.state.orderChanged) {
            this._updateOrder();
        }
    },

    _updateOrder : function() {
        var order = [], lookup = {}, count = 0;
        var sel = this.sel.overlay + ' ' + this.sel.sortableItems;
        $(sel).each(function() {
            var id = $(this).attr(HST.ATTR.ID);
            order.push(id);
            lookup[id] = count++;
        });
        this.items.updateOrder(order);
        var container = $(this.sel.container);
        var items = container.children('.'+this.cls.item).get();
        items.sort(function(a, b) {
            var aId = $('div.componentContentWrapper', a).attr(HST.ATTR.ID);
            var aIndex = lookup[aId];
            var bId = $('div.componentContentWrapper', b).attr(HST.ATTR.ID);
            var bIndex = lookup[bId];
            return (aIndex < bIndex) ? -1 : (aIndex > bIndex) ? 1 : 0;
        });
        $.each(items, function(idx, itm) {
            container.append(itm);
        });
        sendMessage({id: this.id, children: order}, 'rearrange');
        this.syncAll();
    },

    _createSortable : function() {
        //instantiate jquery.UI sortable
        $(this.sel.sortable).sortable({
            items: this.sel.sortableItems,
            connectWith: '.' + this.cls.overlay.base,
            start   : $.proxy(this.ddOnStart, this),
            stop    : $.proxy(this.ddOnStop, this),
            helper  : $.proxy(this.ddHelper, this),
            update  : $.proxy(this.ddOnUpdate, this),
            receive : $.proxy(this.ddOnReceive, this),
            remove  : $.proxy(this.ddOnRemove, this)
//            revert: 100,
//            placeholder : {
//                element: function(el) {
//                    var w = $(el).width(), h = $(el).height();
//                    return $('<li class="ui-state-highlight placeholdert"></li>').height(h).width(w);
//                },
//                update: function(contEainer, p) {
//                    //TODO
//                }
//
//            }
        }).disableSelection();
    },

    _destroySortable : function() {
        $(this.sel.sortable).sortable('destroy');
    },

    ddOnStart : function(event, ui) {
        this.state.reset();
        var id = $(ui.item).attr(HST.ATTR.ID);
        var item = this.items.get(id);
        item.onDragStart(event, ui);
    },

    ddOnStop: function(event, ui) {
        console.log('stop' + this.id);
        var id = $(ui.item).attr(HST.ATTR.ID);
        if(this.items.containsKey(id)) {
            var item = this.items.get(id);
            item.onDragStop(event, ui);
        }
        this._checkState();
    },

    ddOnUpdate : function(event, ui) {
        console.log('update ' + this.id);
        this.state.orderChanged = true;
    },

    ddOnReceive : function(event, ui) {
        console.log('receive ' + this.id);
        this.add(ui.item);
//        var id = $(ui.item).attr(HST.ATTR.ID);
//        var item = Hippo.PageComposer.UI.Factory.getById(id);
//
//        item.parent = this;
//        this.items.put(item.id, item);
        
    },

    ddOnRemove : function(event, ui) {
        console.log('remove ' + this.id);
        this.removeItem(ui.item);
//        var id = $(ui.item).attr(HST.ATTR.ID);
//        var item = this.items.remove(id);
//        item.destroy();
    },

    ddHelper : function(event, element) {
        var id = element.attr(HST.ATTR.ID);
        var item = this.items.get(id);
        var x = $('<div class="hst-dd-helper">Item: ' + id + '</div>').css('width', '120px').css('height', '40px').offset({top: event.clientY, left:event.clientX}).appendTo(document.body);
        return x;
    },

    add : function(element, index) {
        var item = this._insertNewItem(element, false, index);
        this._renderItem(item);
        $(this.sel.sortable).sortable('refresh');
        this.syncAll();
    },

    remove: function() {
        console.warn('Illegal access, can remove container');
        console.trace();
        //$(this.element).remove();
    },

    hasItem : function(id) {
        return this.items.containsKey(id);
    },

    //Param f is a function with signature function(key, value)
    eachItem : function(f, scope) {
        this.items.each(f, scope || this);
    },

    /**
     * Template method for wrapping a containerItem element in a new item element
     */
    createItemElement : function(element) {
    },

    /**
     * Template method for adding an item to the container
     */
    appendItem : function(data) {
    },

    removeItem : function(element) {
        try {
            var v = Hippo.PageComposer.UI.Factory.verify(element);
            //remove item wrapper elements
            $(element).parents('.' + this.cls.item).remove();
            var item = this.items.remove(v.id);
            item.destroy();
            this.syncAll();
            return true;
        } catch(e) {
            if(Hippo.PageComposer.Main.isDebug()) {
                throw e;
            }
        }
        return false;
    },

    syncAll : function() {
    },

    sync : function() {
        if ($(this.sel.item).size() != this.items.size()) {
            return; //DOM and contianerState are out of sync, wait for update/receive/remove to finish, it will trigger sync again
        }
        this.checkEmpty();
        this.syncOverlays(true);
    },

    syncOverlays : function(quite) {
        this._syncOverlay();
        this.eachItem(function(key, item) {
            if (typeof item !== 'undefined') {
                item.update();
            } else if(!quite && Hippo.PageComposer.Main.isDebug()) {
                console.warn('ContainerItem with id=' + id + ' is not found in active map.');
            }
        });
    },

    //if container is empty, make sure it still has a size so items form a different container can be dropped
    checkEmpty : function() {
        if ($(this.sel.item).size() == 0) {
            if (!this.isEmpty) {
                this.isEmpty = true;

                $(this.element).addClass(this.cls.emptyContainer);
                var tmpCls = this.cls.item;
                this.cls.item = this.cls.emptyItem;
                var item = this.createItemElement($('<div class="empty-container-placeholder">Empty container</div>')[0]);
                this.appendItem(item);
                this.cls.item = tmpCls;
            }
        } else {
            if(this.isEmpty) {
                this.isEmpty = false;
                $(this.element).removeClass(this.cls.emptyContainer);
                $(this.sel.container + ' .' + this.cls.emptyItem).remove();
            }
        }
    }

//    onReceive : function(event, ui) {
//        var el = ui.item.find('.componentContentWrapper');
//        ui.item.replaceWith(this.createItemElement(el));
//        sendMessage({id: this.id, element: el[0]}, 'receiveditem');
//        //this.syncAll();
//    },
//
//    onRemove : function(event, ui) {
//        this.syncAll();
//    }
});
Hippo.PageComposer.UI.Factory.register('HST.BaseContainer', Hippo.PageComposer.UI.Container.Base);

//Container implementations
Hippo.PageComposer.UI.Container.Table = Hippo.PageComposer.UI.Container.Base.extend({

    createItemElement : function(element) {
        var td = $('<td></td>').append(element);
        return $('<tr class="' + this.cls.item + '"></tr>').append(td);
    },

    appendItem : function(item) {
        if ($(this.sel.container + " > tbody > tr").size() == 0) {
            $(this.sel.container + " > tbody").append(item);
        } else {
            item.insertAfter(this.sel.container + " > tbody > tr:last");
        }
    }

});
Hippo.PageComposer.UI.Factory.register('HST.Table', Hippo.PageComposer.UI.Container.Table);

Hippo.PageComposer.UI.Container.UnorderedList = Hippo.PageComposer.UI.Container.Base.extend({

    createItemElement : function(element) {
        return $('<li class="' + this.cls.item + '"></li>').append(element);
    },

    appendItem : function(item) {
        if ($(this.sel.container + " > li").size() == 0) {
            $(this.sel.container).append(item);
        } else {
            item.insertAfter(this.sel.container + " > li:last");
        }
    }
});
Hippo.PageComposer.UI.Factory.register('HST.UnorderedList', Hippo.PageComposer.UI.Container.UnorderedList);

Hippo.PageComposer.UI.Container.OrderedList = Hippo.PageComposer.UI.Container.Base.extend({

    createItemElement : function(element) {
        return $('<li class="' + this.cls.item + '"></li>').append(element);
    },

    appendItem : function(item) {
        if ($(this.sel.container + " > li").size() == 0) {
            $(this.sel.container).append(item);
        } else {
            item.insertAfter(this.sel.container + " > li:last");
        }
    }
});
Hippo.PageComposer.UI.Factory.register('HST.OrderedList', Hippo.PageComposer.UI.Container.OrderedList);

Hippo.PageComposer.UI.ContainerItem.Base = Hippo.PageComposer.UI.Widget.extend({
    init : function(id, element) {
        this._super(id, element);

        this.cls.selected = this.cls.selected + '-containerItem';
        this.cls.activated = this.cls.activated + '-containerItem';
        this.cls.overlay.custom = 'hst-overlay-container-item';

        var el = $(element);
        var tmp = el.attr("hst:temporary");
        this.isTemporary = typeof tmp !== 'undefined';
        if(this.isTemporary) {
            el.html('Click to refresh');
        }
    },

    onRender : function() {
        var data = {element: this.element};
        var deleteButton = $('<div/>').addClass('hst-overlay-menu-button').html('X');
        deleteButton.click(function(e) {
            e.stopPropagation();
            sendMessage(data, 'remove');
        });

        this.getOverlay().append(deleteButton);
        this.sync(); 
    },

    getOverlaySource : function() {
        return $(this.element).parents('.hst-container-item')
    },

    select : function() {
        this._super();
        $(this.getOverlay()).addClass(this.cls.selected);
    },

    deselect : function() {
        this._super();
        $(this.getOverlay()).removeClass(this.cls.selected);
    },

    update : function() {
        this.sync();
    },

    onClick : function() {
        if(this.isTemporary) {
            document.location.href = '';
        } else {
            sendMessage({element: this.element}, 'onclick');
        }
    },

    onDragStart : function(event, ui) {
        $(this.element).addClass('hst-item-ondrag');
    },

    onDragStop : function(event, ui) {
        $(this.element).removeClass('hst-item-ondrag');
    },

    onDestroy : function() {
        if(this.overlay) {
            this.overlay.remove();
        } else {
            console.warn('Overlay not found for remove of ' + this.id);
        }
    }

});
Hippo.PageComposer.UI.Factory.register('HST.Item', Hippo.PageComposer.UI.ContainerItem.Base);


Hippo.PageComposer.UI.DDState = function() {
    this.orderChanged = false;
};

Hippo.PageComposer.UI.DDState.prototype = {
    reset : function() {
        this.orderChanged = false;
    }
};
