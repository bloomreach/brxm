/**
 * Fork of original LinkPicker by James Sleeman of Gogo Internet Services (http://www.gogo.co.nz/)
 */

CustomLinker._pluginInfo = {
    name :"CustomLinker",
    version :"1.0",
    developer :"Arthur Bogaart",
    developer_url :"http://www.onehippo.com/",
    c_owner :"OneHippo",
    license :"al2",
    sponsor :"OneHippo",
    sponsor_url :"http://www.onehippo.com/"
};


Xinha.Config.prototype.CustomLinker = { 
        
};


function CustomLinker(editor, args) {
    this.editor = editor;
    this.lConfig = editor.config.CustomLinker;

    var linker = this;
    if (editor.config.btnList.createlink) {
        editor.config.btnList.createlink[3] = function(e, objname, obj) {
            linker._createLink(linker._getSelectedAnchor());
        };
    } else {
        editor.config.registerButton('createlink', 'Insert/Modify Hyperlink', [
                _editor_url + "images/ed_buttons_main.gif", 6, 1 ], false, function(e, objname, obj) {
            linker._createLink(linker._getSelectedAnchor());
        });
    }
    editor.config.addToolbarElement("createlink", "createlink", 0);
}

CustomLinker.prototype._lc = function(string) {
    return Xinha._lc(string, 'CustomLinker');
};

CustomLinker.prototype.createLink = function(url, openModal) {
	
	var a = null;
    var tmp = Xinha.uniq('http://www.example.com/Link');
    this.editor._doc.execCommand('createlink', false, tmp);

    var atr = {
        href : url,
        target :'',
        title :'',
        onclick :''
    };
    
    // Fix them up
    var anchors = this.editor._doc.getElementsByTagName('a');
    for ( var i = 0; i < anchors.length; i++) {
        var anchor = anchors[i];
        if (anchor.href == tmp) {
            // Found one.
            if (!a)
                a = anchor;
            for ( var j in atr) {
                anchor.setAttribute(j, atr[j]);
            }
        }
    }
    this.editor.selectNodeContents(a);
    this.editor.updateToolbar();
    
    if(openModal) {
    	this._createLink(a);
    }
}

CustomLinker.prototype._createLink = function(a) {
    if (!a && this.editor.selectionEmpty(this.editor.getSelection())) {
        alert(this._lc("You must select some text before making a new link."));
        return false;
    }

    var inputs = {
        type :'url',
        href :'',
        target :'',
        p_width :'',
        p_height :'',
        p_options : [ 'menubar=no', 'toolbar=yes', 'location=no', 'status=no', 'scrollbars=yes', 'resizeable=yes' ],
        to :'user@domain.com',
        subject :'',
        body :'',
        anchor :''
    };

    if (a && a.tagName.toLowerCase() == 'a') {
        var href = this.editor.fixRelativeLinks(a.getAttribute('href'));
        var m = href.match(/^mailto:(.*@[^?&]*)(\?(.*))?$/);
        var anchor = href.match(/^#(.*)$/);

        if (m) {
            // Mailto
            inputs.type = 'mailto';
            inputs.to = m[1];
            if (m[3]) {
                var args = m[3].split('&');
                for ( var x = 0; x < args.length; x++) {
                    var j = args[x].match(/(subject|body)=(.*)/);
                    if (j) {
                        inputs[j[1]] = decodeURIComponent(j[2]);
                    }
                }
            }
        } else if (anchor) {
            // Anchor-Link
            inputs.type = 'anchor';
            inputs.anchor = anchor[1];

        } else {

            if (a.getAttribute('onclick')) {
                var m = a.getAttribute('onclick').match(
                        /window\.open\(\s*this\.href\s*,\s*'([a-z0-9_]*)'\s*,\s*'([a-z0-9_=,]*)'\s*\)/i);

                // Popup Window
                inputs.href = href ? href : '';
                inputs.target = 'popup';
                inputs.p_name = m[1];
                inputs.p_options = [];

                var args = m[2].split(',');
                for ( var x = 0; x < args.length; x++) {
                    var i = args[x].match(/(width|height)=([0-9]+)/);
                    if (i) {
                        inputs['p_' + i[1]] = parseInt(i[2]);
                    } else {
                        inputs.p_options.push(args[x]);
                    }
                }
            } else {
                // Normal
                inputs.href = href;
                inputs.target = a.target;
            }
        }
    }

    var linker = this;

    // If we are not editing a link, then we need to insert links now using
    // execCommand
    // because for some reason IE is losing the selection between now and when
    // doOK is
    // complete. I guess because we are defocusing the iframe when we click
    // stuff in the
    // linker dialog.

    this.a = a; // Why doesn't a get into the closure below, but if I set it as
                // a property then it's fine?

    var doOK = function(values) {
        // if(linker.a) alert(linker.a.tagName);
        var a = linker.a;

        // var values = linker._dialog.hide();
        var atr = {
            href :'',
            target :'',
            title :'',
            onclick :''
        };

        if (values.type == 'url') {
            if (values.href) {
                atr.href = values.href;
                atr.target = values.target;
                if (values.target == 'popup') {

                    if (values.p_width) {
                        values.p_options.push('width=' + values.p_width);
                    }
                    if (values.p_height) {
                        values.p_options.push('height=' + values.p_height);
                    }
                    atr.onclick = 'if(window.top && window.top.Xinha){return false}window.open(this.href, \''
                            + (values.p_name.replace(/[^a-z0-9_]/i, '_')) + '\', \'' + values.p_options.join(',')
                            + '\');return false;';
                }
            }
        } else if (values.type == 'anchor') {
            if (values.anchor) {
                atr.href = values.anchor.value;
            }
        } else {
            if (values.to) {
                atr.href = 'mailto:' + values.to;
                if (values.subject)
                    atr.href += '?subject=' + encodeURIComponent(values.subject);
                if (values.body)
                    atr.href += (values.subject ? '&' : '?') + 'body=' + encodeURIComponent(values.body);
            }
        }

        if (a && a.tagName.toLowerCase() == 'a') {
            if (!atr.href) {
                //if (confirm(linker._lc('Are you sure you wish to remove this link?'))) {
                if(true) {
                    var p = a.parentNode;
                    while (a.hasChildNodes()) {
                        p.insertBefore(a.removeChild(a.childNodes[0]), a);
                    }
                    p.removeChild(a);
                    linker.editor.updateToolbar();
                    return;
                }
            } else {
                // Update the link
                for ( var i in atr) {
                    a.setAttribute(i, atr[i]);
                }

                // If we change a mailto link in IE for some hitherto unknown
                // reason it sets the innerHTML of the link to be the
                // href of the link. Stupid IE.
                if (Xinha.is_ie) {
                    if (/mailto:([^?<>]*)(\?[^<]*)?$/i.test(a.innerHTML)) {
                        a.innerHTML = RegExp.$1;
                    }
                }
            }
        } else {
            if (!atr.href)
                return true;

            // Insert a link, we let the browser do this, we figure it knows
            // best
            var tmp = Xinha.uniq('http://www.example.com/Link');
            linker.editor._doc.execCommand('createlink', false, tmp);

            // Fix them up
            var anchors = linker.editor._doc.getElementsByTagName('a');
            for ( var i = 0; i < anchors.length; i++) {
                var anchor = anchors[i];
                if (anchor.href == tmp) {
                    // Found one.
                    if (!a)
                        a = anchor;
                    for ( var j in atr) {
                        anchor.setAttribute(j, atr[j]);
                    }
                }
            }
        }
        linker.editor.selectNodeContents(a);
        linker.editor.updateToolbar();
    };

    ModalDialog.openModal(this.lConfig.callbackUrl, CustomLinker._pluginInfo.name, this.editor.config.xinhaParamToken,
            doOK, inputs);

};

CustomLinker.prototype._getSelectedAnchor = function() {
    var sel = this.editor.getSelection();
    var rng = this.editor.createRange(sel);
    var a = this.editor.activeElement(sel);
    if (a != null && a.tagName.toLowerCase() == 'a') {
        return a;
    } else {
        a = this.editor._getFirstAncestor(sel, 'a');
        if (a != null) {
            return a;
        }
    }
    return null;
};
