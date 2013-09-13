define(['underscore', 'backbone', 'events', 'views/plugin', 'views/pluginlist', 'plugins', 'project', 'commands'],
        function (_, Backbone, Events, PluginView, PluginListView, Plugins, Project, Commands) {

            var currentView, state = new Backbone.Model();

            return Backbone.Router.extend({

                //definition of routes
                routes: {
                    '': 'index',
                    'plugin/:id': 'showPlugin'
                },

                //index route
                index: function () {
                    state.set({
                        'selected-plugin': undefined
                    });
                },

                showPlugin: function (pluginId) {
                    state.set({
                        'selected-plugin': pluginId
                    });
                },

                //constructor for the router
                initialize: function (options) {
                    this.$content = options.content;
                    this.$content.css({
                        overflow: 'hidden',
                        whiteSpace: 'nowrap',
                        width: this.$content.width()
                    });

                    _fireEventsOnStateChange.apply(this);
                    _updateURLAndUIOnEvents.apply(this);
                    _createUI.apply(this);
                },

                start: function () {
                    // Start Backbone routing
                    Backbone.history.start();
                }

            });

            function _fireEventsOnStateChange() {
                // fire events on application state changes
                state.on('change', _.bind(function (state) {
                    if (state.hasChanged('selected-plugin')) {
                        Events.trigger('select-plugin', state.get('selected-plugin'));
                    }
                }, this));
            }

            function _updateURLAndUIOnEvents() {
                Events.on('select-plugin', _.bind(function (pluginId) {
                    _updateURL.apply(this);
                    _createUI.call(this, pluginId);
                }, this));
            }

            function _updateURL() {
                var pluginId = state.get('selected-plugin');
                if (pluginId) {
                    this.navigate("plugin/" + pluginId);
                } else {
                    this.navigate("");
                }
            }

            function _createUI(pluginId) {
                if (pluginId) {
                    Plugins.deferred
                            .done(_.bind(function () {
                                var plugin = Plugins.get(pluginId),
                                        view = new PluginView({ plugin: plugin, project: Project });
                                view.on('install', function (plugin) {
                                    Commands.trigger('install-plugin', plugin);
                                });
                                _showContent.call(this, view, '<');
                            }, this));
                } else {
                    Plugins.deferred
                            .done(_.bind(function () {
                                var view = new PluginListView({ plugins: Plugins, project: Project });
                                view.on('select', function (pluginId) {
                                    state.set({'selected-plugin': pluginId});
                                });
                                _showContent.call(this, view, '>');
                            }, this));
                }
            }

            function _showContent(newView, dir) {
                var oldView = currentView;
                currentView = newView;

                var newContent = newView.render().el;
                if (dir == '<') {
                    this.$content.append(newContent);
                } else {
                    this.$content.prepend(newContent);
                }

                if (oldView) {
                    var distance = this.$content.width();

                    var oldDir = (dir == '<' ? '-' : '+');
                    oldView.$el.animate({
                        marginLeft: oldDir + '=' + distance
                    }, 400, 'linear', function () {
                        oldView.remove();
                    });

                    var newDir = (dir == '<' ? '+' : '-');
                    newView.$el.css({
                        marginLeft: newDir + '=' + distance,
                        float: 'left'
                    }).animate({
                                marginLeft: oldDir + '=' + distance
                            }, 400, 'linear');
                } else {
                    newView.$el.css({
                        float: 'left'
                    });
                }

            }

        });
