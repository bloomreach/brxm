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

    HST = {
        COMPONENT : 'hst:component',
        CONTAINER : 'hst:containercomponent',
        CONTAINERITEM : 'hst:containeritemcomponent'
    };

})();
