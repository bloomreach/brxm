$.namespace('Hippo.DD');

( function() {

    var DDImpl = function() {
        this.current = null;
        this.active = {}
    };

    DDImpl.prototype = {

        init: function() {
            $('<div id="switcher"></div>').appendTo($(document.body)).themeswitcher({
                sloadTheme: ''
            });

            //do try/catch because else errors will disappear
            try {
                //attach mouseover/mouseclick for components
                var me = this;
                $('div.componentContentWrapper').each(function(index) {
                    var id = this.getAttribute('hst:id');
                    var type = this.getAttribute('hst:type');
                    if (type == 'container') {
                        me.active[id] = Hippo.DD.Factory.create(this);
                        me.active[id].render();
                        me.active[id].activate();
                    }
                });

                //register to listen to iframe-messages
                onhostmessage(function(msg) {
                    this.select(msg.data.element);
                    return false;
                }, this, false, 'select');

                onhostmessage(function(msg) {
                    this.deselect(msg.data.element);
                    return false;
                }, this, false, 'deselect');

                onhostmessage(function(msg) {
                    this.add(msg.data.element, msg.data.parentId);
                    return false;
                }, this, false, 'add');

                onhostmessage(function(msg) {
                    this.remove(msg.data.element);
                    return false;
                }, this, false, 'remove');

            } catch(e) {
                console.error(e);
            }
        },

        registerEvents: function(el) {
//            var type = el.getAttribute('hst:type');
//            if (type == 'container' || type == 'containerItem') {
//                var hoverClass = 'hst-hover-' + type;
//                $(el).click(function(e) {
//                    e.stopPropagation();
//                    sendMessage({element: this}, 'onclick');
//                }).hover(function() {
//                    $(this).addClass(hoverClass);
//                }, function() {
//                    $(this).removeClass(hoverClass);
//                });
//            }
        },

        select: function(element) {
            if(this.current != null && this.current.element == element) {
                return;
            }

            this.current = Hippo.DD.Factory.create(element);
            this.current.select();
        },

        deselect : function(element) {
            if (this.current != null) {
                this.current.deselect();
                this.current = null;
            }
        },

        //TODO: delegate this to the affected Container instance in this.active
        remove : function(element) {
            if(!element.hasAttribute('hst:id')) {
                element = $(element).parents('.componentContentWrapper')[0];
            }

            var id = $(element).attr('hst:id');
            var type = $(element).attr('hst:type');

            if (type == 'containerItem') {
                id = $(element).parents('.componentContentWrapper').attr('hst:id');
                if(typeof this.active[id] !== 'undefined') {
                    this.active[id].removeItem(element);
                }
            } else if(type == 'container') {
                if (typeof this.active[id] !== 'undefined') {
                    this.active[id].remove();
                    delete this.active[id];
                }
            }
        },

        add: function(element, parentId) {
            if(typeof this.active[parentId] !== 'undefined') {
                this.active[parentId].add(element);
            }
        }
    };

    Hippo.DD.Main = new DDImpl();

})();