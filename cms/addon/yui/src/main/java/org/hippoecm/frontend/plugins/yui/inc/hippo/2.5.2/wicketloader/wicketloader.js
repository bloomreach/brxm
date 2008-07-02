/**
 * @description <p>Class that queues an array of init functions that get executed upon page load or in Wicket's post ajax phase</p>
 * @namespace YAHOO.hippo
 * @requires yahoo
 * @module wicketloader
 */

YAHOO.namespace('hippo');

YAHOO.hippo.WicketLoader = function() {
};

YAHOO.hippo.WicketLoader.prototype = {
    registered: false,
    initialized: false,
    queue: new Array(),
    
    initialize: function() {
        this.handleQueue();
        this.initialized = true;
    },

    handleQueue: function() {
        while(this.queue.length > 0) {
            this.queue.shift().apply();
        }  
        this.registered = false;
    },
    
    registerFunction: function(func) {
        this.queue.push(func);
        var me = this;
        if(!this.registered) {
            this.registered = true;
            if (!this.initialized) {
                Wicket.Event.addDomReadyEvent(function(){me.initialize()});
            } else {
                Wicket.Ajax.registerPostCallHandler(function(){me.handleQueue()});
            }
        }
    }
};

YAHOO.register("wicketloader", YAHOO.hippo.WicketLoader, {version: "2.5.2", build: "1076"});

