package org.nuxeo.adulact.importer;

import java.util.HashMap;
import java.util.Map;

public class TagConfig {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String PARENT = "parent";

    protected String tagName;

    protected String xpathMatcher = null;

    protected String docType;

    protected Map<String, String> docAttributes = new HashMap<String, String>();

    protected String targetDocAttribute= null;

    public TagConfig(String tagName, String docType, Map<String, String> docAttributes, String targetDocAttribute) {
        this.tagName=tagName;
        this.docType = docType;
        this.docAttributes = docAttributes;
        this.targetDocAttribute = targetDocAttribute;
    }

    public String getTagName() {
        return tagName;
    }

    public String getXpathMatcher() {
        return xpathMatcher;
    }

    public boolean isCreateDoc() {
        return docType!=null;
    }

    public Map<String, String> getDocAttributes() {
        return docAttributes;
    }

    public String getTargetDocAttribute() {
        return targetDocAttribute;
    }

    public String getDocType() {
        return docType;
    }


}
