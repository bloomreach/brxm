package org.hippoecm.hst.pagecomposer.dependencies;

public class VersionedDependency extends PathDependency {

    public VersionedDependency(String path, String version) {
        super(parsePath(path, version));
    }

    private static String parsePath(String path, String version) {
        if(path.endsWith("/")) {
           return path + version; 
        }
        return path + "/" + version;
    }

}
