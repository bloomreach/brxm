$.namespace('Hippo.PageComposer');

( function() {

    var Main = function() {
        this.debug = false;
    };

    Main.prototype = {

        init: function(debug) {
            this.debug = debug;

            this.manager = new Hippo.PageComposer.UI.Manager();
        },

        add: function(element, parentId) {
            this.manager.add(element, parentId);
        },

        remove : function(element) {
            this.manager.remove(element);
        },

        select: function(element) {
            this.manager.select(element);
        },

        deselect : function(element) {
            this.manager.deselect(element);
        },

        isDebug: function() {
            return this.debug;
        },

        die: function(msg) {
            if(Hippo.PageComposer.Main.isDebug()) {//global reference for scope simplicity
                console.error(msg);
            } else {
                throw new Error(msg);
            }
        }
    };

    Hippo.PageComposer.Main = new Main();

})();