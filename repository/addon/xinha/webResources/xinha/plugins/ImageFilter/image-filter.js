/*------------------------------------------*\
Imagefilter for Xinha
____________________
Add a prefix to images with a relative @src value
\*------------------------------------------*/

ImageFilter._pluginInfo = {
    name :"ImageFilter",
    version :"1.0",
    developer :"Arthur Bogaart",
    developer_url :"http://www.onehippo.org",
    c_owner :"Arthur Bogaart",
    sponsor :"",
    sponsor_url :"",
    license :""
}

function ImageFilter(editor) {
    this.editor = editor;
    this.prefix = _editor_jcrnode_url;
    if (this.prefix.charAt(this.prefix.length - 1) != '/') {
        this.prefix += '/';
    }
    this.imgRE = new RegExp('<img[^>]+>', 'gi');
    this.srcRE = new RegExp('src="[^"]+"', 'i');
}

ImageFilter.prototype.inwardHtml = function(html) {
    // add node prefix to all images
    var _this = this;
    html = html.replace(this.imgRE, function(m) {
        m = m.replace(_this.srcRE, function(n) {
            var val = n.substring(5, n.length - 1);
            if (val.indexOf('/') == 0 || val.indexOf('http:') == 0)
                return n;
            else {
                return 'src="' + _this.prefix + val + '"';
            }
        });
        return m;
    });
    return html;
}

ImageFilter.prototype.outwardHtml = function(html) {
    var _this = this;
    html = html.replace(this.imgRE, function(m) {
        m = m.replace(_this.srcRE, function(n) {
            var idx = n.indexOf(_this.prefix);
            if (idx > -1) {
                return 'src="' + n.substr(_this.prefix.length + idx);
            }
            return n;
        });
        return m;
    });
    return html;
}