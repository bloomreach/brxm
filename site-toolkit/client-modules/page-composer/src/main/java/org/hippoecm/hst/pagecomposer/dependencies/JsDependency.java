package org.hippoecm.hst.pagecomposer.dependencies;

import java.util.LinkedList;
import java.util.List;

public class JsDependency extends BaseDependency implements Dependency{

    private List<JsDependency> devDependencies;

    public JsDependency(String path) {
        super(path);
    }

    public JsDependency(String path, String... devModePaths) {
        super(path);
        devDependencies = new LinkedList<JsDependency>();
        for(String devPath : devModePaths) {
            devDependencies.add(new JsDependency(devPath));
        }
    }

    @Override
    protected void addSelf(List<Dependency> all, boolean devMode) {
        if(devMode && devDependencies != null) {
            for(Dependency dependency : devDependencies) {
                dependency.setParent(getParent());
                all.add(dependency);
            }
        } else {
            super.addSelf(all, devMode);
        }
    }

    @Override
    public String asString(String path) {
        return "<script type=\"text/javascript\" src=\"" + path + "\"></script>";
    }

}
