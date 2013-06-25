package org.nuxeo.adullact.importer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.dom4j.Element;
import org.nuxeo.ecm.automation.core.scripting.CoreFunctions;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public class ImporterFunction extends CoreFunctions {

    protected final CoreSession session;

    protected final Stack<DocumentModel> docsStack;

    protected final Map<Element, DocumentModel> elToDoc;

    protected final Element el;

    public ImporterFunction(CoreSession session, Stack<DocumentModel> docsStack, Map<Element, DocumentModel> elToDoc, Element el) {
        super();
        this.session = session;
        this.docsStack=docsStack;
        this.elToDoc=elToDoc;
        this.el=el;
    }

    public Calendar parseDate(String source, String format) throws Exception {
        DateFormat df = new SimpleDateFormat(format);
        Date date = df.parse(source);
        Calendar result = Calendar.getInstance();
        result.setTime(date);
        return result;
    }

    public DocumentModel mkdir(DocumentModel parent, String regexp, String data, String typeName) throws ClientException {

        String[] parts = data.split(regexp);
        List<DocumentModel> result = new ArrayList<DocumentModel>();
        DocumentModel root = parent;

        for (String part : parts) {
            DocumentModel child = null;
            try {
                child = session.getChild(root.getRef(), part);
            } catch (Exception e) {
                child = session.createDocumentModel(root.getPathAsString(),part, typeName);
                child.setPropertyValue("dc:title", part);
                child = session.createDocument(child);
            }
            result.add(child);
            docsStack.push(child);
            root = child;
        }

        if (result.size()>0) {
            elToDoc.put(el, result.get(result.size()-1));
            return result.get(result.size()-1);
        }
        return null;
    }


}
