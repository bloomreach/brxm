/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Creates an HTML table with information about all CKEditor widgets (buttons, combo boxes, etc.) that are
 * available in a CKEditor distribution. The table lists for each widget:
 * - name
 * - label in the UI (the 'tool tip')
 * - associated CKEditor command
 * - CKEditor plugin the widget came from
 * - toolbar group in which the widget is placed by default
 * - position in the toolbar group
 *
 * Execute this script in the example page 'samples/replacebyclass.html' that is part of the CKEditor sources:
 * 1. open http://ckeditor.com/builder
 * 2. upload 'build-config.js' next to this file
 * 3. download the source version of CKEditor
 * 4. unzip the downloaded archive
 * 5. open 'samples/replacebyclass.html' in Chrome
 * 6. open the 'console' tab in Chrome developer tools
 * 7. paste the contents of this file into the console and hit <enter>
 *
 * The table should appear above the CKEditor instance.
 */

(function() {
    // hardcoded list of plugins per widget (the automatic lookup not always works correctly)
    var pluginNames = {
        Anchor: 'link',
        Button: 'button',
        Checkbox: 'forms',
        FontSize: 'font',
        Image: 'image',
        ImageButton: 'forms',
        Link: 'link',
        Radio: 'forms',
        Source: 'sourcearea, codemirror'
    }

    function addToUniqueArray(map, key, value) {
        if (map[key] !== undefined && map[key].indexOf(value) === -1) {
            map[key].push(value);
        } else {
            map[key] = [ value ];
        }
    }

    function getPluginPerLangKey() {
        var plugins = CKEDITOR.plugins.registered,
            map = {};
        for (var plugin in plugins) {
            if (plugins[plugin].langEntries) {
                for (langEntry in plugins[plugin].langEntries.en) {
                    addToUniqueArray(map, langEntry, plugin);
                }
            }
        }
        return map;
    }

    function getPluginPerLangValue() {
        var plugins = CKEDITOR.plugins.registered,
            map = {};
        for (var plugin in plugins) {
            if (plugins[plugin].langEntries) {
                for (langEntry in plugins[plugin].langEntries.en) {
                    addToUniqueArray(map, plugins[plugin].langEntries.en[langEntry], plugin);
                }
            }
        }
        return map;
    }

    function getPluginName(button, pluginPerLangKey, pluginPerLangValue) {
        var keyPlugins = pluginPerLangKey[button.command] || [],
            valuePlugins = pluginPerLangValue[button.label] || [];

        if (valuePlugins.length === 1) {
            return valuePlugins[0];
        } else if (keyPlugins.length === 1) {
            return keyPlugins[0];
        }
        return '???';
    }

    function getWidgets() {
        var widgets = [],
            pluginPerLangKey = getPluginPerLangKey(),
            pluginPerLangValue = getPluginPerLangValue(),
            buttons = CKEDITOR.instances.editor1.ui.items;

        for (buttonName in buttons) {
            var button = buttons[buttonName];

            if (button.type !== 'separator') {
                var groupAndPosition = button.toolbar.split(',');

                widgets.push({
                    name: buttonName,
                    label: button.label,
                    command: button.command || '',
                    plugin: pluginNames[buttonName] || getPluginName(button, pluginPerLangKey, pluginPerLangValue),
                    toolbarGroup: groupAndPosition[0],
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
            + "    <th style=\"text-align: left\">Widget</th>"
            + "    <th style=\"text-align: left\">Label</th>"
            + "    <th style=\"text-align: left\">Command</th>"
            + "    <th style=\"text-align: left\">Plugin</th>"
            + "    <th style=\"text-align: left\">Toolbar Group</th>"
            + "    <th style=\"text-align: left\">Position</th>"
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
