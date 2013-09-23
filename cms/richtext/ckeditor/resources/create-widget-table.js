//
// Creates an HTML table with information about all CKEditor widgets (buttons, combo boxes, etc.) that are
// available in a CKEditor distribution. The table lists for each widget:
// - name
// - label in the UI (the 'tool tip')
// - associated CKEditor command
// - CKEditor plugin the widget came from
// - toolbar group in which the widget is placed by default
// - position in the toolbar group
//
// Execute this script in the example page 'samples/replacebyclass.html' that is part of the CKEditor sources:
// 1. open http://ckeditor.com/builder
// 2. upload 'build-config.js' next to this file
// 3. download the source version of CKEditor
// 4. unzip the downloaded archive
// 5. open 'samples/replacebyclass.html' in Chrome
// 6. open the 'console' tab in Chrome developer tools
// 7. paste the contents of this file into the console and hit <enter>
//
// The table should appear above the CKEditor instance.
//
(function() {
    function getPluginPerCommand() {
        var plugins = CKEDITOR.plugins.registered,
            map = {};
        for (var plugin in plugins) {
            if (plugins[plugin].langEntries) {
                for (langEntry in plugins[plugin].langEntries.en) {
                    map[langEntry] = plugin;
                }
            }
        }
        return map;
    }

    function getWidgets() {
        var widgets = [],
            commandToPluginMap = getPluginPerCommand(),
            buttons = CKEDITOR.instances.editor1.ui.items;
        for (buttonName in buttons) {
            var button = buttons[buttonName];
            if (button.type !== 'separator') {
                var groupAndPosition = button.toolbar.split(','),
                    group = groupAndPosition[0];

                widgets.push({
                    name: buttonName,
                    label: button.label,
                    command: button.command || '',
                    plugin: commandToPluginMap[button.command] || '',
                    toolbarGroup: group,
                    position: groupAndPosition.length > 1 ? groupAndPosition[1] : ''
                });
            }
        }
        return widgets;
    }

    function createHtmlTable(widgets) {
        var html = "<style>table { border: 1px solid #000 } th, td { padding: 5px; border-collapse: collapse; text-align: left; }</style>"
            + '<table style="width: 100%; border: 1px solid rgb(0, 0, 0); border-collapse: collapse">'
            + "  <tr>"
            + "    <th>Widget</th>"
            + "    <th>Label</th>"
            + "    <th>Command</th>"
            + "    <th>Plugin</th>"
            + "    <th>Toolbar Group</th>"
            + "    <th>Position</th>"
            + "  </tr>";

        for (var i = 0; i < widgets.length; i++) {
            html += "  <tr>"
                + "    <td>" + widgets[i].name + "</td>"
                + "    <td>" + widgets[i].label + "</td>"
                + "    <td>" + widgets[i].command + "</td>"
                + "    <td>" + widgets[i].plugin + "</td>"
                + "    <td>" + widgets[i].toolbarGroup + "</td>"
                + "    <td>" + widgets[i].position + "</td>"
                + "  </tr>";
        }

        html += "</table>";

        return html;
    }

    var widgets = getWidgets();

    widgets.sort(function(a, b) {
        return a.name.localeCompare(b.name);
    });

    new CKEDITOR.dom.element($("div.description")).appendHtml(createHtmlTable(widgets));
}());
