define(['backbone', 'underscore', 'jquery'],
        function (Backbone, _, jQuery) {
            var events = _.extend({}, Backbone.Events);

            events.on('all', function () {
                console.log(this, arguments);
            });

            events.on('error', function (error) {
                jQuery('#feedback').html('<div class="alert">' + error.msg + '</div>');
                setTimeout(function () {
                    jQuery('#feedback').html('');
                }, 3000);
            });
            events.on('message', function (message) {
                jQuery('#feedback').html('<div class="alert alert-success">' + message.msg + '</div>');
                setTimeout(function () {
                    jQuery('#feedback').html('');
                }, 3000);
            });

            return events;
        });
