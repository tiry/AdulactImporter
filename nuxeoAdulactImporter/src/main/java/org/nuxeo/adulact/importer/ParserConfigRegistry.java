package org.nuxeo.adulact.importer;

import java.util.List;

public interface ParserConfigRegistry {

    public abstract List<AttributeConfig> getAttributConfigs();

    public abstract List<DocConfig> getDocCreationConfigs();

}