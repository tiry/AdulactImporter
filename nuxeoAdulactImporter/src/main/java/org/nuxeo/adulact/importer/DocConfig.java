package org.nuxeo.adulact.importer;

public class DocConfig {

    protected String tagName;

    protected String docType;

    protected String parent;

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
