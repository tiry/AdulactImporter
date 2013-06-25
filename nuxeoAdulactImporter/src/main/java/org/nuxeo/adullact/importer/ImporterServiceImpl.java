package org.nuxeo.adullact.importer;

import java.io.File;
import java.io.InputStream;
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
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.schema.types.ListType;

/**
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class ImporterServiceImpl {

    private static final String MSG_NO_ELEMENT_FOUND = "**CREATION**\nNo element \"%s\" found in %s, use the DOC_TYPE-INDEX value";

    private static final String MSG_CREATION = "**CREATION**\nTry to create document in %s with name %s based on \"%s\" fragment "
            + "with the following conf: %s\n";

    private static final String MSG_UPDATE_PROPERTY = "**PROPERTY UPDATE**\nValue found for %s in %s is \"%s\". With the following conf: %s";

    public static final Log log = LogFactory.getLog(ImporterServiceImpl.class);

    protected CoreSession session;

    protected DocumentModel rootDoc;

    protected Stack<DocumentModel> docsStack;

    protected Map<String, Object> mvelCtx = new HashMap<String, Object>();

    protected Map<Element, DocumentModel> elToDoc = new HashMap<Element, DocumentModel>();

    protected ParserConfigRegistry registry;

    public ImporterServiceImpl(DocumentModel rootDoc,
            ParserConfigRegistry registry) {
        session = rootDoc.getCoreSession();
        this.rootDoc = rootDoc;
        docsStack = new Stack<DocumentModel>();
        docsStack.add(rootDoc);
        mvelCtx.put("root", rootDoc);
        mvelCtx.put("docs", docsStack);
        mvelCtx.put("session", session);

        this.registry = registry;
    }

    protected ParserConfigRegistry getRegistry() {
        return registry;
    }

    protected DocConfig getDocCreationConfig(Element el) {

        for (DocConfig conf : getRegistry().getDocCreationConfigs()) {
            // direct tagName match
            if (conf.getTagName().equals(el.getName())) {
                return conf;
            } else {
                // try xpath match
                try {
                    if (el.matches(conf.getTagName())) {
                        return conf;
                    }
                } catch (Exception e) {
                    // NOP
                }
            }
        }
        return null;
    }

    protected List<AttributeConfig> getAttributConfigs(Element el) {

        List<AttributeConfig> result = new ArrayList<AttributeConfig>();

        for (AttributeConfig conf : getRegistry().getAttributConfigs()) {
            if (conf.getTagName().equals(el.getName())) {
                result.add(conf);
            } else {
             // try xpath match
                try {
                    if (el.matches(conf.getTagName())) {
                        result.add(conf);
                    }
                } catch (Exception e) {
                    // NOP
                }
            }
        }
        return result;
    }

    protected File workingDirectory;

    public List<DocumentModel> parse(InputStream is) throws Exception {
        Document doc = new SAXReader().read(is);
        workingDirectory = null;
        return parse(doc);
    }

    public List<DocumentModel> parse(File file) throws Exception {

        Document doc= null;
        try {
            doc = new SAXReader().read(file);
            workingDirectory = file.getParentFile();
        } catch (Exception e) {
            File tmp = new File(System.getProperty("java.io.tmpdir"));
            File directory = new File(tmp, file.getName()+ System.currentTimeMillis());
            directory.mkdir();
            ZipUtils.unzip(file, directory);
            for (File child : directory.listFiles()) {
                if (child.getName().endsWith(".xml")) {
                    return parse(child);
                }
            }
            throw new ClientException("Can not find XML file inside the zip archive");
        }
        return parse(doc);
    }

    public List<DocumentModel> parse(Document doc) throws Exception {
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
            propValue.put(name,
                    resolveAndEvaluateXmlNode(el, conf.getMapping().get(name)));
        }
        return propValue;
    }

    protected Blob resolveBlob(Element el, AttributeConfig conf) {

        @SuppressWarnings("unchecked")
        Map<String, Object> propValues = (Map<String, Object>) resolveComplex(
                el, conf);

        if (propValues.containsKey("content")) {
            Blob  blob = null;
            String content = (String) propValues.get("content");
            if (content!=null &&  workingDirectory!=null) {
                File file = new File(workingDirectory, content.trim());
                if (file.exists())  {
                    blob = new FileBlob(file);
                }
            }
            if (blob==null) {
                blob = new StringBlob((String) propValues.get("content"));
            }
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
            Object value = resolveAndEvaluateXmlNode(el, conf.getSingleXpath());
            if (log.isDebugEnabled()) {
                log.debug(String.format(MSG_UPDATE_PROPERTY, targetDocProperty,
                        el.getUniquePath(), value, conf.toString()));
            }
            property.setValue(value);

        } else if (property.isComplex()) {

            if (property instanceof BlobProperty) {
                Object value = resolveBlob(el, conf);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(MSG_UPDATE_PROPERTY,
                            targetDocProperty, el.getUniquePath(),
                            value, conf.toString()));
                }
                property.setValue(value);
            } else {
                Object value = resolveComplex(el, conf);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(MSG_UPDATE_PROPERTY,
                            targetDocProperty, el.getUniquePath(),
                            value, conf.toString()));
                }
                property.setValue(value);
            }



        } else if (property.isList()) {

            ListType lType = (ListType) property.getType();
            @SuppressWarnings("unchecked")
            List<Serializable> values = (List<Serializable>) property.getValue();
            if (values == null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(MSG_UPDATE_PROPERTY,
                            targetDocProperty, el.getUniquePath(),
                            "%NO_VALUE%", conf.toString()));
                }
                values = new ArrayList<Serializable>();
            }
            if (lType.getFieldType().isSimpleType()) {
                Serializable value = (Serializable) resolveAndEvaluateXmlNode(
                        el, conf.getSingleXpath());
                if (log.isDebugEnabled()) {
                    log.debug(String.format(MSG_UPDATE_PROPERTY,
                            targetDocProperty, el.getUniquePath(),
                            value, conf.toString()));
                }
                values.add(value);

            } else {
                Serializable value = (Serializable) resolveComplex(el, conf);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(MSG_UPDATE_PROPERTY,
                            targetDocProperty, el.getUniquePath(),
                            value, conf.toString()));
                }
                values.add(value);

            }
        }
    }

    protected Map<String, Object> getMVELContext(Element el) {
        mvelCtx.put("currentDocument", docsStack.peek());
        mvelCtx.put("currentElement", el);
        mvelCtx.put("Fn", new ImporterFunction(session, docsStack, elToDoc, el));
        return mvelCtx;
    }

    protected Object resolve(Element el, String xpr) {
        if (xpr == null) {
            return null;
        }

        if (xpr.startsWith("#{") && xpr.endsWith("}")) { // MVEL
            xpr = xpr.substring(2, xpr.length() - 1);
            return resolveMVEL(el, xpr);
        } else if (xpr.contains("{{")) { // String containing XPaths
            StringBuffer sb = new StringBuffer();
            int idx = xpr.indexOf("{{");
            while (idx >= 0) {
                int idx2 = xpr.indexOf("}}", idx);
                if (idx2 > 0) {
                    sb.append(xpr.substring(0, idx));
                    String xpath = xpr.substring(idx + 2, idx2);
                    sb.append(resolveAndEvaluateXmlNode(el, xpath));
                    xpr = xpr.substring(idx2);
                } else {
                    sb.append(xpr);
                    xpr = "";
                }
                idx = xpr.indexOf("{{");
            }
            return sb.toString();
        } else {
            return resolveXP(el, xpr); // default to pure XPATH
        }
    }

    protected Object resolveMVEL(Element el, String xpr) {
        Map<String, Object> ctx = new HashMap<String, Object>(
                getMVELContext(el));
        Serializable compiled = MVEL.compileExpression(xpr);
        return MVEL.executeExpression(compiled, ctx);
    }

    protected Object resolveXP(Element el, String xpr) {

        @SuppressWarnings("unchecked")
        List<Object> nodes = el.selectNodes(xpr);
        if (nodes.size() == 1) {
            return nodes.get(0);
        } else if (nodes.size() > 1) {
            // Workaround for NXP-11834
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
        Object ob = resolveAndEvaluateXmlNode(el, xpr);
        if (ob == null) {
            return null;
        }
        return ob.toString();
    }

    protected Object resolveAndEvaluateXmlNode(Element el, String xpr) {
        Object ob = resolve(el, xpr);
        if (ob == null) {
            return null;
        }
        if (ob instanceof Node) {
            return ((Node) ob).getText();
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
            if (log.isDebugEnabled()) {
                log.debug(String.format(MSG_NO_ELEMENT_FOUND, conf.getName(),
                        el.getUniquePath()));
            }
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

        if (log.isDebugEnabled()) {
            log.debug(String.format(MSG_CREATION, path, name,
                    el.getUniquePath(), conf.toString()));
        }

        try {
            doc = session.createDocument(doc);
        } catch (Exception e) {
            throw new Exception(String.format(MSG_CREATION, path, name,
                    el.getUniquePath(), conf.toString()), e);
        }
        docsStack.push(doc);
        elToDoc.put(el, doc);
    }

    protected void process(Element el) throws Exception {

        DocConfig createConf = getDocCreationConfig(el);
        if (createConf != null) {
            createNewDocument(el, createConf);
        }
        List<AttributeConfig> configs = getAttributConfigs(el);
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
