

/**
 * @class a YAHOO.hippo.HippoAutocomplete extension
 * @requires hippoautocomplete
 * @extends YAHOO.hippo.HippoAutoComplete
 * @constructor
 * @param {String} id the id of the linked element
 * @param {String} config
 */
YAHOO.namespace("hippo"); 

YAHOO.hippo.SearchBox = function(id, config) {
    YAHOO.hippo.SearchBox.superclass.constructor.apply(this, arguments);
};
    
YAHOO.extend(YAHOO.hippo.SearchBox, YAHOO.hippo.HippoAutoComplete, {
    initConfig : function(config) {
        YAHOO.hippo.SearchBox.superclass.initConfig.call(this, config);
        _this = this;
        this.itemSelectEvent.subscribe(function(sType, aArgs) {
            var obj = aArgs[2];
            var nodePath = obj[1];
            var url = _this.callbackUrl + '&browse=' + encodeURIComponent(nodePath);
            _this.callbackMethod(url);
        });
    }
});
