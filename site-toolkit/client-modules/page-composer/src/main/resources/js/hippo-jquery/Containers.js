$.namespace('Hippo.DD', 'Hippo.DD.Container', 'Hippo.DD.ContainerItem');

(function() {
    Hippo.DD.FactoryImpl = function() {
        this.registry = {};
    };

    Hippo.DD.FactoryImpl.prototype = {
        create : function(element) {
            if(typeof element === 'undefined' || element === null) {
                console.error("element is undefined or null");
                throw new Error("element is undefined or null");
            }
            if(!element.hasAttribute('hst:id')) {
                console.error('Attribute hst:id not found on element[@id=' + element.id + ']');
                throw new Error('Attribute hst:id not found on element[@id=' + element.id + ']');
            }
            if(!element.hasAttribute('hst:type')) {
                console.error('Attribute hst:type not found on element[@id=' + element.id + ']');
                throw new Error('Attribute hst:type not found on element[@id=' + element.id + ']');
            }
            var id = element.getAttribute('hst:id');
            var type = element.getAttribute('hst:type');

            if(type === 'container') {
                if (!element.hasAttribute('hst:containertype')) {
                    console.error('Attribute hst:containertype not found on element[@id=' + element.id + ']');
                    throw new Error('Attribute hst:containertype not found on element[@id=' + element.id + ']');
                }
                var cType = element.getAttribute('hst:containertype');
                if (typeof this.registry[cType] === 'undefined') {
                    console.error('No container implementation found for containerType=' + cType);
                    throw new Error('No container implementation found for containerType=' + cType);
                }
                var c = new this.registry[cType](id, element);
                if (c instanceof Hippo.DD.Container.Base) {
                    return c;
                } else {
                    console.error('Container instance should be a subclass of Hippo.DD.Container.Base');
                    throw new Error('Container instance should be a subclass of Hippo.DD.Container.Base');
                }
            } else if(type === 'containerItem') {
                //for now use a generic component for containerItems
                return new Hippo.DD.ContainerItem.Generic(id, element);
            }
        },

        register : function(key, value) {
            this.registry[key] = value;
        }
    };

    Hippo.DD.Factory = new Hippo.DD.FactoryImpl();

    Hippo.DD.Base = Class.extend({
        init : function(id, element) {
            this.id = id;
            this.element = element;
            if (!element.id) {
                console.warn('No @id found on element, using value of hst:id instead.');
                element.id = this.id;
                //throw new Error('Container element should contain id attribute');
            }

            //points to the componentWrapperElement
            this.containerSelector = '#' + element.id;

            this.selectedClassName = 'hst-selected';
            this.activatedClassName = 'hst-activated';
        },

        select : function() {
            $(this.element).parent().addClass('ui-selected');
            $(this.element).addClass(this.selectedClassName);
            //this.render();
        },

        deselect : function() {
            $(this.element).parent().removeClass('ui-selected');
            $(this.element).removeClass(this.selectedClassName);
            //this.destroy();
        },

        render : function(){
        },

        destroy : function() {
        },

        activate : function() {
            $(this.element).addClass(this.activatedClassName);
        },

        deactivate : function() {
            $(this.element).addClass(this.activatedClassName);
        }

    });

    Hippo.DD.ContainerItem.Generic = Hippo.DD.Base.extend({
        init : function(id, element) {
            this._super(id, element);

            this.activatedClassName = this.activatedClassName  + '-containerItem';
            this.selectedClassName = this.selectedClassName  + '-containerItem';
        }
    });

    Hippo.DD.Container.Base = Hippo.DD.Base.extend({
        init : function(id, element) {
            this._super(id, element);

            this.itemSelector = this.containerSelector + ' div.componentContentWrapper';

            this.ddItemClassname = 'hst-dd-item';
            this.ddHostClassname = 'hst-container';
            this.ddHostSelector= this.containerSelector + ' .' + this.ddHostClassname;
            this.ddItemSelector = '.' + this.ddItemClassname; //This selector should be relative to the DDHost

            this.activatedClassName = this.activatedClassName + '-container';
            this.selectedClassName = this.selectedClassName + '-container';

            this.emptyContainerClassname = 'hst-empty-container';
            this.emptyItemClassname = 'hst-empty-container-item';
        },

        render : function() {
            this._super();
            var self = this;
            $(this.ddHostSelector).sortable({
                items: this.ddItemSelector,
                connectWith: '.' + this.ddHostClassname,
                update: $.proxy(this.onUpdate, this),
                helper: $.proxy(this.onHelper, this),
                receive: $.proxy(this.onReceive, this),
                remove: $.proxy(this.onRemove, this),
                opacity: 0.85,
                placeholder : {
                    element: function(el) {
                        var w = $(el).width(), h = $(el).height();
                        return $('<li class="ui-state-highlight placeholdert"></li>').height(h).width(w);
                    },
                    update: function(contEainer, p) {
                        //TODO
                    }

                }
            }).disableSelection();
            this.checkEmpty();
            $('.hst-container').addClass('ui-selectable');
        },

        destroy: function() {
            try {
                $(this.ddHostSelector).sortable('destroy');
            } catch(e) {
                console.error(e);
            }
        },

        add : function(element) {
            this.destroy();
            this.addItem(element);
            this.render();
        },

        dropFromParent : function(data) {
            this.destroy();
            for (var i = 0; i < data.length; ++i) {
                this.addItem(data[i]);
            }
            this.render();
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
            //remove item wrapper elements
            $(element).parents('.hst-dd-item').remove();
            this.checkEmpty();
        },

        checkEmpty : function() {
            //if container is empty, make sure it still has a size so items form a different container can be dropped
            if($(this.itemSelector).size() == 0) {
                $(this.element).addClass(this.emptyContainerClassname);
                var preClassName = this.ddItemClassname;
                this.ddItemClassname = this.emptyItemClassname;
                this.addItem($('<div class="empty-container-placeholder">Empty container</div>')[0]);
                this.ddItemClassname = preClassName;
            } else {
                $(this.element).removeClass(this.emptyContainerClassname);
                $(this.ddHostSelector + ' .' + this.emptyItemClassname).remove();
            }

            $('.hst-dd-item').addClass('ui-state-default');
        },

        //event listeners
        onUpdate : function(event, ui) {
            this.checkEmpty();
            var order = [];
            this.eachItem(function() {
                order.push(this.getAttribute('hst:id'));
            });
            sendMessage({id: this.id, children: order}, 'rearrange');
        },

        onHelper : function(event, element) {
            var clone = element.find('.componentContentWrapper').clone();
            return $('<div class="hst-dd-helper"></div>').append(clone);//.width($(clone).width()).height($(clone).height());
        },

        onReceive : function(event, ui) {
            var el = ui.item.find('.componentContentWrapper');
            ui.item.replaceWith(this.createItem(el));
            sendMessage({id: this.id, element: el[0]}, 'receiveditem');
        },

        onRemove : function(event, ui) {
        }

    });

    Hippo.DD.Container.Table = Hippo.DD.Container.Base.extend({

        createItem : function(element) {
            var td = $('<td></td>').append(element);
            return $('<tr class="' + this.ddItemClassname + '"></tr>').append(td);
        },

        addItem : function(element) {
            var item = this.createItem(element);
            if($(this.ddHostSelector + " > tbody > tr").size() == 0) {
                $(this.ddHostSelector + " > tbody").append(item);
            } else {
                item.insertAfter(this.ddHostSelector + " > tbody > tr:last");
            }
            Hippo.DD.Main.registerEvents(item.element);
        }

    });
    Hippo.DD.Factory.register('Hippo.DD.Container.Table', Hippo.DD.Container.Table);

    Hippo.DD.Container.UnorderedList = Hippo.DD.Container.Base.extend({

        createItem : function(element) {
            return $('<li class="' + this.ddItemClassname + '"></li>').append(element);
        },

        addItem : function(element) {
            var item = this.createItem(element);

            if($(this.ddHostSelector + " > li").size() == 0) {
                $(this.ddHostSelector).append(item);
            } else {
                item.insertAfter(this.ddHostSelector + " > li:last");
            }
            Hippo.DD.Main.registerEvents(item.element);
        }
    });
    Hippo.DD.Factory.register('Hippo.DD.Container.UnorderedList', Hippo.DD.Container.UnorderedList);

    Hippo.DD.Container.OrderedList = Hippo.DD.Container.Base.extend({

        createItem : function(element) {
            return $('<li class="' + this.ddItemClassname + '"></li>').append(element);
        },

        addItem : function(element) {
            var item = this.createItem(element);

            if($(this.ddHostSelector + " > li").size() == 0) {
                $(this.ddHostSelector).append(item);
            } else {
                item.insertAfter(this.ddHostSelector + " > li:last");
            }
            Hippo.DD.Main.registerEvents(item.element);
        }
    });
    Hippo.DD.Factory.register('Hippo.DD.Container.OrderedList', Hippo.DD.Container.OrderedList);

})();
