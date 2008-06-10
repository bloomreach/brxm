//Create new loading indicator
var indicator = new AjaxLoadIndicator('${id}');

//register with wicket pre call
var wicketGlobalPreCallHandler = function() {
    indicator.show();
}

//register with wicket post call
var wicketGlobalPostCallHandler = function() {
    indicator.hide();
}