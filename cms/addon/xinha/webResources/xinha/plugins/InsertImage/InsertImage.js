  
InsertImage._pluginInfo = {
  name          : "InsertImage",
  origin        : "Xinha Core override",
  version       : "$LastChangedRevision: 1055 $".replace(/^[^:]*:\s*(.*)\s*\$$/, '$1'),
  developer     : "Arthur Bogaart (OneHippo)",
  developer_url :"http://www.onehippo.com/",
  c_owner :"OneHippo",
  license :"al2",
  sponsor :"OneHippo",
  sponsor_url :"http://www.onehippo.com/"
};

function InsertImage(editor) {
    this.editor = editor;
    var cfg = editor.config;
    var self = this;

    editor.config.btnList.insertimage[3] = function() { self.show(); }

    this.imgRE = new RegExp('<img[^>]+>', 'gi');
    this.srcRE = new RegExp('src="[^"]+"', 'i');
    this.noReplacePrefixes = ['http:', 'https:', '/'];
}

Xinha.Config.prototype.InsertImage = {
        callbackUrl: null        
};

InsertImage.prototype._lc = function(string) {
    return Xinha._lc(string, 'Xinha');
};

InsertImage.prototype.onGenerateOnce = function()
{
    InsertImage.loadAssets();
};

InsertImage.loadAssets = function()
{
    var self = InsertImage;
    if (self.loading) return;
    self.loading = true;
    Xinha._getback(_editor_url + 'modules/InsertImage/pluginMethods.js', function(getback) { eval(getback); self.methodsReady = true; });
};

InsertImage.prototype.onUpdateToolbar = function()
{ 
  if (!(InsertImage.methodsReady))
    {
      this.editor._toolbarObjects.insertimage.state("enabled", false);
  }
  else this.onUpdateToolbar = null;
};

InsertImage.prototype.prepareDialog = function()
{
    var config = this.editor.config;
    var callbackUrl = config.InsertImage.callbackUrl;

    this.dialog = new ModalDialog(callbackUrl, InsertImage._pluginInfo.name, config.xinhaParamToken, this.editor);
    // Connect the OK button
    var self = this;
    this.dialog.onOk = function(values) {
        
        //Workaround for strange value.value approach of f_align in Xinha/modules/InsertImage/pluginMethods.js ..
        if(!YAHOO.lang.isUndefined(values.f_align)) {
            values.f_align = {value: values.f_align};
        }
        
        var img = self.image;
        if(values.f_url == '' && img != null) { //Image should be removed
            var p = img.parentNode;
            while(img.hasChildNodes())
            {
                p.insertBefore(img.removeChild(img.childNodes[0]), img);
            }
            p.removeChild(img);
            self.editor.updateToolbar();
        } else {
            self.apply();
        }
        self.editor.plugins.AutoSave.instance.save();
    };
    this.dialogReady = true;
};


InsertImage.prototype.insertImage = function(values, openModal) {
    if (!this.dialog)
    {
      this.prepareDialog();
    }
    this.dialog.close(values);
    if(openModal) {
        this.dialog.show(values);
    }
}

/**
 * Prefix relative image sources with binaries prefix
 */
InsertImage.prototype.inwardHtml = function(html) {
    var _this = this;
    var prefix = this.getPrefix();    
    html = html.replace(this.imgRE, function(m) {
        m = m.replace(_this.srcRE, function(n) {
            var url = n.substring(5, n.length - 1);
            if(_this.shouldPrefix(url)) {
                return 'src="' + prefix + url + '"';
            }
            return n;
        });
        return m;
    });
    return html;
}

/**
 * Strip of binaries prefix
 */
InsertImage.prototype.outwardHtml = function(html) {
    var _this = this;
    var prefix = this.getPrefix();
    html = html.replace(this.imgRE, function(m) {
        m = m.replace(_this.srcRE, function(n) {
            var idx = n.indexOf(prefix);
            if (idx > -1) {
                return 'src="' + n.substr(prefix.length + idx);
            }
            return n;
        });
        return m;
    });
    return html;
}

InsertImage.prototype.getPrefix = function() {
    if (this.prefix == null) {
        this.prefix = this.editor.config.prefix;
        if (this.prefix.charAt(this.prefix.length - 1) != '/') {
            this.prefix += '/';
        }
    }
    return this.prefix;
}

InsertImage.prototype.shouldPrefix = function(url) {
    for (var i=0; i<this.noReplacePrefixes.length; i++) {
        if(url.indexOf(this.noReplacePrefixes[i]) == 0) 
            return false;
    }
    return true;
}

//Workaround for method not found error
InsertImage.prototype.stripBaseURL = function(url) {
    return url;
}
