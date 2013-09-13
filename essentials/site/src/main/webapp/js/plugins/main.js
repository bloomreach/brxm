requirejs.config({

    shim: {
        'backbone-core': {
            deps: ['underscore', 'jquery'],
            exports: 'Backbone'
        },
        'backbone': {
            deps: ['backbone-core'],
            exports: 'Backbone'
        },
        'underscore': {
            exports: '_'
        }
    },

    paths: {
        'jquery': '../../webjars/jquery/1.9.1/jquery',
        'underscore': '../../webjars/underscorejs/1.4.4/underscore',
        'backbone-core': '../../webjars/backbonejs/1.0.0/backbone',
        'backbone': '../backbone-relational'
    }

});

define(['jquery', 'underscore', 'backbone', 'router'],
        function (jQuery, _, Backbone, Router) {
            jQuery(document).ready(function () {
                var router = new Router({
                    content: jQuery('#content')
                });
                router.start();
            });
        });
