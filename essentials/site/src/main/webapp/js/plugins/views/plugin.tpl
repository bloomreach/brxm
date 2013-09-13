<section>
    <h3><%= plugin.name %></h3>
    <dl>
        <dt>Installed</dt>
        <dd><% if (plugin.installed) { %>
            <span class="installed">Yes</span>
            <% } else { %>
            <a href="#" class="not-installed">No</a>
            <% } %>
        </dd>

        <dt>Versions</dt>
        <ul>
            <li>
                <% _.each(plugin.versions, function(tag) { %>
                <h4><%= tag.version %></h4>
                <dl>
                    <% _.each(tag.dependencies, function(dependency) { %>
                    <dt><%= dependency.artifactId + ' (' + dependency.projectType + ')' %></dt>
                    <dd>
                        <blockquote style="white-space: pre;">
                            &lt;dependency&gt;
                            &lt;artifactId&gt;<%=dependency.artifactId%>&lt;/artifactId&gt;
                            &lt;groupId&gt;<%=dependency.groupId%>&lt;/groupId&gt;
                            &lt;scope&gt;<%=dependency.scope%>&lt;/scope&gt;
                            &lt;version&gt;<%=dependency.version%>&lt;/version&gt;
                            &lt;/dependency&gt;
                        </blockquote>
                    </dd>
                    <% }); %>
                </dl>
                <% }); %>
            </li>
        </ul>
    </dl>
</section>
