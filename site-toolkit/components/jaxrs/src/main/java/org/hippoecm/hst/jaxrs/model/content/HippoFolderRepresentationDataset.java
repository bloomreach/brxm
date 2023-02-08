/*
 *  Copyright 2010-2023 Bloomreach
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
package org.hippoecm.hst.jaxrs.model.content;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * HippoFolderRepresentationDataset
 * @version $Id$
 */
@XmlRootElement(name = "dataset")
public class HippoFolderRepresentationDataset extends AbstractNodeRepresentationDataset {

    private static final long serialVersionUID = 1L;
    
    public HippoFolderRepresentationDataset() {
        super();
    }
    
    @XmlElementWrapper(name="folders")
    @XmlElements(@XmlElement(name="folder"))
    public List<HippoFolderRepresentation> getFolders() {
        return (List<HippoFolderRepresentation>) getNodeRepresentations();
    }
}
