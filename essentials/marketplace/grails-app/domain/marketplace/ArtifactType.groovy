package marketplace

/**
 * User: obourgeois
 * Date: 27-12-13
 */
public enum ArtifactType {
    CMS("CMS"),
    SITE("Site")

    private final String value

    ArtifactType(String value) {
        this.value = value;
    }

    String toString() {
        value
    }

    String getKey() {
        name()
    }

    static list(){
        [CMS,SITE]
    }
}