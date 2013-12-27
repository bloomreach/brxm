package marketplace

class Artifact {
    String artifactId
    String groupId
    String scope
    // Version is a reserved column name
    String aversion
    ArtifactType artifactType

    static constraints = {
        artifactId(nullable: false)
        aversion(nullable: true)
        artifactType(nullable: false)
    }

    String toString() {
        artifactId
    }
}
