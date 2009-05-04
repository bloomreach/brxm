
//FIXME: This code assumes YUI's core classes are already loaded..
function fixIE6Hover(className, tag, hoverClassName) {
    var YUID = YAHOO.util.Dom;
    var items = YUID.getElementsByClassName(className, tag);
    
    for(var i=0; i<items.length; i++) {
        var el = items[i];
        el.onmouseover = function() {
            YUID.addClass(this, hoverClassName);
        } 
        el.onmouseout = function() {
            YUID.removeClass(this, hoverClassName);
        } 
    }
}