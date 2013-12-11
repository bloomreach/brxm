'use strict';
var WM = (function () {
    var pages = [];
    var pageNames = [];
    var previous = null;
    var next = null;
    var selected = null;


    return {

        createModel: function () {
            return this;
        },
        addPage: function (pageName) {
            var page = {};
            page.name = pageName;
            page.selected = false;
            pages.push(page);
            pageNames.push(pageName);

        },
        getSelectedArray:function(){
            var array = [];
            for (var i = 0; i < pages.length; i++) {
                var page = pages[i];
                array.push(page.selected);
            }
            return  array;
        },
        pages: pageNames,

        getSelected: function () {
            return selected;
        },
        getPrevious: function () {
            return previous;
        },
        getNext: function () {
            return next;
        },
        getPages: function () {
            return pages;
        },
        setSelected: function (idx) {
            if (idx < pages.length) {
                previous = selected;
                if (previous) {
                    previous.selected = false;
                }
                selected = pages[idx];
                selected.selected = true;
                next = pages[idx + 1];
            }

        }



    };


}());

