package org.nuxeo.adulact.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Registry {

    protected List<AttributeConfig> attConfig = null;

    protected List<DocConfig> docConfig = null;

    public List<AttributeConfig> getAttributConfigs() {
        if (attConfig == null) {
            attConfig = new ArrayList<AttributeConfig>();

            attConfig.add(new AttributeConfig("titre", "dc:title", "text()", null)); // use xpath
            attConfig.add(new AttributeConfig("dossierActe", "dc:source", "#{'Seance ' + currentDocument.name}", null)); // MVEL

            attConfig.add(new AttributeConfig("document", "dc:title", "@nom", null));
            attConfig.add(new AttributeConfig("document", "dc:source", "@type", null));

            attConfig.add(new AttributeConfig("signature", "dc:format", "@formatSignature", null));

            Map<String, String> complex = new HashMap<String, String>();
            complex.put("filename", "@nom");
            complex.put("mimetype", "mimetype/text()");
            complex.put("content", "@nom");

            attConfig.add(new AttributeConfig("document", "file:content", complex, null));

        }

        return attConfig;
    }

    public List<DocConfig> getDocCreationConfigs() {
        if (docConfig == null) {
            docConfig = new ArrayList<DocConfig>();
            docConfig.add(new DocConfig("seance", "Workspace", null, "@idSeance")); // pure xpath

            String findParent = "#{" +
                    "nodes = currentElement.selectNodes('@refSeance');" +
                    "if (nodes.size()>0) {" +
                    "  String seanceRef = nodes.get(0).getText();" +
                    "  String parentRef = '//seance[@idSeance=\"' + seanceRef + '\"]';" +
                    "  return xml.selectNodes(parentRef).get(0);" +
                    " } else {" +
                    "  return root.getPathAsString();" +
                    " }" +
                    "}";

            // xpath resolution inside String + complex MVEL Parent resolution
            docConfig.add(new DocConfig("dossierActe", "Folder", findParent, "Acte-{{@idActe}}"));
            docConfig.add(new DocConfig("document", "File", "..", "@nom"));
        }
        return docConfig;
    }
}
