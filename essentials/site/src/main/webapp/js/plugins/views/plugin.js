define(['underscore', 'backbone', 'jquery', 'text!views/plugin.tpl'],
        function (_, Backbone, jQuery, html) {

            function pluginToJson(plugin, project) {
                var asJson = plugin.toJSON();
                asJson.installed = project.hasPlugin(plugin);
                return asJson;
            }

            return Backbone.View.extend({
                template: _.template(html),

                events: {
                    'click a.not-installed': '_install'
                },

                initialize: function (options) {
                    _.bindAll(this, 'render', '_install');
                    this.plugin = options.plugin;
                    this.project = options.project;
                    this.listenTo(this.project, 'sync', this.render);
                },

                render: function () {
                    var rendered = this.template({
                        plugin: pluginToJson(this.plugin, this.project)
                    });
                    this.$el.html(rendered);
                    return this;
                },

                _install: function (e) {
                    this.trigger('install', this.plugin);
                    e.preventDefault();
                }
            });
        }
);
