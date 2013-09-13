<h1>Plugins</h1>
<% _.each(plugins, function(plugin) { %>
<section class="compact-plugin" data-id="<%= plugin.id %>">
    <h4><%= plugin.name + (plugin.installed ? "*" : "") %></h4>
    <p><%= plugin.description %></p>
</section>
<% }); %>
