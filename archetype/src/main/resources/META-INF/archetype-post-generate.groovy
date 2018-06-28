final String ANSI_RESET = "\u001B[0m";
final String ANSI_BLACK = "\u001B[30m";
final String ANSI_RED = "\u001B[31m";
final String ANSI_GREEN = "\u001B[32m";
final String ANSI_YELLOW = "\u001B[33m";
final String ANSI_BLUE = "\u001B[34m";
final String ANSI_PURPLE = "\u001B[35m";
final String ANSI_CYAN = "\u001B[36m";
final String ANSI_WHITE = "\u001B[37m";


println "---------------------------------------------------------------"
println GroovySystem.version
println request.getOutputDirectory() + "/" + request.getArtifactId()
println "---------------------------------------------------------------"

def property = System.getProperty("site");

if (property == null) {
//    Main project creation. No need to do anything extra here (strip markers?)
    println ANSI_WHITE + "Creating main project$ANSI_RESET"
} else if (property != null) {
    println ANSI_WHITE + "Creating site project$ANSI_RESET"
    def parentGroupId = System.getProperty("parentGroupId")?.trim() ?
            System.getProperty("parentGroupId") : System.console().readLine('Parent project groupId: ')
    def parentArtifactId = System.getProperty("parentArtifactId")?.trim() ?
            System.getProperty("parentArtifactId") : System.console().readLine('Parent project artifactId: ')
    def parentVersion = System.getProperty("parentVersion")?.trim() ?
            System.getProperty("parentVersion") : System.console().readLine ('Parent project version: ')

    println "Parent GroupId: $parentGroupId"
    println "Parent artifactId: $parentArtifactId"
    println "Parent version: $parentVersion"

    def parentModuleDir = new File(request.getOutputDirectory() + "/" + request.getArtifactId())
    def propertiesMarker = '<!-- PROPERTIES_MARKER -->'
    def pomFile = new File(parentModuleDir, 'pom.xml')
    println "Updating " + pomFile

    def pomContent = pomFile.getText('UTF-8')

    def properties = """<!-- Parent project version -->
    <parent.project.version>$parentVersion</parent.project.version>"""

    pomContent = pomContent.replace(propertiesMarker, properties)

// rewrite pom.xml
    pomFile.newWriter().withWriter { w ->
        w << pomContent
    }

}
