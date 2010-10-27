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

        //jquery selector shortcuts
        this.selector = '#' + element.id;
        this.overlaySelector = '#' + this.overlayId;

        //selected classname
        this.selCls = 'hst-selected';
        //activated classname
        this.actCls = 'hst-activated';
        //base overlay classname
        this.overlayCls = 'hst-overlay';
        //overlay hover classname
        this.overlayHoverCls = 'hst-overlay-hover';
        //optional custom classname
        this.overlayCustomCls = null;

        this.rendered = false;
    },

    select : function() {
        $(this.element).addClass(this.selCls);
    },

    deselect : function() {
        $(this.element).removeClass(this.selCls);
    },

    activate : function() {
        $(this.element).addClass(this.actCls);
    },

    deactivate : function() {
        $(this.element).addClass(this.actCls);
    },

    render : function(_parent) {
        if (this.rendered) {
            return;
        }
        this.parent = _parent;

        var self = this;
        var parent = _parent || document.body;
        var overlay = $('<div/>').addClass(this.overlayCls).appendTo(parent);
        if(this.overlayCustomCls != null) {
            overlay.addClass(this.overlayCustomCls);
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
        this.getOverlay().addClass(this.overlayHoverCls);
    },

    onMouseOut : function(element) {
        this.getOverlay().removeClass(this.overlayHoverCls);
    },

    onRender  : function() {
    },

    onDestroy : function() {
    },

    _syncOverlay : function() {
//Webkit does not work with the 'new' position function
//Webkit also doesn't work with setting position through .offset so we are back to css.left/top
//        this.menuOverlay.position({
//            my: 'right top',
//            at: 'right top',
//            of: el
//        });

        var overlay = this.overlay;
        var el = $(this.element);
        var elOffset = $(el).offset();
        var left = (elOffset.left + $(el).outerWidth()) - overlay.width();
        var top = elOffset.top;
        var width = el.outerWidth();
        var height = el.outerHeight();

        if(this.parent) {
            var pOffset = this.parent.offset();
            left = left - pOffset.left;
            top = top- pOffset.top;
            overlay.css('position', 'inherit');
        }

        overlay.css('left', left);
        overlay.css('top', top);
        overlay.width(width);
        overlay.height(height);

        this.onSyncOverlay();
    },

    onSyncOverlay : function() {
    }
});

Hippo.PageComposer.UI.Container.Base = Hippo.PageComposer.UI.Widget.extend({
    init : function(id, element) {
        this._super(id, element);

        this.items = new Hippo.Util.OrderedMap();

        this.itemCls = 'hst-dd-item';
        this.hostCls = 'hst-container';

        this.hostSelector = this.selector + ' .' + this.hostCls;
        this.itemSelector = this.selector + ' div.componentContentWrapper';

        this.dragSelector = '.hst-overlay-containeritem';

        this.selCls = this.selCls + '-container';
        this.actCls = this.actCls + '-container';
        this.overlayCustomCls = 'hst-overlay-container';

        this.emptyCls = 'hst-empty-container';

        //is this needed?
        this.emptyItemCls = 'hst-empty-container-item';

        //workaround: set to opposite to evoke this.sync() to render an initially correct UI
        this.isEmpty = $(this.itemSelector).size() > 0;

        var self = this;
        $(this.itemSelector).each(function() {
            self.insertNewItem(this, true);
        });
    },

    onRender : function() {
        this._super();
        this.eachItem(function(key, item) {
            item.render(this.getOverlay());
        });
        this.enableSortable();
        this.sync();
    },

    enableSortable : function() {
        //instantiate jquery.UI sortable
        $(this.overlaySelector).sortable({
            items: this.dragSelector,
            connectWith: '.' + this.overlayCustomCls,
            start: $.proxy(this.ddOnStart, this),
            stop: $.proxy(this.ddOnStop, this),
            helper: $.proxy(this.ddHelper, this)
//            update: $.proxy(this.onUpdate, this),
//            revert: 100,
//            receive: $.proxy(this.onReceive, this)
//            remove: $.proxy(this.onRemove, this),
//            placeholder : {
//                element: function(el) {
//                    var w = $(el).width(), h = $(el).height();
//                    return $('<li class="ui-state-highlight placeholdert"></li>').height(h).width(w);
//                },
//                update: function(contEainer, p) {
//                    //TODO
//                }
//
//            },
//            sort: function() {
//            },
//            change: function() {
//            }

        }).disableSelection();
    },

    ddOnStart : function() {
        console.log('start');
    },

    ddOnStop: function() {
        console.log('stop');
    },

    ddHelper : function(event, element) {
        var id = element.attr('hst:id');
        var item = this.items.get(id);
        var x = $('<div class="hst-dd-helper">Item: ' + id + '</div>').css('width', '120px').css('height', '40px').offset({top: event.clientY, left:event.clientX}).appendTo(document.body);
        return x;
    },

    onDestroy: function() {
        this.disableSortable();
    },

    disableSortable : function() {
        //destroy jquery.UI sortable
        $(this.hostSelector).sortable('destroy');
    },

    add : function(element, index) {
        var item = this.insertNewItem(element, false, index);
        item.render();
        $(this.hostSelector).sortable('refresh');
        this.syncAll();
    },

    remove: function() {
        console.warn('Illegal access, can remove container');
        console.trace();
        //$(this.element).remove();
    },

    //Param f is a function with signature function(key, value)
    eachItem : function(f, scope) {
        this.items.each(f, scope || this);
    },

    insertNewItem : function(element, exists, index) {
        var item = Hippo.PageComposer.UI.Factory.create(element);
        this.items.put(item.id, item, index);
        if(!exists) {
            var itemElement = this.createItemElement(item.element);
            this.appendItem(itemElement, index);
        }
        return item;
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
            $(element).parents('.' + this.itemCls).remove();
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
        if ($(this.itemSelector).size() != this.items.size()) {
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
        if ($(this.itemSelector).size() == 0) {
            if (!this.isEmpty) {
                this.isEmpty = true;

                $(this.element).addClass(this.emptyCls);
                var tmpCls = this.itemCls;
                this.itemCls = this.emptyItemCls;
                var item = this.createItemElement($('<div class="empty-container-placeholder">Empty container</div>')[0]);
                this.appendItem(item);
                this.itemCls = tmpCls;
            }
        } else {
            if(this.isEmpty) {
                this.isEmpty = false;
                $(this.element).removeClass(this.emptyCls);
                $(this.hostSelector + ' .' + this.emptyItemCls).remove();
            }
        }
    },

    //event listeners

    //called after an update in the order of container items has been invoked
    onUpdate : function(event, ui) {
        var order = [];
        $(this.itemSelector).each(function() {
            order.push(this.id);
        });
        if(this._updateOrder(order)) {
            sendMessage({id: this.id, children: order}, 'rearrange');
            this.syncAll();
        }
    },

    onReceive : function(event, ui) {
        var el = ui.item.find('.componentContentWrapper');
        ui.item.replaceWith(this.createItemElement(el));
        sendMessage({id: this.id, element: el[0]}, 'receiveditem');
        //this.syncAll();
    },

    onRemove : function(event, ui) {
        this.syncAll();
    },

    //private methods
    _updateOrder : function(order) {
        //can add test if order has actually changed to give performance a small boost
        this.items.clear();
        if(order.length > 0) {
            var len = order.length, f = Hippo.PageComposer.UI.Factory;
            for(var i=0; i<len; ++i) {
                var id = order[i];
                var value = f.getById(id);
                if(value != null) {
                    this.items.put(id, value);
                }
            }
        }
        return true;
    }
});
Hippo.PageComposer.UI.Factory.register('HST.BaseContainer', Hippo.PageComposer.UI.Container.Base);

//Container implementations
Hippo.PageComposer.UI.Container.Table = Hippo.PageComposer.UI.Container.Base.extend({

    createItemElement : function(element) {
        var td = $('<td></td>').append(element);
        return $('<tr class="' + this.itemCls + '"></tr>').append(td);
    },

    appendItem : function(item) {
        if ($(this.hostSelector + " > tbody > tr").size() == 0) {
            $(this.hostSelector + " > tbody").append(item);
        } else {
            item.insertAfter(this.hostSelector + " > tbody > tr:last");
        }
    }

});
Hippo.PageComposer.UI.Factory.register('HST.Table', Hippo.PageComposer.UI.Container.Table);

Hippo.PageComposer.UI.Container.UnorderedList = Hippo.PageComposer.UI.Container.Base.extend({

    createItemElement : function(element) {
        return $('<li class="' + this.itemCls + '"></li>').append(element);
    },

    appendItem : function(item) {
        if ($(this.hostSelector + " > li").size() == 0) {
            $(this.hostSelector).append(item);
        } else {
            item.insertAfter(this.hostSelector + " > li:last");
        }
    }
});
Hippo.PageComposer.UI.Factory.register('HST.UnorderedList', Hippo.PageComposer.UI.Container.UnorderedList);

Hippo.PageComposer.UI.Container.OrderedList = Hippo.PageComposer.UI.Container.Base.extend({

    createItemElement : function(element) {
        return $('<li class="' + this.itemCls + '"></li>').append(element);
    },

    appendItem : function(item) {
        if ($(this.hostSelector + " > li").size() == 0) {
            $(this.hostSelector).append(item);
        } else {
            item.insertAfter(this.hostSelector + " > li:last");
        }
    }
});
Hippo.PageComposer.UI.Factory.register('HST.OrderedList', Hippo.PageComposer.UI.Container.OrderedList);

Hippo.PageComposer.UI.ContainerItem.Base = Hippo.PageComposer.UI.Widget.extend({
    init : function(id, element) {
        this._super(id, element);

        this.selCls = this.selCls + '-containerItem';
        this.actCls = this.actCls + '-containerItem';
        this.overlayCustomCls = 'hst-overlay-containeritem';

        var tmp = $(element).attr("hst:temporary");
        this.isTemporary = typeof tmp !== 'undefined';
        if(this.isTemporary) {
            $(element).html('Click to refresh');
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

    select : function() {
        this._super();
        $(this.getOverlay()).addClass(this.selCls);
    },

    deselect : function() {
        this._super();
        $(this.getOverlay()).removeClass(this.selCls);
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

    onDestroy : function() {
        this.getOverlay().remove();
    }

});
Hippo.PageComposer.UI.Factory.register('HST.Item', Hippo.PageComposer.UI.ContainerItem.Base);

