package org.nuxeo.adulact.importer;

import java.util.HashMap;
import java.util.Map;

public class AttributeConfig {

    protected String tagName;

    protected String targetDocProperty;

    // xpath to select when this config may be valid
    protected String filter;

    // mapping between Nuxeo property names and corresponding xpath to extract values
    protected Map<String,String> mapping;

    protected String xmlPath;

    public AttributeConfig() {}

    public AttributeConfig(String tagName,String targetDocProperty, Map<String,String> mapping, String filter ) {
        this.tagName = tagName;
        this.targetDocProperty = targetDocProperty;
        if (mapping==null) {
            mapping = new HashMap<String, String>();
        } else {
            this.mapping = mapping;
        }
        this.filter=filter;
    }

    public AttributeConfig(String tagName,String targetDocProperty, String xmlPath, String filter ) {
        this.tagName = tagName;
        this.targetDocProperty = targetDocProperty;
        this.xmlPath = xmlPath;
        this.filter=filter;
    }

    public String getTagName() {
        return tagName;
    }

    public String getTargetDocProperty() {
        return targetDocProperty;
    }

    public String getFilter() {
        return filter;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public String getSingleXpath() {
        if (xmlPath!=null) {
            return xmlPath;
        }
        if (mapping!=null) {
            return mapping.values().iterator().next();
        }
        return null;
    }
}
