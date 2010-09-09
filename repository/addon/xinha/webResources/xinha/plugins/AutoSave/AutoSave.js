AutoSave._pluginInfo = {
  name          : "AutoSave",
  version       : "1.0",
  developer     : "Arthur Bogaart",
  developer_url : "http://www.hippo.nl",
  c_owner       : "Arthur Bogaart",
  sponsor       : "",
  sponsor_url   : "",
  license       : "htmlArea"
}

Xinha.Config.prototype.AutoSave =
{
  'timeoutLength' : 2000,
  'callbackUrl' : ''
};

function AutoSave(editor) {
    this.editor = editor;
    this.lConfig = editor.config.AutoSave;
    
    this.timeoutID = null; // timeout ID, editor is dirty when non-null
    this.saving = false;   // whether a save is in progress

    //Atach onkeyup and onchange event listeners to textarea for autosaving in htmlmode
    var txtArea = this.editor._textArea;
    var me = this;
    var fn = function(ev) {
        me.checkChanges();
    };

    if(YAHOO.util.Event) {
        YAHOO.util.Event.addListener(txtArea, 'keyup', fn);
        YAHOO.util.Event.addListener(txtArea, 'cut', fn);
        YAHOO.util.Event.addListener(txtArea, 'paste', fn);
    } else {
        if (txtArea.addEventListener) {
            txtArea.addEventListener('keyup', fn, false);
            txtArea.addEventListener('cut', fn, false);
            txtArea.addEventListener('paste', fn, false);
        } else if (txtArea.attachEvent) {
            txtArea.attachEvent('onkeyup', fn);
            txtArea.attachEvent('cut', fn);
            txtArea.attachEvent('paste', fn);
        } else {
            txtArea['onkeyup'] = fn;
            txtArea['cut'] = fn;
            txtArea['paste'] = fn;
        }
    }
}

AutoSave.prototype._lc = function(string) {
    return Xinha._lc(string, 'AutoSave');
};

AutoSave.prototype.getId = function() {
    return this.editor._textArea.getAttribute("id");
};

AutoSave.prototype.getContents = function() {
    return this.editor.getInnerHTML();
};

// Save the contents of the xinha field.  Only one throttled request is executed concurrently.
AutoSave.prototype.save = function(throttled) {
	// nothing to do if editor is not dirty
	if (this.timeoutID == null) {
		return;
	} else {
		window.clearTimeout(this.timeoutID);
	}

	if (throttled) {
		// reschedule when a save is already in progress
		if (this.saving) {
			this.checkChanges();
			return;
		}
		this.saving = true;
	}

	this.timeoutID = null;
	var xmlHttpReq = null;
    if (window.XMLHttpRequest) {    // Mozilla/Safari
        xmlHttpReq = new XMLHttpRequest();
    } else if (window.ActiveXObject) {     // IE
        xmlHttpReq = new ActiveXObject("Microsoft.XMLHTTP");
    }

    if(this.editor._editMode == 'wysiwyg') { //save Iframe html into textarea
        this.editor._textArea.value = this.editor.outwardHtml(this.editor.getHTML());
    }
    var self = this;
    var callbackUrl = this.editor.config.callbackUrl;
    xmlHttpReq.open('POST', callbackUrl, throttled);
    xmlHttpReq.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    xmlHttpReq.setRequestHeader('Wicket-Ajax', "true");
    xmlHttpReq.onreadystatechange = function() {
        if (xmlHttpReq.readyState == 4) {
            //console.log('AJAX-UPDATE: ' + self.xmlHttpReq.responseText);
        	if (throttled) {
        		self.saving = false;
        	}
        }
    }
    xmlHttpReq.send(wicketSerialize(Wicket.$(this.getId())));
};

AutoSave.prototype.onUpdateToolbar = function() {
    this.checkChanges();
};

AutoSave.prototype.onKeyPress = function(ev) {
    if( ev != null && ev.ctrlKey && this.editor.getKey(ev) == 's') {
        this.save(true);
        Xinha._stopEvent(ev);
        return true;
    }
    this.checkChanges();
};

AutoSave.prototype.checkChanges = function() {
    if(this.timeoutID != null) {
        window.clearTimeout(this.timeoutID);
    }
    var self = this;
    var editorId = this.getId(); 
    this.timeoutID = window.setTimeout(function() {
        YAHOO.hippo.EditorManager.saveByTextareaId(editorId);   
    }, this.lConfig.timeoutLength);
};

/**
 * Explicitly replace <p> </p> with general-purpose space (U+0020) with a <p> </p> including a non-breaking space (U+00A0)
 * to prevent the browser from not rendering these paragraphs
 * 
 * See http://issues.onehippo.com/browse/HREPTWO-1713 for more info 
 */
AutoSave.prototype.inwardHtml = function(html) {
    this.imgRE = new RegExp('<p> <\/p>', 'gi');
    html = html.replace(this.imgRE, function(m) {
        return '<p>&nbsp;</p>';
    });
    return html;
};
