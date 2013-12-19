package marketplace

class Vendor {
//    String content
    String name
    String introduction
    String website

    static constraints = {
//        content(maxSize: 200, nullable: true)
        introduction(nullable: true)
        website(url:true)
        name(nullable: false, maxSize: 50)
    }

    String toString() {
        name
    }

}
