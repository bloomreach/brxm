import java.nio.file.Paths

final String propertiesMarker = '<!-- PROPERTIES_MARKER -->'
final String groupAfterMarker = '<!-- GROUP_AFTER -->'
final String cmsDependenciesMarker = '<!-- PARENT_PROJECT_DEPENDENCIES -->'
final String pomFile = 'pom.xml'
final String hcmModuleFilename = "hcm-module.yaml"

def projectLocation = request.getOutputDirectory() + "/" + request.getArtifactId()

println ""
println "Project location: $projectLocation"
println ""

def property = System.getProperty("site");

def rootModuleDir = new File(projectLocation)
def rootPomFile = new File(rootModuleDir, pomFile)
def cmsDependenciesModuleDir = new File(rootModuleDir, "cms-dependencies")
def cmsDependenciesPomFile = new File(cmsDependenciesModuleDir, pomFile)

def repositoryDataDir = Paths.get(projectLocation).resolve("repository-data")

def appModuleDescriptorFile = repositoryDataDir.resolve("application")
        .resolve("src").resolve("main").resolve("resources").resolve(hcmModuleFilename).toFile();
def devModuleDescriptorFile = repositoryDataDir.resolve("development")
        .resolve("src").resolve("main").resolve("resources").resolve(hcmModuleFilename).toFile();
def siteModuleDescriptorFile = repositoryDataDir.resolve("site")
        .resolve("src").resolve("main").resolve("resources").resolve(hcmModuleFilename).toFile();
def webfilesModuleDescriptorFile = repositoryDataDir.resolve("webfiles")
        .resolve("src").resolve("main").resolve("resources").resolve(hcmModuleFilename).toFile();

def afterGroup = 'after: hippo-cms'
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
      <version>\${parent.project.version}</version>
    </dependency>"""
    replacePlaceHolder(cmsDependenciesPomFile, cmsDependenciesMarker, cmdDepText)

    afterGroup = "after: $parentArtifactId"

    //remove repository-data/application configuration & security yaml definitions as they've been already defined at
    //parent module
    def appModuleConfigDir = repositoryDataDir.resolve("application")
            .resolve("src").resolve("main").resolve("resources").resolve("hcm-config")
    appModuleConfigDir.resolve("configuration").toFile().deleteDir()
    appModuleConfigDir.resolve("security").toFile().deleteDir()
}

replacePlaceHolder(appModuleDescriptorFile, groupAfterMarker, afterGroup)
replacePlaceHolder(devModuleDescriptorFile, groupAfterMarker, afterGroup)
replacePlaceHolder(siteModuleDescriptorFile, groupAfterMarker, afterGroup)
replacePlaceHolder(webfilesModuleDescriptorFile, groupAfterMarker, afterGroup)

private static void replacePlaceHolder(File file, String marker, String text) {
    def fileContent = file.getText('UTF-8').replace(marker, text)
    file.newWriter().withWriter { w ->
        w << fileContent
    }
}
