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

package org.nuxeo.adullact.importer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Main Nuxeo Runtime component managing extension points and exposing
 * {@link XmlImporterSevice}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class ImporterComponent extends DefaultComponent implements
        XmlImporterSevice {

    protected List<DocConfig> docConfigs = new ArrayList<DocConfig>();

    protected List<AttributeConfig> attributeConfigs = new ArrayList<AttributeConfig>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("documentMapping".equals(extensionPoint)) {
            docConfigs.add((DocConfig) contribution);
        } else if ("attributeMapping".equals(extensionPoint)) {
            attributeConfigs.add((AttributeConfig) contribution);
        }
    }

    protected ParserConfigRegistry getRegistry() {
        return new ParserConfigRegistry() {

            @Override
            public List<DocConfig> getDocCreationConfigs() {
                return docConfigs;
            }

            @Override
            public List<AttributeConfig> getAttributConfigs() {
                return attributeConfigs;
            }
        };
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root, File xmlFile)
            throws Exception {
        ImporterServiceImpl importer = new ImporterServiceImpl(root,
                getRegistry());
        return importer.parse(xmlFile);
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root,
            InputStream xmlStream) throws Exception {
        ImporterServiceImpl importer = new ImporterServiceImpl(root,
                getRegistry());
        return importer.parse(xmlStream);
    }

}
