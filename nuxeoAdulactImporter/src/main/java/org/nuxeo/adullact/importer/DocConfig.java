package org.nuxeo.adullact.importer;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor that is used to define how DocumenModel should be created from XML
 * input
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@XObject("docConfig")
public class DocConfig {

    @XNode("@tagName")
    protected String tagName;

    @XNode("docType")
    protected String docType;

    @XNode("parent")
    protected String parent;

    @XNode("name")
    protected String name;

    public DocConfig() {
    }

    public DocConfig(String tagName, String docType, String parent, String name) {
        this.tagName = tagName;
        this.docType = docType;
        this.parent = parent;
        this.name = name;
    }

    public String getTagName() {
        return tagName;
    }

    public String getDocType() {
        return docType;
    }

    public String getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        String msg = "\nDocConfig:\n\tTag Name: %s\n\tDocType %s\n\tParent: %s\n\tName: %s\n";
        return String.format(msg, tagName, docType, parent, name);
    }

}
