/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import java.io.Serializable;

/**
 * A Blueprint is provided by the developers to create and manage channels by the {@link ChannelManager},
 * and is defined by the node type <code>hst:blueprint</code>. Please see the <code>hst.cnd</code> for the node type definition
 */
@SuppressWarnings("serial")
public class Blueprint implements Serializable {

	private boolean hasContentPrototype;
	private String id;
    private String name;
    private String description;
    private String contentRoot;
    private Channel prototypeChannel;
    private String path;

    /**
     * Get {@link Blueprint} id
     *
     * @return The {@link Blueprint} <code>id</code>
     */
	public String getId() {
		return id;
	}

	/**
	 * Set {@link Blueprint} id
	 * 
	 * @param id - The {@link Blueprint} <code>id</code>
	 */
	public void setId(String id) {
		this.id = id;
	}

    /**
     * Get the name of the {@link Blueprint} as provided in the property <code>hst:name</code>, if the property doesn't
     * exist, the <code>id</code> (node name) is returned as the name
     *
     * @return The name of the {@link Blueprint}
     */
	public String getName() {
		return name;
	}

	/**
	 * Set the {@link Blueprint} name
	 * 
	 * @param name - The {@link Blueprint} name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

    /**
     * Get value of <code>hst:description</code> of the {@link Blueprint} if available, <code>null</code> otherwise
     * 
     * @return The {@link Blueprint} description
     */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the {@link Blueprint} description
	 * 
	 * @param description The {@link Blueprint} description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

    /**
     * Get the {@link Blueprint} content root must start with a <code>/</code> and be a valid JCR path
     * 
     * @return Absolute path of the <code>hst:contentRoot</code> of the {@link Blueprint} if available, <code>null</code> otherwise
     */
	public String getContentRoot() {
		return contentRoot;
	}

	/**
	 * Set {@link Blueprint} content root
	 * 
	 * @param contentRoot - The {@link Blueprint} content root to set
	 */
	public void setContentRoot(String contentRoot) {
		this.contentRoot = contentRoot;
	}

    /**
     * Does the {@link Blueprint} have a content prototype.  If so, it will be used to create a content structure
     *
     * @return <code>true</code> if a prototype exists, <code>false</code> otherwise
     */
	public boolean hasContentPrototype() {
		return hasContentPrototype;
	}

	/**
	 * Set whether a {@link Blueprint} has content prototype or not
	 * 
	 * @param hasContentPrototype - The {@link Blueprint} <code>hasContentPrototype</code> flag value to set
	 */
	public void setHasContentPrototype(boolean hasContentPrototype) {
		this.hasContentPrototype = hasContentPrototype;
	}

    /**
     * Get prototype {@link Channel}
     * 
     * @return The prototype {@link Channel}
     */
    public Channel getPrototypeChannel() {
        return prototypeChannel;
    }

    /**
     * Set the prototype {@link Channel}
     * 
     * @param prototypeChannel - The prototype {@link Channel} to set
     */
    public void setPrototypeChannel(Channel prototypeChannel) {
        this.prototypeChannel = prototypeChannel;
    }

    /**
     * Get the normalized absolute path
     * 
     * @return The normalized absolute path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the normalized absolute path
     * 
     * @param path - The normalized absolute path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

}
