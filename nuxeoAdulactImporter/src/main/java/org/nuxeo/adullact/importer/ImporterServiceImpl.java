package org.nuxeo.adullact.importer;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Text;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultText;
import org.mvel2.MVEL;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.schema.types.ListType;

public class ImporterServiceImpl {

    public static final Log log = LogFactory.getLog(ImporterServiceImpl.class);

    protected CoreSession session;

    protected DocumentModel rootDoc;

    protected Stack<DocumentModel> docsStack;

    protected Map<String, Object> mvelCtx = new HashMap<String, Object>();

    protected Map<Element, DocumentModel> elToDoc = new HashMap<Element, DocumentModel>();

    protected ParserConfigRegistry registry;

    public ImporterServiceImpl(DocumentModel rootDoc, ParserConfigRegistry registry) {
        session = rootDoc.getCoreSession();
        this.rootDoc = rootDoc;
        docsStack = new Stack<DocumentModel>();
        docsStack.add(rootDoc);
        mvelCtx.put("root", rootDoc);
        mvelCtx.put("docs", docsStack);
        this.registry = registry;
    }

    protected ParserConfigRegistry getRegistry() {
        return registry;
    }

    protected DocConfig getDocCreationConfig(String tagName) {

        for (DocConfig conf : getRegistry().getDocCreationConfigs()) {
            if (conf.getTagName().equals(tagName)) {
                return conf;
            }
        }
        return null;
    }

    protected List<AttributeConfig> getAttributConfigs(String tagName) {

        List<AttributeConfig> result = new ArrayList<AttributeConfig>();

        for (AttributeConfig conf : getRegistry().getAttributConfigs()) {
            if (conf.getTagName().equals(tagName)) {
                result.add(conf);
            }
        }
        return result;
    }

    public List<DocumentModel> parse(File file) throws Exception {
        Document doc = new SAXReader().read(file);
        Element root = doc.getRootElement();
        elToDoc = new HashMap<Element, DocumentModel>();
        mvelCtx.put("xml", doc);
        mvelCtx.put("map", elToDoc);
        process(root);
        return new ArrayList<DocumentModel>(docsStack);
    }

    protected Object resolveComplex(Element el, AttributeConfig conf) {

        Map<String, Object> propValue = new HashMap<String, Object>();
        for (String name : conf.getMapping().keySet()) {
            propValue.put(name, resolveAndEvaluateXmlNode(el, conf.getMapping().get(name)));
        }
        return propValue;
    }

    protected Blob resolveBlob(Element el, AttributeConfig conf) {

        Map<String, Object> propValues = (Map<String, Object>) resolveComplex(el, conf);

        if (propValues.containsKey("content")) {
            StringBlob blob = new StringBlob((String) propValues.get("content"));
            if (propValues.containsKey("mimetype")) {
                blob.setMimeType((String) propValues.get("mimetype"));
            }
            if (propValues.containsKey("filename")) {
                blob.setFilename((String) propValues.get("filename"));
            }
            return blob;
        }
        return null;
    }

    protected void processDocAttributes(DocumentModel doc, Element el,
            AttributeConfig conf) throws Exception {

        String targetDocProperty = conf.getTargetDocProperty();
        Property property = doc.getProperty(targetDocProperty);

        if (property.isScalar()) {

            property.setValue(resolveAndEvaluateXmlNode(el, conf.getSingleXpath()));

        } else if (property.isComplex()) {

            if (property instanceof BlobProperty) {
                property.setValue(resolveBlob(el, conf));
            } else {
                property.setValue(resolveComplex(el, conf));
            }



        } else if (property.isList()) {

            ListType lType = (ListType) property.getType();
            List<Serializable> values = (List<Serializable>) property.getValue();
            if (values == null) {
                values = new ArrayList<Serializable>();
            }
            if (lType.getFieldType().isSimpleType()) {

                values.add((Serializable) resolveAndEvaluateXmlNode(el, conf.getSingleXpath()));

            } else {

                values.add((Serializable) resolveComplex(el, conf));

            }
        }
    }

    protected Map<String, Object> getMVELContext(Element el) {
        mvelCtx.put("currentDocument", docsStack.peek());
        mvelCtx.put("currentElement", el);
        return mvelCtx;
    }

    protected Object resolve(Element el, String xpr) {
        if (xpr==null) {
            return null;
        }

        if (xpr.startsWith("#{") && xpr.endsWith("}")) { // MVEL
            xpr = xpr.substring(2, xpr.length() - 1);
            return resolveMVEL(el, xpr);
        } else if (xpr.contains("{{")) { // String containing XPaths
            StringBuffer sb = new StringBuffer();
            int idx =  xpr.indexOf("{{");
            while (idx >=0) {
                int idx2 = xpr.indexOf("}}", idx);
                String path =null;
                if (idx2 > 0) {
                    sb.append(xpr.substring(0, idx));
                    String xpath = xpr.substring(idx+2, idx2);
                    sb.append(resolveAndEvaluateXmlNode(el, xpath));
                    xpr = xpr.substring(idx2);
                } else {
                    sb.append(xpr);
                    xpr ="";
                }
                idx =  xpr.indexOf("{{");
            }
            return sb.toString();
        } else {
            return resolveXP(el, xpr); // default to pure XPATH
        }
    }

    protected Object resolveMVEL(Element el, String xpr) {
        Map<String, Object> ctx = new HashMap<String, Object>(getMVELContext(el));
        Serializable compiled = MVEL.compileExpression(xpr);
        return MVEL.executeExpression(compiled, ctx);
    }

    protected Object resolveXP(Element el, String xpr) {

        List<Object> nodes = el.selectNodes(xpr);
        if (nodes.size() == 1) {
            return nodes.get(0);
        } else if (nodes.size() > 1) {
            // NXP-11834
            if (xpr.endsWith("text()")) {
                String value = "";
                for (Object node : nodes) {
                    if (!(node instanceof DefaultText)) {
                        String msg = "Text selector must return a string (expr:\"%s\") element %s";
                        log.error(String.format(msg, xpr, el.getStringValue()));
                        return value;
                    }
                    value += ((DefaultText) node).getText();
                }
                return new DefaultText(value);
            }
            return nodes;
        }
        return null;
    }


    protected String resolvePath(Element el, String xpr) {
        Object ob = resolve(el, xpr);
        if (ob == null) {
            for (int i = 0; i < docsStack.size(); i++) {
                if (docsStack.get(i).isFolder()) {
                    return docsStack.get(i).getPathAsString();
                }
            }
        } else {
            if (ob instanceof DocumentModel) {
                return ((DocumentModel) ob).getPathAsString();
            } else if (ob instanceof Node) {
                if (ob instanceof Element) {
                    Element targetElement = (Element) ob;
                    DocumentModel target = elToDoc.get(targetElement);
                    if (target != null) {
                        return target.getPathAsString();
                    } else {
                        return targetElement.getText();
                    }
                } else if (ob instanceof Attribute) {
                    return ((Attribute) ob).getValue();
                } else if (ob instanceof Text) {
                    return ((Text) ob).getText();
                } else if (ob.getClass().isAssignableFrom(Attribute.class)) {
                    return ((Attribute) ob).getValue();
                }
            } else {
                return ob.toString();
            }
        }
        return rootDoc.getPathAsString();
    }

    protected String resolveName(Element el, String xpr) {
        Object ob = resolveAndEvaluateXmlNode(el,xpr);
        if (ob==null) {
            return null;
        }
        return ob.toString();
    }

    protected Object resolveAndEvaluateXmlNode(Element el, String xpr) {
        Object ob = resolve(el, xpr);
        System.out.println("Test");
        if (ob==null) {
            return null;
        }
        if (ob instanceof Node) {
            return ((Node)ob).getText();
        } else {
            return ob;
        }
    }

    protected void createNewDocument(Element el, DocConfig conf)
            throws Exception {
        DocumentModel doc = session.createDocumentModel(conf.getDocType());

        String path = resolvePath(el, conf.getParent());
        Object nameOb = resolveName(el, conf.getName());
        String name = null;
        if (nameOb == null) {
            int idx = 1;
            for (int i = 0; i < docsStack.size(); i++) {
                if (docsStack.get(i).getType().equals(conf.getDocType())) {
                    idx++;
                }
            }
            name = conf.getDocType() + "-" + idx;
        } else {
            name = nameOb.toString();
        }
        doc.setPathInfo(path, name);
        doc = session.createDocument(doc);
        docsStack.push(doc);
        elToDoc.put(el,  doc);
    }

    protected void process(Element el) throws Exception {

        DocConfig createConf = getDocCreationConfig(el.getName());
        if (createConf != null) {
            createNewDocument(el, createConf);
        }
        List<AttributeConfig> configs = getAttributConfigs(el.getName());
        if (configs != null) {
            for (AttributeConfig config : configs) {
                processDocAttributes(docsStack.peek(), el, config);
            }
            DocumentModel doc = docsStack.pop();
            doc = session.saveDocument(doc);
            docsStack.push(doc);
        }
        for (Object e : el.elements()) {
            process((Element) e);
        }
    }

}
