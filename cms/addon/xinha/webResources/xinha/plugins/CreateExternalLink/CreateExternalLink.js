function CreateExternalLink(editor, args) {
    this.editor = editor;
    var cfg = editor.config;
    var self = this;
    editor.config.registerButton('createexternallink', this._lc('External link'), [_editor_url + cfg.imgURL + "ed_buttons_main.png",5,1], false, function() { self.show(self._getSelectedAnchor()); });
}

CreateExternalLink._pluginInfo = {
    name :"CreateExternalLink",
    version :"1.0",
    developer :"Arthur Bogaart",
    developer_url :"http://www.onehippo.com/",
    c_owner :"OneHippo",
    license :"al2",
    sponsor :"OneHippo",
    sponsor_url :"http://www.onehippo.com/"
};

Xinha.Config.prototype.CreateExternalLink= {
    callbackUrl: null        
};

CreateExternalLink.prototype._lc = function(string) {
    return Xinha._lc(string, {url : _editor_url + 'plugins/CreateExternalLink/lang/', context:"CreateExternalLink"});
};

CreateExternalLink.prototype.onGenerateOnce = function()
{
  CreateExternalLink.loadAssets();
};

CreateExternalLink.loadAssets = function()
{
    var self = CreateExternalLink;
    if (self.loading) return;
    self.loading = true;
    var t =  this;
    Xinha._getback(_editor_url + 'modules/CreateLink/pluginMethods.js', 
        function(getback) { 
            eval(getback);
            if(Xinha.is_ie) {
                self.prototype.show = CreateExternalLink.prototype.showIE;
            } else {
                self.prototype.show = CreateLink.prototype.show;
            }
            self.prototype.apply = CreateLink.prototype.apply;
            self.prototype._getSelectedAnchor = CreateLink.prototype._getSelectedAnchor;
            self.methodsReady = true; 
        }
    );
}

CreateExternalLink.prototype.showIE = function(a)
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

CreateExternalLink.prototype.onUpdateToolbar = function()
{ 
    if (!(ModalDialog && CreateExternalLink.methodsReady))
    {
        this.editor._toolbarObjects.createexternallink.state("enabled", false);
    }
    else this.onUpdateToolbar = null;
};


CreateExternalLink.prototype.prepareDialog = function()
{
    var config = this.editor.config;
    var callbackUrl = config.CreateExternalLink.callbackUrl;

    this.dialog = new ModalDialog(callbackUrl, CreateExternalLink._pluginInfo.name, config.xinhaParamToken, this.editor);
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
        self.editor.plugins.AutoSave.instance.saveSynchronous();
    };
    this.dialogReady = true;
};