(function() {
    if (typeof window.console === 'undefined') {
        window.console = {
            log : function() {
            },
            dir : function() {
            },
            warn : function() {
            },
            error : function() {
            }
        };
    }

    //Copied from http://ejohn.org/blog/javascript-array-remove/
    //Adds removeByIndex to array
    Array.prototype.removeByIndex= function(from, to) {
        var rest = this.slice((to || from) + 1 || this.length);
        this.length = from < 0 ? this.length + from : from;
        return this.push.apply(this, rest);
    };


    HST = {
        COMPONENT : 'hst:component',
        CONTAINER : 'hst:containercomponent',
        CONTAINERITEM : 'hst:containeritemcomponent'
    };

})();
