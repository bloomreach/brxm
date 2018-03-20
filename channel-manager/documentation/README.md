The Maven build generates documentation from sources in Markdown (text) and PlantUML (diagrams):

    $ mvn
    
The resulting HTML file can be viewed in a browser:

    target/html/channel-manager-guidebook.html

The PlantUML diagrams are only generated when [Graphviz](https://www.graphviz.org) is installed.
The diagrams can also be previewed in IntelliJ using the PlantUML plugin.
