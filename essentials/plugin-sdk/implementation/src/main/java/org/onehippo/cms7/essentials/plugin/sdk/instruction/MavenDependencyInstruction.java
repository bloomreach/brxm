/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugin.sdk.instruction;

import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.service.MavenDependencyService;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.springframework.stereotype.Component;

/**
 * Built-in instruction for adding Maven dependencies to pom.xml files.
 */
@Component
@XmlRootElement(name = "mavenDependency", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class MavenDependencyInstruction extends BuiltinInstruction {

    private final MavenDependency dependency = new MavenDependency();
    private String targetPom;

    @Inject MavenDependencyService dependencyService;

    public MavenDependencyInstruction() {
        super(Type.EXECUTE);
    }

    public Status execute(final Map<String, Object> parameters) {
        final Module module = Module.pomForName(targetPom);
        return module != Module.INVALID && dependencyService.addDependency(module, dependency)
                ? Status.SUCCESS : Status.FAILED;
    }

    @Override
    void populateDefaultChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        final String message = String.format("Add Maven dependency %s:%s to module '%s'.",
                dependency.getGroupId(), dependency.getArtifactId(), targetPom);
        changeMessageQueue.accept(getDefaultGroup(), message);
    }

    @XmlAttribute
    public String getTargetPom() {
        return targetPom;
    }

    public void setTargetPom(final String targetPom) {
        this.targetPom = targetPom;
    }

    @XmlAttribute
    public String getGroupId() {
        return dependency.getGroupId();
    }

    public void setGroupId(final String groupId) {
        dependency.setGroupId(groupId);
    }

    @XmlAttribute
    public String getArtifactId() {
        return dependency.getArtifactId();
    }

    public void setArtifactId(final String artifactId) {
        dependency.setArtifactId(artifactId);
    }

    @XmlAttribute
    public String getVersion() {
        return dependency.getVersion();
    }

    public void setVersion(final String version) {
        dependency.setVersion(version);
    }

    @XmlAttribute
    public String getType() {
        return dependency.getType();
    }

    public void setType(final String type) {
        dependency.setType(type);
    }

    @XmlAttribute
    public String getScope() {
        return dependency.getScope();
    }

    public void setScope(final String scope) {
        dependency.setScope(scope);
    }
}
