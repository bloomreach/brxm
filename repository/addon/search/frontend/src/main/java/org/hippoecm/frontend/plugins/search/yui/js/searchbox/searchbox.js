

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
            var data = aArgs[2];
            var url = _this.callbackUrl + '&browse=' + encodeURIComponent(data.url);
            _this.callbackMethod(url);
            this._elTextbox.value = '';
        });
        
        this.suppressInputUpdate = true;
        this.resultTypeList = false; 
    },
    
    formatResult : function(oResultData, sQuery, sResultMatch) {
        return "<span>" + oResultData.label + " (" + oResultData.state + ")<br/>" +  oResultData.excerpt + "</span>";
    },
    
    doBeforeLoadData : function(sQuery, oResponse, oPayload) {
        this.setFooter(oResponse.meta.totalHits + " hits");
        return true;
    }

});
