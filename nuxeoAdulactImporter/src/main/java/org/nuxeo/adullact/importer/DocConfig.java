package org.nuxeo.adullact.importer;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

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

    public DocConfig() {}

    public DocConfig(String tagName, String docType, String parent, String name) {
        this.tagName=tagName;
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





}
