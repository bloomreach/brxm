define(['underscore', 'backbone', 'models', 'eventbus'],
        function (_, Backbone, Models, Eventbus) {

            var ProjectDependencies = Backbone.Collection.extend({
                model: Models.Dependency,
                url: "/site/rest/manager/list",

                initialize: function () {
                    Eventbus.then(_.bind(function (connection) {
                        connection.subscribe('site');
                        connection.subscribe('cms');

                        connection.on('received', _.bind(function () {
                            this.fetch();
                        }, this));
                    }, this));
                }

            });

            var Project = Backbone.RelationalModel.extend({
                relations: [
                    {
                        key: 'dependencies',
                        type: 'HasMany',
                        relatedModel: Models.Dependency,
                        collectionType: ProjectDependencies
                    }
                ],

                initialize: function () {
                    this.deferred = this.get('dependencies').fetch();
                    this.get('dependencies').on('sync', _.bind(function () {
                        this.trigger('sync');
                    }, this));
                },

                hasPlugin: function (plugin) {
                    var self = this;
                    return plugin.get('versions').any(function (version) {
                        return version.get('dependencies').every(function (dependency) {
                            var found = self.get('dependencies').find(function (pluginDep) {
                                return _.isEqual(pluginDep.attributes, dependency.attributes);
                            });
                            return found !== undefined;
                        });
                    });
                }
            });

            return new Project();
        });
