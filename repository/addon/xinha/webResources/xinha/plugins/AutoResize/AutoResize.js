AutoResize._pluginInfo = {
    name :"AutoResize",
    version :"1.0",
    developer :"Arthur Bogaart",
    developer_url :"http://www.onehippo.org",
    c_owner :"Arthur Bogaart",
    sponsor :"",
    sponsor_url :"",
    license :""
}

Xinha.Config.prototype.AutoResize =
{
    'minHeight' : 150,
    'minWidth'  : 150
}

function AutoResize(editor) {
    this.editor = editor;

    this.initialized = false;
    this.timeout = null;

    this.DOM = YAHOO.util.Dom;
    
    this.parentElement = this.findParent();
    var xl = parseInt(this.DOM.getStyle(this.parentElement, 'padding-left'));
    var xr = parseInt(this.DOM.getStyle(this.parentElement, 'padding-right'));
    this.parentPaddingX = xl+xr;
    
    this.dim = {
            w: -1,
            h: -1,
            viewWidth: 0,
            viewHeight: this.DOM.getViewportHeight()
    }
    var me = this;
    YAHOO.hippo.LayoutManager.registerResizeListener(this.editor._textArea,
            this, function(type, args) {
        me.checkResize(editor);
    }, true);
}

AutoResize.prototype.getDim = function(vWidth, vHeight) {
    var x,y;

    var minHeight = this.editor.config.AutoResize.minHeight;
    var minWidth = this.editor.config.AutoResize.minWidth;
    
    var parentRegion = this.DOM.getRegion(this.parentElement);
    x = parentRegion.width - this.parentPaddingX;
    
    if(x < minWidth) {
        x = minWidth;
    }
    
    var p = vHeight / minHeight;
    var yy = 0;
    if(p >= 2.2) {
        yy = (minHeight/20)*p;
    }

    y = minHeight;
    if(this.dim.viewHeight - vHeight > 0) {
        if(y - yy > minHeight) {
            y -= yy;
        }
    } else {
        y += yy;
    }
    
    return {
        w: x, 
        h: Math.round(y),
        viewHeight: vHeight,
        viewWidth: vWidth
    };
}

AutoResize.prototype.checkResize = function(editor) {
    if(this.timeout != null) {
        window.clearTimeout(this.timeout);
        this.timeout = null;
    }
    this.doResize(editor);
}

AutoResize.prototype.doResize = function(editor) {
    if(editor._iframe == null) {
        var me = this;
        var again = function() {
            me.doResize(editor);
        }
        this.timeout = window.setTimeout(again, 250);
    } else {
        this.timeout = null;
        var vWidth = this.DOM.getViewportWidth();
        var vHeight = this.DOM.getViewportHeight();
        var newDim = this.getDim(vWidth, vHeight);

        if(this.dim.w != newDim.w || this.dim.h != newDim.h) {
            this.dim = newDim;
            
            editor.sizeEditor(newDim.w + 'px', newDim.h + 'px', true, true);
            if(Xinha.is_ie) {
                this.DOM.setStyle(this.findParent(), 'height', this.dim.h + 'px');
            }
        }
    }
}

AutoResize.prototype.findParent = function() {
    var el = this.editor._textArea;
    while (el != null && el != document.body) {
        if (this.DOM.hasClass(el, 'hippo-editor-field-html')) {
            return el;
        }
        el = el.parentNode;
    }
    return null;
};
