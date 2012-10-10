
initHippoTree = function(id, callbackUrl) {
    var register = function(key) {
        shortcut.add(key,function() {
            wicketAjaxGet(callbackUrl+'&key=' + key);
        },{
            'disable_in_input': false,
            'type': 'keydown',
            'propagate': false,
            'target': document
        });
    };
    register('Up');
    register('Down');
    register('Left');
    register('Right');
};
