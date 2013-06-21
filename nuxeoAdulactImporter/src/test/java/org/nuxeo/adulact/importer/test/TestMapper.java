package org.nuxeo.adulact.importer.test;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.adulact.importer.DepotParser;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestMapper {


    @Inject
    CoreSession session;

    @Test
    public void test() throws Exception {

        File xml = FileUtils.getResourceFileFromContext("depot.xml");
        Assert.assertNotNull(xml);

        DocumentModel root = session.getRootDocument();

        DepotParser parser = new DepotParser(root);

        parser.parse(xml);

        session.save();

        List<DocumentModel> docs = session.query("select * from Document order by ecm:path");
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

}
