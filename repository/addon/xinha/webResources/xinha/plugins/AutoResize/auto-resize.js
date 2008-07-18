
function doFindDisplayTable(el) {
    while (el != null && el != document.body) {
        if (YAHOO.util.Dom.hasClass(el, 'displaytable')) {
            return true;
        }
        el = el.parentNode;
    }
    return false;
}

function AutoResize(editor) {
    this.editor = editor;
    this.templateStyle = false;
    
    var isTemplate = doFindDisplayTable(this.editor._textArea);
    
    YAHOO.hippo.LayoutManager.registerResizeListener(this.editor._textArea, this.editor, function(type, args, me) {
        var w= args[0].body.w ? parseInt(args[0].body.w) : null;
        var h= args[0].body.h ? parseInt(args[0].body.h) : null;
        if(YAHOO.lang.isNumber(w)) {
            if(isTemplate) {
                w -= (parseInt(w*0.1) + 43);
            }
            w -= 20;
        } 
        if(YAHOO.lang.isNumber(h)) {
            h =parseInt(h/3);
        }
        doAutoResize(me, w, h);
    });
    
}

AutoResize._pluginInfo = {
  name          : "AutoResize",
  version       : "1.0",
  developer     : "Arthur Bogaart",
  developer_url : "http://www.onehippo.org",
  c_owner       : "Arthur Bogaart",
  sponsor       : "",
  sponsor_url   : "",
  license       : ""
}

function doAutoResize(editor, w, h) {
    if (editor._iframe == null) {
        var again  = function() {
            doAutoResize(editor, w, h);
        }
        window.setTimeout(again, 100);
    } else {
        editor.sizeEditor(w + 'px', h + 'px', true, true);
    }
};