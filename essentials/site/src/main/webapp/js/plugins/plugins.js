define(['backbone', 'models'],
        function (Backbone, Models) {

            var Version = Backbone.RelationalModel.extend({
                relations: [
                    {
                        key: 'dependencies',
                        relatedModel: Models.Dependency,
                        type: 'HasMany'
                    }
                ]
            });

            var Plugin = Backbone.RelationalModel.extend({
                defaults: {
                    name: 'Plugin'
                },

                relations: [
                    {
                        key: 'versions',
                        relatedModel: Version,
                        type: 'HasMany'
                    }
                ]

            });

            var Plugins = Backbone.Collection.extend({
                model: Plugin,
                url: "/site/rest/plugins",

                initialize: function () {
                    this.deferred = this.fetch();
                },

                sync: function () {
                    if (arguments[0] === 'read') {
                        var options = arguments[2],
                                success = options.success;
                        options.success = function (resp) {
                            success(resp.plugins);
                        };
                        return Backbone.sync.call(this, 'read', arguments[1], options);
                    } else {
                        return Backbone.sync.apply(this, arguments);
                    }
                }

            });

            return new Plugins();
        });
