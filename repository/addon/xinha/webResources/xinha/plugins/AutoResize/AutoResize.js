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

function AutoResize(editor) {
    this.editor = editor;
    this.init   = {
            initialized: false, 
            scroll: false,
            minHeight: 200,
            marginWidth: 0, 
            scrollbarWidth: this.getScrollbarWidth(), 
            isTemplateEditor: this.isTemplateEditor(this.editor._textArea),
            nrOfXinhas: this.nrOfXinhas()
    };
    this.dim    = {width: 0, height: this.init.minHeight};
    this.preDim = {width:0, height:0};

    YAHOO.hippo.LayoutManager.registerResizeListener(this.editor._textArea,
            this, function(type, args, me) {
    	var unit = args[0];
    	
        if(unit.body.w) me.dim.width  = parseInt(unit.body.w);
        if(unit.body.h) me.dim.height = parseInt(unit.body.h);
        
        if(me.init.nrOfXinhas > 1) unit.scroll = true;
        if(!me.init.initialized) {
            me.init.scroll = unit.scroll;
            me.init.marginWidth = me.dim.width - parseInt(me.editor._initial_ta_size.w.substr(0, me.editor._initial_ta_size.w.length-2));
            me.init.initialized = true;
        } 
        
        if (unit.scroll && !me.init.scroll) me.dim.width -= me.init.scrollbarWidth;
        else if(!unit.scroll && me.init.scroll) me.dim.width += me.init.scrollbarWidth;
        else {
            //Temp workaround. We can't yet correctly predict wether we'll be seeing scrollbars or not
            //so by default we pretend their visible.
            me.dim.width -= me.init.scrollbarWidth;
        }
        
        me.dim.width = me.dim.width - me.init.marginWidth - 14;
        
        if((me.init.nrOfXinhas * me.init.minHeight) > me.dim.height) {
            me.dim.height = me.init.minHeight;
        } else {
            var a = YAHOO.util.Dom.getViewportHeight()/1024;
            var rat = 1 - (a <= 1 ? a : 0.1);
            var exp = me.init.nrOfXinhas*me.init.nrOfXinhas;
            var min = 1 - exp * rat;
            var extra = min > 0 ? me.init.minHeight* min : 0;
            var h = parseInt(me.init.minHeight + extra);
            
            me.dim.height = h < me.init.minHeight ? me.init.minHeight : h; 
        }
        if(me.preDim.width != me.dim.width || me.preDim.height != me.dim.height) {
            me.doAutoResize(me.editor, me.dim.width, me.dim.height);
            me.preDim.width = me.dim.width;
            me.preDim.height = me.dim.height;
        }
    });
}

AutoResize.prototype.doAutoResize = function(editor, w, h) {
    if (editor._iframe == null) {
        var me = this;
        var again = function() {
            me.doAutoResize(editor, w, h);
        }
        window.setTimeout(again, 250);
    } else {
        editor.sizeEditor(w + 'px', h + 'px', true, true);
    }
};

AutoResize.prototype.isTemplateEditor = function(el) {
    while (el != null && el != document.body) {
        if (YAHOO.util.Dom.hasClass(el, 'templateItemTable')) {
            return true;
        }
        el = el.parentNode;
    }
    return false;
};

AutoResize.prototype.nrOfXinhas = function() {
    var nr = YAHOO.util.Dom.getElementsByClassName('hippo-editor-field-html');
    return nr.length;
}


/**
 * This function calculates the width of a scrollbar by adding an element without overflow
 * to the dom, then adding a much bigger child element, setting the overflow to auto and
 * calculating the difference from before and after. 
 * @return The width of a scrollbar
 */
AutoResize.prototype.getScrollbarWidth = function() {
    if(Xinha.is_ie) {
        var a; 
        var t = this.editor._textArea;   
        var old = t.wrap;
        t.wrap = 'off'; 
        a = t.offsetHeight;   
        t.wrap = 'soft'; 
        a -= t.offsetHeight;
        t.wrap = old;
        return a;
    } else {
        var parent = document.createElement('div');
        parent.style.position = 'absolute';
        parent.style.top = '-1000px';
        parent.style.left = '-1000px';
        parent.style.width = '100px';
        parent.style.height = '50px';
        parent.style.overflow = 'hidden';
      
        var child = document.createElement('div');
        child.style.width = '100%';
        child.style.height = '200px';
      
        parent.appendChild(child);
        document.body.appendChild(parent);
      
        var beforeScroll = child.offsetWidth;
        parent.style.overflow = 'auto';
        var scrollWidth = beforeScroll - child.offsetWidth;
      
        document.body.removeChild(document.body.lastChild);
        return scrollWidth;
    }
}