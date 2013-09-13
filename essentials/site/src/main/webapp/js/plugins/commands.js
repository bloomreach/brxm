/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

define(['jquery', 'underscore', 'backbone', 'events'],
        function (jQuery, _, Backbone, Events) {

            var commands = _.extend({}, Backbone.Events);

            commands.on('install-plugin', function (plugin) {
                jQuery.ajax({
                    method: 'POST',
                    contentType: "application/json; charset=utf-8",
                    url: '/site/rest/manager/install',
                    data: JSON.stringify(plugin.toJSON()),

                    dataType: 'json'
                }).success(function () {
                            Events.trigger('message', {
                                msg: 'Installed plugin ' + plugin.get('name')
                            });
                        }).error(function () {
                            Events.trigger('error', {
                                msg: 'Could not install plugin ' + plugin.get('name')
                            });
                        });
            });

            commands.on('all', function () {
                console.log(this, arguments);
            });

            return commands;
        }
);
