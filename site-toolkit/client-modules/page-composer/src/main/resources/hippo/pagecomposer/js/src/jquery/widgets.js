$.namespace('Hippo.PageComposer.UI', 'Hippo.PageComposer.UI.Container', 'Hippo.PageComposer.UI.ContainerItem');

Hippo.PageComposer.UI.Widget = Class.extend({
    init : function(id, element) {
        this.id = id;
        this.element = element;

        this.selector = '#' + element.id;

        this.selCls = 'hst-selected';
        this.actCls = 'hst-activated';

        var self = this;
        $(element).hover(
            function() {
                self.onMouseOver(this);
            },
            function() {
                self.onMouseOut(this);
            }
        );

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

    render : function() {
        if (this.rendered) {
            console.warn('Component has already been rendered, abort.');
            return;
        }
        this.onRender();
        this.rendered = true;
    },

    destroy : function() {
        this.onDestroy();
        this.rendered = false;
    },

    onMouseOver : function(element) {
    },

    onMouseOut  : function(element) {
    },

    onRender  : function() {
    },

    onDestroy : function() {
    },

    sync: function() {
    }
});

Hippo.PageComposer.UI.Container.Base = Hippo.PageComposer.UI.Widget.extend({
    init : function(id, element) {
        this._super(id, element);

        this._items = {};
        this._numOfItems = 0;

        this.itemSelector = this.selector + ' div.componentContentWrapper';

        this.itemCls = 'hst-dd-item';
        this.hostCls = 'hst-container';

        this.hostSelector = this.selector + ' .' + this.hostCls;
        this.dragSelector = '.' + this.itemCls; //This selector should be relative to the DDHost

        this.selCls = this.selCls + '-container';
        this.actCls = this.actCls + '-container';

        this.emptyCls = 'hst-empty-container';

        //is this needed?
        this.emptyItemCls = 'hst-empty-container-item';

        //workaround: set to opposite to evoke this.sync() to render an initially correct UI
        this.isEmpty = $(this.itemSelector).size() > 0;

        var self = this;
        $(this.itemSelector).each(function() {
            self.createItem(this);
        });
    },

    onRender : function() {
        this._super();
        var self = this;

        //instantiate jquery.UI sortable
        $(this.hostSelector).sortable({
            items: this.dragSelector,
            connectWith: '.' + this.hostCls,
            update: $.proxy(this.onUpdate, this),
            helper: $.proxy(this.onHelper, this),
            revert: 100,
            receive: $.proxy(this.onReceive, this),
            remove: $.proxy(this.onRemove, this),
            placeholder : {
                element: function(el) {
                    var w = $(el).width(), h = $(el).height();
                    return $('<li class="ui-state-highlight placeholdert"></li>').height(h).width(w);
                },
                update: function(contEainer, p) {
                    //TODO
                }

            },
            start: function() {
                $('div.hst-menu-overlay').hide();
            },
            stop: function() {
                $('div.hst-menu-overlay').show();
            }

        }).disableSelection();

        this.sync();
    },

    onDestroy: function() {
        //destroy jquery.UI sortable
        $(this.hostSelector).sortable('destroy');
    },

    add : function(element) {
        this.destroy();
        var item = this.createItem(element);
        var wrapped = this.createItemElement(element);
        this.appendItem(wrapped);
        this.render();
        this.syncAll();
    },

    eachItem : function(f) {
        $.each(this._items, f);
    },

    remove: function() {
        console.log('So this is used!');
        console.trace();
        $(this.element).remove();
    },

    createItem : function(element) {
        var item = Hippo.PageComposer.UI.Factory.create(element);
        this._items[item.id] = item;
        this._numOfItems++;
        item.render();
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
            this._numOfItems--;
            this.syncAll();
            return true;
        } catch(e) {
        }
        return false;
    },

    syncAll : function() {
    },

    sync : function() {
        console.log('syncing container');
        this.checkEmpty();
        this.syncOverlays(true);
    },

    syncOverlays : function(quite) {
        var self = this;
        $.each(this._items, function(index, item) {
            if (typeof item !== 'undefined') {
                item.update();
            } else if(!quite && Hippo.PageComposer.Main.isDebug()) {
                console.warn('ContainerItem with id=' + id + ' is not found in active map.');
            }
        });
    },

    checkEmpty : function() {
        //if container is empty, make sure it still has a size so items form a different container can be dropped
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
        var children = {};
        $(this.itemSelector).each(function() {
            order.push(this.id);
            children[this.id] = Hippo.PageComposer.UI.Factory.getById(this.id);
        });
        this._items = children;
        this._numOfItems = order.length;
        sendMessage({id: this.id, children: order}, 'rearrange');
        this.syncAll();
    },

    onHelper : function(event, element) {
        var clone = element.find('.componentContentWrapper').clone();
        return $('<div class="hst-dd-helper"></div>').append(clone);//.width($(clone).width()).height($(clone).height());
    },

    onReceive : function(event, ui) {
        console.log('onreceive')
        var el = ui.item.find('.componentContentWrapper');
        ui.item.replaceWith(this.createItemElement(el));
        sendMessage({id: this.id, element: el[0]}, 'receiveditem');
        //this.syncAll();
    },

    onRemove : function(event, ui) {
        this.syncAll();
    }
});

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
Hippo.PageComposer.UI.Factory.register('Hippo.DD.Container.Table', Hippo.PageComposer.UI.Container.Table);

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
Hippo.PageComposer.UI.Factory.register('Hippo.DD.Container.UnorderedList', Hippo.PageComposer.UI.Container.UnorderedList);

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
Hippo.PageComposer.UI.Factory.register('Hippo.DD.Container.OrderedList', Hippo.PageComposer.UI.Container.OrderedList);

Hippo.PageComposer.UI.ContainerItem.Base = Hippo.PageComposer.UI.Widget.extend({
    init : function(id, element) {
        this._super(id, element);

        this.selCls = this.selCls + '-containerItem';
        this.actCls = this.actCls + '-containerItem';

        var self = this;
        this.menuOverlay = $('<div/>').addClass('hst-menu-overlay').appendTo(document.body);
        this.menuOverlay.hover(function() {
            self.onMouseOverMenuOverlay(this);
        }, function() {
            self.onMouseOutMenuOverlay(this);
        });
        this.menuOverlay.click(function() {
            sendMessage({element: element}, 'onclick');
        });
        $(element).click(function() {
            self.onClick();
        });

        var deleteButton = $('<div/>').addClass('hst-menu-overlay-button').html('X');
        deleteButton.click(function(e) {
            e.stopPropagation();
            sendMessage({element: element}, 'remove');
        });

        this.menuOverlay.append(deleteButton);

        this.sync();
    },

    sync : function() {
        this.syncOverlays();
    },

    //Webkit does not work with the 'new' position function
    //Webkit also doesn't work with setting position through .offset so we are back to css.left/top
    syncOverlays : function() {
        var el = $(this.element);

        var elOffset = $(el).offset();
        var left = (elOffset.left + $(el).outerWidth()) - this.menuOverlay.width();
        var top = elOffset.top;

        this.menuOverlay.css('left', left);
        this.menuOverlay.css('top', top);

//            this.menuOverlay.position({
//                my: 'right top',
//                at: 'right top',
//                of: el
//            });
    },

    select : function() {
        this._super();
        $(this.menuOverlay).addClass(this.selCls);
    },

    deselect : function() {
        this._super();
        $(this.menuOverlay).removeClass(this.selCls);
    },

    update : function() {
        this.syncOverlays();
    },

    onMouseOver : function(element) {
        this.menuOverlay.addClass('hst-menu-overlay-hover');
    },

    onMouseOut : function(element) {
        this.menuOverlay.removeClass('hst-menu-overlay-hover');
    },

    onMouseOverMenuOverlay : function(element) {
        this.menuOverlay.addClass('hst-menu-overlay-hover');
    },

    onMouseOutMenuOverlay : function(element) {
        this.menuOverlay.removeClass('hst-menu-overlay-hover');
    },

    onDestroy : function() {
        this.menuOverlay.remove();
    },

    onClick : function() {
        sendMessage({element: this.element}, 'onclick');
    }

});
Hippo.PageComposer.UI.Factory.register('Hippo.PageComposer.UI.ContainerItem.Base', Hippo.PageComposer.UI.ContainerItem.Base);
