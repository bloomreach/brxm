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
//here for backward-compatibility of configurations, doesn't actually do anything anymore
Xinha.Config.prototype.AutoResize =
{
    'minHeight' : 150,
    'minWidth'  : 150
}

function AutoResize(editor) {
}
