'use strict';
(function (WizardModel, undefined) {


    var pages = [];

    WizardModel.alert = function (msg) {
        console.log(msg);
    };

    WizardModel.addPage = function (page) {
        pages.push(page);
    };

    WizardModel.createPage = function(pageName){
        var page = {};
        page.name = pageName;
        page.selected = false;
        return page;
    }
   



})(window.WizardModel = window.WizardModel || {}, undefined);