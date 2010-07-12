/**
 * @description
 * <p>
 * Provides a singleton flash detection helper
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hippoajax, uploader
 * @module flash
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.Flash) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.FlashImpl = function() {
            this.config = null;
        };

        YAHOO.hippo.FlashImpl.prototype = {

            register : function(config) {
                this.config = config;
            },

            probe : function() {
                if(this.config != null) {
                    var playerVersion = YAHOO.deconcept.SWFObjectUtil.getPlayerVersion();
                    var url = this.config.callbackUrl + '&major=' + playerVersion.major + '&minor=' + playerVersion.minor + '&rev=' + playerVersion.rev;
                    this.config.callbackFunction(url);
                }
            }
        };

    })();

   YAHOO.hippo.Flash = new YAHOO.hippo.FlashImpl();

   YAHOO.register("flash", YAHOO.hippo.Flash, {
       version: "2.8.1", build: "19"
   });
}
