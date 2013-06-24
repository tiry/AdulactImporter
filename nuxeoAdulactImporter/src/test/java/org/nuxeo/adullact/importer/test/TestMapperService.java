package org.nuxeo.adullact.importer.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.adullact.importer.XmlImporterSevice;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.adullact.importer")
@LocalDeploy("org.nuxeo.adullact.importer:test-ImporterMapping-contrib.xml")
public class TestMapperService {

    @Inject
    CoreSession session;

    @Inject
    XmlImporterSevice importerService;

    @Test
    public void test() throws Exception {

        File xml = FileUtils.getResourceFileFromContext("depot.xml");
        Assert.assertNotNull(xml);

        DocumentModel root = session.getRootDocument();

        XmlImporterSevice importer = Framework.getLocalService(XmlImporterSevice.class);
        Assert.assertNotNull(importer);
        importer.importDocuments(root, xml);

        session.save();

        List<DocumentModel> docs = session.query("select * from Workspace");
        Assert.assertEquals("we should have only one Seance", 1, docs.size());
        DocumentModel seanceDoc  = docs.get(0);

        docs = session.query("select * from Document where ecm:primaryType='Folder'");
        Assert.assertEquals("we should have 4 actes",4, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='File'");
        Assert.assertEquals("we should have 13 files",13, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder' AND ecm:parentId='" + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have 3 actes in the seance",3, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder'  AND ecm:parentId!='" + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 1 actes ouside of the seance",1, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='File'  AND ecm:parentId='" + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 4 files in the seance",4, docs.size());

        docs = session.query("select * from Document order by ecm:path");
        for (DocumentModel doc : docs) {
            if(!doc.getId().equals(root.getId())) {
                System.out.println("> [" +  doc.getType() + "] " + doc.getPathAsString() + " : " + " - title: '" + doc.getTitle() + "', dc:source: '" + doc.getPropertyValue("dc:source"));
                BlobHolder bh = doc.getAdapter(BlobHolder.class);
                if (bh!=null) {
                    Blob blob = bh.getBlob();
                    if (blob!=null) {
                        System.out.println(" ------ > File " + blob.getFilename() + " " + blob.getMimeType() + " " + blob.getLength());
                    }
                }
            }
        }
    }


    @Test
    public void testNXP11834() throws Exception {

        File xml = FileUtils.getResourceFileFromContext("NXP-11834.xml");
        Assert.assertNotNull(xml);

        DocumentModel root = session.getRootDocument();

        XmlImporterSevice importer = Framework.getLocalService(XmlImporterSevice.class);
        Assert.assertNotNull(importer);
        try {
            importer.importDocuments(root, xml);
        } catch (ClassCastException e) {
            fail("See NXP-11834 ticket");

        }
    }

}
