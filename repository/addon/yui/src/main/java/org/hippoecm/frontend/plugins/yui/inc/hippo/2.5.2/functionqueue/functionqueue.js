/**
 * @description
 * <p>
 * Class that queues an array of init functions that get executed upon page load
 * or in Wicket's post ajax phase
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo
 * @module hippoutil
 */
( function() {
    var Lang = YAHOO.lang;

    /**
     * @description
     * <p>
     * Class that queues an array of init functions that get executed upon page
     * load or in Wicket's post ajax phase
     * </p>
     * @namespace YAHOO.hippo
     * @requires yahoo
     * @module hippoutil
     */

    YAHOO.namespace('hippo');

    YAHOO.hippo.FunctionQueue = function(_id) {
        this.id = _id;
        this.queue = new Array();
        this.uniques = new Array();
        this.preQueueHandler = null;
        this.postQueueHandler = null;

    };

    YAHOO.hippo.FunctionQueue.prototype = {

        handleQueue : function() {
            if (Lang.isFunction(this.preQueueHandler)) {
                this.preQueueHandler.apply();
            }
            while (this.queue.length > 0) {
                this.queue.shift().apply();
            }
            this.uniques = [];

            if (Lang.isFunction(this.postQueueHandler)) {
                this.postQueueHandler.apply();
            }
        },

        registerFunction : function(func, uniqueId) {
            if (!Lang.isFunction(func))
                return;
            if (!Lang.isUndefined(uniqueId) && !Lang.isNull(uniqueId)) {
                for ( var i = 0; i < this.uniques.length; i++) {
                    if (this.uniques[i] == uniqueId)
                        return;
                }
                this.uniques.push(uniqueId);
            }
            this.queue.push(func);
        },

        toString : function() {
            return 'Function queue [' + this.id + ']';
        }
    };
})();
YAHOO.register("functionqueue", YAHOO.hippo.FunctionQueue, {
    version :"2.5.2",
    build :"1076"
});
