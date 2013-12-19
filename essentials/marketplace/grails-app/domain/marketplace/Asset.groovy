package marketplace

class Asset {

    String identifier;
    String url
    String data
    String mimeType = "plain/text"

    static constraints = {
        identifier nullable: false
        url url: true
        data nullable: true
    }
}
