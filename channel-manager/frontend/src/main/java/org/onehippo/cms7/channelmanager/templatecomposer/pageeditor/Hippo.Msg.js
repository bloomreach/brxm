
/**
 * Hippo.Msg is decorating Ext.Msg and makes alert, confirm and prompt blocking.
 * If a blocking message is waiting for user interaction, new fired messages will be queued
 * (e. g. important for asynchronous requests which could trigger hide while we are waiting for user input).
 */
Hippo.Msg = (function() {
    var msgQueue = [];

    var blockingType = [];
    blockingType['alert'] = true;
    blockingType['confirm'] = true;
    blockingType['prompt'] = true;
    blockingType['show'] = false;
    blockingType['wait'] = false;
    blockingType['hide'] = false;
    var blocked = false;
    var waitTimeout;

    var func = function(type, args) {
        if (blocked) {
            if (blockingType[type]) {
                msgQueue.push(function() {
                    func.apply(this, args);
                });
            }
            return;
        }
        if (blockingType[type]) {
            blocked = true;
            if (args.length >= 3) {
                var oldFunction = args[2];
                var scope = this;
                if (args.length >= 4) {
                    scope = args[3];
                }
                args[2] = function() {
                    oldFunction.apply(scope, arguments);
                    blocked = false;
                    if (msgQueue.length > 0) {
                        var nextMessage = msgQueue.shift();
                        nextMessage();
                    }
                }
            }
        }
        Ext.Msg[type].apply(Ext.Msg, args);
    };

    return {
        alert : function() {
            func('alert', arguments);
        },
        confirm : function() {
            func('confirm', arguments);
        },
        prompt : function() {
            func('prompt', arguments);
        },
        show : function() {
            func('show', arguments);
        },
        wait : function() {
            if (waitTimeout) {
                return;
            }
            var args = arguments;
            waitTimeout = window.setTimeout(function() {
                waitTimeout = null;
                func('wait', args);
            }, 500);
        },
        hide : function() {
            if (waitTimeout) {
                window.clearTimeout(waitTimeout);
                waitTimeout = null;
            }
            func('hide', arguments);
        }
    }
})();
