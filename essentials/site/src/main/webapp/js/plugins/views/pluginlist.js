define(['underscore', 'backbone', 'jquery', 'text!views/pluginlist.tpl'],
        function (_, Backbone, jQuery, html) {

            function pluginToJson(plugin, project) {
                var asJson = plugin.toJSON();
                asJson.installed = project.hasPlugin(plugin);
                return asJson;
            }

            return Backbone.View.extend({
                template: _.template(html),

                events: {
                    'click section': '_select'
                },

                initialize: function (options) {
                    _.bindAll(this, 'render', '_select');
                    this.plugins = options.plugins;
                    this.project = options.project;
                    this.listenTo(this.project, 'all', this.render);
                },

                render: function () {
                    this.$el.html('<p>Loading...</p>');
                    jQuery.when(this.plugins.deferred, this.project.deferred)
                            .done(_.bind(function () {
                                var plugins = [];
                                var project = this.project;
                                this.plugins.each(function (plugin) {
                                    plugins.push(pluginToJson(plugin, project));
                                });
                                this.$el.html(this.template({
                                    plugins: plugins
                                }));
                            }, this))
                            .fail(_.bind(function () {
                                this.$el.html('<p>Failed to load</p>');
                            }, this));
                    return this;
                },

                _select: function (event) {
                    var id = $(event.currentTarget).attr('data-id');
                    this.trigger('select', id);
                    event.preventDefault();
                }
            });
        }
);