/*
 * (C) Copyright 2002-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.adullact.importer.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.adullact.importer.XmlImporterSevice;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * Verify Service mapping
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
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
        Assert.assertEquals("we should have only one Seance and one ", 1, docs.size());
        DocumentModel seanceDoc  = docs.get(0);

        docs = session.query("select * from Document where ecm:primaryType='Folder'");
        Assert.assertEquals("we should have 4 actes",4, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='File'");
        Assert.assertEquals("we should have 12 files",12, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Section'");
        Assert.assertEquals("we should have 1 Section",1, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder' AND ecm:parentId='" + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have 3 actes in the seance",3, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder'  AND ecm:parentId!='" + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 1 actes outside of the seance",1, docs.size());

        docs = session.query("select * from Document where ecm:primaryType in ('File', 'Section')  AND ecm:parentId='" + seanceDoc.getId() + "'");
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

    @Test
    public void testZip() throws Exception {

        File zipXml = FileUtils.getResourceFileFromContext("nuxeo-webdelib-export.zip");
        Assert.assertNotNull(zipXml);

        DocumentModel root = session.getRootDocument();

        XmlImporterSevice importer = Framework.getLocalService(XmlImporterSevice.class);
        Assert.assertNotNull(importer);
        importer.importDocuments(root, zipXml);

        session.save();

        List<DocumentModel> docs = session.query("select * from Workspace");
        Assert.assertEquals("we should have only one Seance and one ", 1, docs.size());
        DocumentModel seanceDoc  = docs.get(0);

        docs = session.query("select * from Document where ecm:primaryType='Folder'");
        Assert.assertEquals("we should have 5 actes",5, docs.size());

        docs = session.query("select * from Document where ecm:primaryType in ('File','Section')");
        Assert.assertEquals("we should have 18 files",18, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder' AND ecm:parentId='" + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have 4 actes in the seance",4, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder'  AND ecm:parentId!='" + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 1 actes outside of the seance",1, docs.size());

        docs = session.query("select * from Document where ecm:primaryType in ('File', 'Section')  AND ecm:parentId='" + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 4 files in the seance",4, docs.size());

        IterableQueryResult result = session.queryAndFetch("select content/name from File", "NXQL", null);

        for ( Iterator<Map<String, Serializable>> rows = result.iterator(); rows.hasNext(); ) {
            String filename = (String)rows.next().values().iterator().next();
            Assert.assertTrue(filename.endsWith(".pdf") || filename.endsWith(".zip")|| filename.endsWith(".pdf2"));
        }
        docs = session.query("select * from Document where ecm:primaryType in ('File')");
        for (DocumentModel fileDoc : docs) {
            Blob blob = fileDoc.getAdapter(BlobHolder.class).getBlob();
            Assert.assertNotNull(blob);
            Assert.assertNotNull(blob.getFilename());
            Assert.assertNotNull(blob.getMimeType());
            Assert.assertTrue(blob.getFilename().endsWith(".pdf2")  || blob.getLength()>1000);
        }

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

}
