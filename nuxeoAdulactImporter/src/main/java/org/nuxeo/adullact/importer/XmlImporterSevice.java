package org.nuxeo.adullact.importer;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface XmlImporterSevice {

    public List<DocumentModel> importDocuments(DocumentModel root, File xmlFile) throws Exception;

    public List<DocumentModel> importDocuments(DocumentModel root, InputStream xmlStream) throws Exception;

}
