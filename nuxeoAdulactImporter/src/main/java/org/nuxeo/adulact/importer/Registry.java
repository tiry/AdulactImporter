package org.nuxeo.adulact.importer;

import java.util.ArrayList;
import java.util.List;

public class Registry {

    protected List<AttributeConfig> attConfig = null;

    protected List<DocConfig> docConfig = null;

    public List<AttributeConfig> getAttributConfigs() {
        if (attConfig == null) {
            attConfig = new ArrayList<AttributeConfig>();

            attConfig.add(new AttributeConfig("titre", "dc:title", "text()", null)); // use xpath
            attConfig.add(new AttributeConfig("dossierActe", "dc:source", "#{'Seance ' + currentDocument.name}", null)); // MVEL

            //attConfig.add(new )

        }

        return attConfig;
    }

    public List<DocConfig> getDocCreationConfigs() {
        if (docConfig == null) {
            docConfig = new ArrayList<DocConfig>();
            docConfig.add(new DocConfig("seance", "Workspace", null, "@idSeance")); // pure xpath
            docConfig.add(new DocConfig("dossierActe", "Folder", "../seance", "Acte-{{@idActe}}")); // xpath resolution inside String
        }
        return docConfig;
    }
}
