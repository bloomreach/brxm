define(['backbone', 'jquery', 'underscore', 'events'],
        function (Backbone, jQuery, _, Events) {

            var socket = null;
            var connection = _.extend({

                subscribe: function (channel) {
                    socket.send(JSON.stringify({ type: 'subscribe', channel: channel }));
                },

                unsubscribe: function (channel) {
                    socket.send(JSON.stringify({ type: 'unsubscribe', channel: channel }));
                }

            }, Backbone.Events);

            if (window.location.protocol == 'http:') {
                connect('ws://' + window.location.host + '/site/events/hub');
            } else {
                connect('wss://' + window.location.host + '/site/events/hub');
            }
            var eventbus = new jQuery.Deferred();

            return eventbus;

            function connect(host) {
                if ('WebSocket' in window) {
                    socket = new WebSocket(host);
                } else if ('MozWebSocket' in window) {
                    socket = new MozWebSocket(host);
                } else {
                    console.log('Error: WebSocket is not supported by this browser.');
                    return;
                }

                socket.onopen = function () {
                    console.log('Info: WebSocket connection opened.');
                    eventbus.resolve(connection);
                };

                socket.onclose = function () {
                    connection.trigger('close');
                    Events.trigger('error', "Server connection was terminated.");
                    console.log('Info: WebSocket closed.');
                };

                socket.onmessage = function (message) {
                    connection.trigger('received', JSON.parse(message.data));
                    console.log(message.data);
                };

            }

        }
);
