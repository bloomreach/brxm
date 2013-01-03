//FIXME: This code assumes YUI's core classes are already loaded..
function fixIE6Hover(className, tag, hoverClassName) {
    var YUID = YAHOO.util.Dom,
        items = YUID.getElementsByClassName(className, tag),
        onMouseOver, onMouseOut, i, el;

    onMouseOver = function() {
        YUID.addClass(this, hoverClassName);
    };

    onMouseOut = function() {
        YUID.removeClass(this, hoverClassName);
    };

    for (i = 0; i < items.length; i++) {
        el = items[i];
        el.onmouseover = onMouseOver;
        el.onmouseout = onMouseOut;
    }
}