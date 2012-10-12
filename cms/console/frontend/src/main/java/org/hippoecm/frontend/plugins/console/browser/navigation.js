
Hippo = Hippo || {};
Hippo.Tree = Hippo.Tree || {};
Hippo.Tree.addShortcuts = function(callbackUrl) {

    var register = function(key) {
        shortcut.add(key, function() {
            wicketAjaxGet(callbackUrl+'&key=' + key);
        }, {
            'disable_in_input': true
        });
    };
    register('Up');
    register('Down');
    register('Left');
    register('Right');
};
