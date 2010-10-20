$.namespace('Hippo.DD', 'Hippo.DD.Container', 'Hippo.DD.ContainerItem');

(function() {
    Hippo.DD.FactoryImpl = function() {
        this.registry = {};
        this.active = {};
    };

    Hippo.DD.FactoryImpl.prototype = {
        create : function(element) {
            var die = function(msg) {
                console.error(msg);
                throw new Error(msg);
            };

            if (typeof element === 'undefined' || element === null) {
                die("element is undefined or null");
            }

            var el = $(element);
            var id = el.attr('hst:id');
            if (typeof id === 'undefined') {
                die('Attribute hst:id not found');
            }

            if (!element.id) {
                console.warn('No @id found on element, using value of hst:id instead.');
                element.id = id;
            }

            var type = el.attr('hst:type');
            if (typeof type === 'undefined') {
                die('Attribute hst:type not found');
            }

            if (type === HST.CONTAINER) {
                var cType = el.attr('hst:containertype');
                if (typeof cType === 'undefined') {
                    die('Attribute hst:containertype not found');
                }
                if (typeof this.registry[cType] === 'undefined') {
                    die('No container implementation found for containerType=' + cType);
                }

                var c = new this.registry[cType](id, element);
                if (c instanceof Hippo.DD.Container.Base) {
                    return c;
                } else {
                    die('Container instance of ' + cType + ' should be a subclass of Hippo.DD.Container.Base');
                }
            } else if (type === HST.CONTAINERITEM) {
                //for now use a generic component for containerItems
                return new Hippo.DD.ContainerItem.Generic(id, element);
            }
            return null;
            //die('Could not instantiate new object of type ' + type + ' for element ' + element);
        },

        createOrRetrieve : function(element) {
            var die = function(msg) {
                console.error(msg);
                throw new Error(msg);
            };

            if (typeof element === 'undefined' || element === null) {
                die("element is undefined or null");
            }

            var el = $(element);
            var id = el.attr('hst:id');
            if (typeof id === 'undefined') {
                die('Attribute hst:id not found');
            }

            if(typeof this.active[id] === 'undefined') {
                this.create(element);
            }
            return this.active[id];
        },

        register : function(key, value) {
            this.registry[key] = value;
        },

        sync : function() {
            $.each(this.active, function(key, value) {
                if(value instanceof Hippo.DD.Container.Base) {
                    value.sync();
                }
            });
        }
    };

    Hippo.DD.Factory = new Hippo.DD.FactoryImpl();

    Hippo.DD.Base = Class.extend({
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

            //TODO: remove again and solve by registering with container
            Hippo.DD.Factory.active[this.id] = this;
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

        render      : function() {
            if(this.rendered) {
                console.warn('Component has already been rendered, abort.');
                return;
            }
            this.onRender();
            this.rendered = true;
        },

        destroy     : function() {
            this.onDestroy();
            this.rendered = false;
        },

        onMouseOver : function(element) {
        },

        onMouseOut  : function(element) {
        },

        onRender  : function() {},

        onDestroy : function() {},

        sync: function() {}

    });

    Hippo.DD.ContainerItem.Generic = Hippo.DD.Base.extend({
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
        }
    });

    Hippo.DD.Container.Base = Hippo.DD.Base.extend({
        init : function(id, element) {
            this._super(id, element);

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
        },

        onRender : function() {
            this._super();
            var self = this;

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

            this.eachItem(function() {
                $(this).click(function() {
                   sendMessage({element: this}, 'onclick'); 
                });
            });
            
        },

        onDestroy: function() {
            $(this.hostSelector).sortable('destroy');
        },

        add : function(element) {
            this.destroy();
            var xx = Hippo.DD.Factory.create(element);
            this.addItem(element);
            this.render();
            Hippo.DD.Factory.sync();
        },

        eachItem : function(f) {
            $(this.itemSelector).each(f);
        },

        remove: function() {
            $(this.element).remove();
        },

        /**
         * Template method for wrapping a containerItem element in a new item element
         */
        createItem : function(element) {
        },

        /**
         * Template method for adding an item to the container
         */
        addItem : function(data) {
        },

        removeItem : function(element) {
            if(Hippo.DD.Factory.active[element.id]) {
                Hippo.DD.Factory.active[element.id].destroy();
            }
            //remove item wrapper elements
            $(element).parents('.' + this.itemCls).remove();
            Hippo.DD.Factory.sync();
        },

        sync : function() {
            this.checkEmpty();
            this.syncOverlays(true);
        },

        syncOverlays : function(quite) {
            this.eachItem(function() {
                var id = this.getAttribute('hst:id');
                if (typeof Hippo.DD.Factory.active[id] !== 'undefined') {
                    Hippo.DD.Factory.active[id].update();
                } else {
                    if(!quite) {
                        console.warn('ContainerItem with id=' + id + ' is not found in active map.');
                    }
                }
            });
        },

        checkEmpty : function() {
            //if container is empty, make sure it still has a size so items form a different container can be dropped
            if ($(this.itemSelector).size() == 0) {
                if(!this.isEmpty) {
                    this.isEmpty = true;

                    $(this.element).addClass(this.emptyCls);
                    var tmpCls = this.itemCls;
                    this.itemCls = this.emptyItemCls;
                    this.addItem($('<div class="empty-container-placeholder">Empty container</div>')[0]);
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

        //called after an update in the order of a container has been invoked
        onUpdate : function(event, ui) {
            var order = [];
            this.eachItem(function() {
                var id = this.getAttribute('hst:id');
                order.push(id);
            });
            sendMessage({id: this.id, children: order}, 'rearrange');
            Hippo.DD.Factory.sync();
        },

        onHelper : function(event, element) {
            var clone = element.find('.componentContentWrapper').clone();
            return $('<div class="hst-dd-helper"></div>').append(clone);//.width($(clone).width()).height($(clone).height());
        },

        onReceive : function(event, ui) {
            var el = ui.item.find('.componentContentWrapper');
            ui.item.replaceWith(this.createItem(el));
            sendMessage({id: this.id, element: el[0]}, 'receiveditem');
            Hippo.DD.Factory.sync();
        },

        onRemove : function(event, ui) {
            Hippo.DD.Factory.sync();
        }
    });

    //Container implementations
    Hippo.DD.Container.Table = Hippo.DD.Container.Base.extend({

        createItem : function(element) {
            var td = $('<td></td>').append(element);
            return $('<tr class="' + this.itemCls + '"></tr>').append(td);
        },

        addItem : function(element) {
            var item = this.createItem(element);
            if ($(this.hostSelector + " > tbody > tr").size() == 0) {
                $(this.hostSelector + " > tbody").append(item);
            } else {
                item.insertAfter(this.hostSelector + " > tbody > tr:last");
            }
            Hippo.DD.Main.registerEvents(element);
        }

    });
    Hippo.DD.Factory.register('Hippo.DD.Container.Table', Hippo.DD.Container.Table);

    Hippo.DD.Container.UnorderedList = Hippo.DD.Container.Base.extend({

        createItem : function(element) {
            return $('<li class="' + this.itemCls + '"></li>').append(element);
        },

        addItem : function(element) {
            var item = this.createItem(element);

            if ($(this.hostSelector + " > li").size() == 0) {
                $(this.hostSelector).append(item);
            } else {
                item.insertAfter(this.hostSelector + " > li:last");
            }
            Hippo.DD.Main.registerEvents(element);
        }
    });
    Hippo.DD.Factory.register('Hippo.DD.Container.UnorderedList', Hippo.DD.Container.UnorderedList);

    Hippo.DD.Container.OrderedList = Hippo.DD.Container.Base.extend({

        createItem : function(element) {
            return $('<li class="' + this.itemCls + '"></li>').append(element);
        },

        addItem : function(element) {
            var item = this.createItem(element);

            if ($(this.hostSelector + " > li").size() == 0) {
                $(this.hostSelector).append(item);
            } else {
                item.insertAfter(this.hostSelector + " > li:last");
            }
            Hippo.DD.Main.registerEvents(element);
        }
    });
    Hippo.DD.Factory.register('Hippo.DD.Container.OrderedList', Hippo.DD.Container.OrderedList);

})();
