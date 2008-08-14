/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.template;

/**
 * Instances of this class are used by the {@see ModuleFilter} to obtain the
 * module which execute() has to be executed from a list on the session.
 * The {@see ModuleRenderTag} adds a {@see ModuleRenderAttributes} instance to a
 * list on the session if the module wants to run it execute() method.
 *
 */
public class ModuleRenderAttributes {
	 private String pageName;
     private String className;
     private String containerName;
     private String moduleName;
     
     public ModuleRenderAttributes(String pageName, String containerName, String moduleName, String className) {
    	 this.pageName = pageName;
    	 this.containerName = containerName;
    	 this.moduleName = moduleName;
    	 this.className = className;
     }

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}
	
	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.getClassName()).append("[");
		sb.append("pageName").append("=").append(pageName);
		sb.append(", containerName").append("=").append(containerName);
		sb.append(", moduleName").append("=").append(moduleName);
		sb.append(", className").append("=").append(className).append("]");
		return sb.toString();
	}

	
}
