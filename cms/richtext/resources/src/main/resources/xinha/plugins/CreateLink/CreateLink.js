function CreateLink(editor, args) {
    this.editor = editor;
    var cfg = editor.config;
    var self = this;
    
    editor.config.btnList.createlink[0] = this._lc('Internal link');
    editor.config.btnList.createlink[3] = function() {
        self.show(self._getSelectedAnchor()); 
    };

    if(typeof editor._createLink == 'undefined') {
        editor._createLink = function(target) {
            if(!target) {
              target = self._getSelectedAnchor();
            }

            if(target && target.tagName.toLowerCase() == 'a' && target.href.length > 0) {
              var attrValue = editor.fixRelativeLinks(target.getAttribute('href', 2));
              var startsWith = function(str) {
                return attrValue.match("^"+str) == str;
              };
              
              if(startsWith('http://') || startsWith('https://') || startsWith('mailto:') || startsWith('ftp://')) {
                editor._createExternalLink();
                return;
              }
            }
            self.show(target);
        };
    }
}

CreateLink._pluginInfo = {
    name :"CreateLink",
    version :"1.0",
    developer :"Arthur Bogaart",
    developer_url :"http://www.onehippo.com/",
    c_owner :"OneHippo",
    license :"al2",
    sponsor :"OneHippo",
    sponsor_url :"http://www.onehippo.com/"
};

Xinha.Config.prototype.CreateLink= {
    callbackUrl: null        
};

CreateLink.prototype._lc = function(string) {
    return Xinha._lc(string, {url : _editor_url + 'plugins/CreateLink/lang/', context:"CreateLink"});
};

CreateLink.prototype.onGenerateOnce = function()
{
  CreateLink.loadAssets();
};

CreateLink.loadAssets = function()
{
    var self = CreateLink;
    if (self.loading) return;
    self.loading = true;
    Xinha._getback(_editor_url + 'modules/CreateLink/pluginMethods.js', 
        function(getback) { 
            eval(getback); 
            self.methodsReady = true;
            if(Xinha.is_ie) {
                CreateLink.prototype.show = CreateLink.prototype.showIE;
            }
        }
    );
}

CreateLink.prototype.showIE = function(a)
{
  if (!this.dialog)
  {
    this.prepareDialog();
  } 
    var editor = this.editor;
    this.a = a;
    if(!a && this.editor.selectionEmpty(this.editor.getSelection()))
    {
        alert(this._lc("You need to select some text before creating a link"));
        return false;
    }

    var inputs =
    {
        f_href   : '',
        f_title  : '',
        f_target : '',
        f_other_target : ''
    };
    
    if(a && a.tagName.toLowerCase() == 'a')
    {
        //IE IFlags: http://tobielangel.com/2007/1/11/attribute-nightmare-in-ie/
        inputs.f_href   = this.editor.fixRelativeLinks(a.getAttribute('href', 2));

        inputs.f_title  = a.title;
        if (a.target)
        {
            if (!/_self|_top_|_blank/.test(a.target))
            {
                inputs.f_target = '_other';
                inputs.f_other_target = a.target;
            }
            else
            {
                inputs.f_target = a.target;
                inputs.f_other_target = '';
            }
        }
    }

    // now calling the show method of the Xinha.Dialog object to set the values and show the actual dialog
    this.dialog.show(inputs);
};

CreateLink.prototype.onUpdateToolbar = function()
{ 
    if (!(ModalDialog && CreateLink.methodsReady))
    {
        this.editor._toolbarObjects.createlink.state("enabled", false);
    }
    else this.onUpdateToolbar = null;
};


CreateLink.prototype.prepareDialog = function()
{
    var config = this.editor.config;
    var callbackUrl = config.CreateLink.callbackUrl;

    this.dialog = new ModalDialog(callbackUrl, CreateLink._pluginInfo.name, config.xinhaParamToken, this.editor);
    // Connect the OK button
    var self = this;
    this.dialog.onOk = function(values) {
        //Xinha handles the f_target value as an object instead of a string; if it is set in the Dialog
        //convert the string value to an object
        if (self.dialog._values.f_target != undefined && self.dialog._values.f_target != null) {
            var v = { value: self.dialog._values.f_target};
            self.dialog._values.f_target = v;
        }
        self.apply();
        self.editor.plugins.AutoSave.instance.save();
    };
    this.dialogReady = true;
};

CreateLink.prototype._getSelectedAnchor = function() {
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

/**
 * Add link from outside Xinha (like DragDrop)
 */

CreateLink.prototype.createLink = function(values, openModal) {
    if (!this.dialog)
    {
      this.prepareDialog();
    }
    this.dialog.close(values);
    if(openModal) {
        this.dialog.show(values);
    }
}