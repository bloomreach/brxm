final String propertiesMarker = '<!-- PROPERTIES_MARKER -->'
final String cmsDependenciesMarker = '<!-- PARENT_PROJECT_DEPENDENCIES -->'
final String pomFile = 'pom.xml'

def projectLocation = request.getOutputDirectory() + "/" + request.getArtifactId()

println ""
println "Project location: $projectLocation"
println ""

def property = System.getProperty("site");

def rootModuleDir = new File(projectLocation)
def rootPomFile = new File(rootModuleDir, pomFile)
def cmsDependenciesModuleDir = new File(rootModuleDir, "cms-dependencies")
def cmsDependenciesPomFile = new File(cmsDependenciesModuleDir, pomFile)

if (property == null) {
//    Main project creation. No need to do anything extra here, just clear markers
    println "Creating main project"
    replacePlaceHolder(rootPomFile, propertiesMarker, '')
    replacePlaceHolder(cmsDependenciesPomFile, cmsDependenciesMarker, '')
} else if (property != null) {
    println "Creating site project"

    def parentGroupId = System.getProperty("parentGroupId")?.trim();
    if (parentGroupId != null) {
        println "Parent GroupId: $parentGroupId"
    }
    def parentArtifactId = System.getProperty("parentArtifactId")?.trim()
    if (parentArtifactId != null) {
        println "Parent artifactId: $parentArtifactId"
    }

    def parentVersion = System.getProperty("parentVersion")?.trim();
    if (parentVersion != null) {
        println "Parent version: $parentVersion"
    }

    parentGroupId = parentGroupId != null ? parentGroupId : System.console().readLine('Parent groupId: ')
    parentArtifactId = parentArtifactId != null ? parentArtifactId : System.console().readLine('Parent artifactId: ')
    parentVersion = parentVersion ? parentVersion : System.console().readLine ('Parent version: ')

    println "Updating " + rootPomFile

    def properties = """<!-- Parent project version -->
    <parent.project.version>$parentVersion</parent.project.version>"""
    replacePlaceHolder(rootPomFile, propertiesMarker, properties)

    println "Updating " + cmsDependenciesPomFile

    def cmdDepText = """<dependency>
      <groupId>$parentGroupId</groupId>
      <artifactId>$parentArtifactId-repository-data-application</artifactId>
      <version>$parentVersion</version>
    </dependency>"""
    replacePlaceHolder(cmsDependenciesPomFile, cmsDependenciesMarker, cmdDepText)
}

private static void replacePlaceHolder(File pomFile, String marker, String text) {
    def pomContent = pomFile.getText('UTF-8').replace(marker, text)
// rewrite pom.xml
    pomFile.newWriter().withWriter { w ->
        w << pomContent
    }
}
