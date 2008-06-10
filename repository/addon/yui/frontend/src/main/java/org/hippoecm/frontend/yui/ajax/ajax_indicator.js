function AjaxLoadIndicator(elId) {
    this.elId = elId;
    this.calls = 0;
}

AjaxLoadIndicator.prototype.getElement = function() {
    return YAHOO.util.Dom.get(this.elId);
};

AjaxLoadIndicator.prototype.show = function() {
    this.calls++;
    YAHOO.util.Dom.setStyle(this.getElement(), 'display', 'block');
}

AjaxLoadIndicator.prototype.hide = function() {
    if(this.calls > 0) {
        this.calls--;
    } 
    if (this.calls == 0) {
        YAHOO.util.Dom.setStyle(this.getElement(),'display', 'none');
    }
};