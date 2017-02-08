package org.onehippo.cm.migration;

import java.io.FileInputStream;
import java.io.IOException;

import javax.jcr.PropertyType;

import org.apache.commons.io.FilenameUtils;

public class SourceInitializeInstruction extends InitializeInstruction {

    private EsvNode sourceNode;

    public SourceInitializeInstruction(final EsvNode instructionNode, final Type type,
                                       final InitializeInstruction combinedWith) throws EsvParseException {
        super(instructionNode, type, combinedWith);
    }

    public EsvNode getSourceNode() {
        return sourceNode;
    }

    public void prepareSource(final EsvParser parser) throws IOException, EsvParseException {
        prepareResource(parser.getBaseDir(), true);
        setSourcePath(FilenameUtils.removeExtension(getResourcePath()) + ".yaml");
        sourceNode = parser.parse(new FileInputStream(getResource()), getResource().getCanonicalPath());
        final String contentRoot = normalizePath(getPropertyValue("hippo:contentroot", PropertyType.STRING, true));
        setContentPath(contentRoot + "/" + sourceNode.getName());
    }

    public boolean isDeltaMerge() {
        return sourceNode != null && sourceNode.isDeltaMerge();
    }

    public boolean isDeltaSkip() {
        return sourceNode != null && sourceNode.isDeltaSkip();
    }
}
