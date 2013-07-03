/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.site.util;

import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.ModuleImpl;
import com.bc.ceres.core.runtime.internal.ModuleManifestParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.bc.ceres.site.util.ExclusionListBuilder.*;
import static com.bc.ceres.site.util.ModuleUtils.*;
import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

public class ModuleUtilsTest {

    private List<Module> modules = new ArrayList<Module>();
    public static final String PLUGINS_LIST_CSV = "plugins_list.csv";

    @Before
    public void setUp() throws Exception {
        ModuleImpl module1 = generateModule("test_excluded_module.xml");
        ModuleImpl module2 = generateModule("test_glayer_module.xml");
        ModuleImpl module3 = generateModule("test_jai_module.xml");

        modules.add(module1);
        modules.add(module2);
        modules.add(module3);
    }

    @After
    public void tearDown() {
        final File exclusionList = new File(ExclusionListBuilder.EXCLUSION_LIST_FILENAME);
        if (exclusionList.exists()) {
            exclusionList.delete();
        }
    }

    @Test
    public void testFileBasedPomParsing() throws Exception {
        final List<URL> poms = new ArrayList<URL>();
        poms.add(getClass().getResource("test_pom.xml"));
        File exclusionList = generateFileBasedTestInclusionFile();
        ExclusionListBuilder.generateExclusionList(exclusionList, poms);

        CsvReader csvReader = new CsvReader(new FileReader(exclusionList), new char[]{','});
        try {
            final String[] firstChunkOfModules = csvReader.readRecord();

            assertEquals(true, Arrays.asList(firstChunkOfModules).contains("ceres-launcher"));
            assertEquals(false, Arrays.asList(firstChunkOfModules).contains("beam-download-portlet"));

            int numberOfModules = firstChunkOfModules.length;
            assertEquals(9, numberOfModules);

            final URL pom2 = getClass().getResource("test_pom2.xml");
            ExclusionListBuilder.addPomToExclusionList(exclusionList, pom2);
        } finally {
            csvReader.close();
        }

        csvReader = new CsvReader(new FileReader(exclusionList), new char[]{ExclusionListBuilder.CSV_SEPARATOR});
        try {
            final String[] secondChunkOfModules = csvReader.readRecord();
            int newNumberOfModules = secondChunkOfModules.length;

            assertEquals(true, Arrays.asList(secondChunkOfModules).contains("ceres-launcher"));
            assertEquals(true, Arrays.asList(secondChunkOfModules).contains("beam-download-portlet"));
            assertEquals(12, newNumberOfModules);
        } finally {
            csvReader.close();
        }
    }

    @Test
    public void testIsIncluded() throws Exception {
        final List<URL> pomList = ExclusionListBuilder.retrievePoms(ExclusionListBuilder.POM_LIST_FILENAME);
        final File exclusionList = new File(ExclusionListBuilder.EXCLUSION_LIST_FILENAME);
        ExclusionListBuilder.generateExclusionList(exclusionList, pomList);

        final CsvReader csvReader = new CsvReader(new FileReader(exclusionList), CSV_SEPARATOR_ARRAY);
        try {
            final String[] excludedModules = csvReader.readRecord();

            assertEquals(false, ModuleUtils.isExcluded(modules.get(0), excludedModules));
            assertEquals(true, ModuleUtils.isExcluded(modules.get(1), excludedModules));
            assertEquals(true, ModuleUtils.isExcluded(modules.get(2), excludedModules));
        } finally {
            csvReader.close();
        }
    }

    @Test
    public void testFileBasedIsIncluded() throws Exception {

        String moduleString = "some-beam-module,ceres-glayer, ceres-jai,ceres-main-module";
        final CsvReader csvReader = new CsvReader(new StringReader(moduleString), CSV_SEPARATOR_ARRAY);
        try {
            final String[] excludedModules = csvReader.readRecord();

            assertEquals(false, ModuleUtils.isExcluded(modules.get(0), excludedModules));
            assertEquals(true, ModuleUtils.isExcluded(modules.get(1), excludedModules));
            assertEquals(true, ModuleUtils.isExcluded(modules.get(2), excludedModules));
        } finally {
            csvReader.close();
        }
    }

    @Test
    public void testRemoveDoubles() throws Exception {
        Module[] moduleArray = modules.toArray(new Module[modules.size()]);
        Module[] resultModuleArray = ModuleUtils.removeDoubles(moduleArray);

        assertEquals(3, moduleArray.length);
        assertEquals(moduleArray.length, resultModuleArray.length);

        moduleArray = addModule("test_jai_module2.xml");
        assertEquals(4, moduleArray.length);

        resultModuleArray = ModuleUtils.removeDoubles(moduleArray);
        assertEquals(moduleArray.length - 1, resultModuleArray.length);
    }

    @Test
    public void testParseYearFromCopyrightString() throws Exception {
        assertThat(parseYearFromCopyrightString(null), is("-1"));
        assertThat(parseYearFromCopyrightString("Copyright (c) 2012 by Brockmann Consult GmbH"), is("2012"));
        assertThat(parseYearFromCopyrightString("(c) Copyright 1999 by Brockmann Consult GmbH"), is("1999"));
        assertThat(parseYearFromCopyrightString("(c) Copyright 2004 by NASA"), is("2004"));
        assertThat(parseYearFromCopyrightString("(c) 1756 Copyright by NASA"), is("1756"));
        assertThat(parseYearFromCopyrightString("(c) Copyright 3084 by FUB"), is("3084"));
        assertThat(parseYearFromCopyrightString("(c) Copyright 2009-2013 by FUB"), is("2009-2013"));
        assertThat(parseYearFromCopyrightString("(c) Copyright 2009 - 2013 by FUB"), is("2009-2013"));
        assertThat(parseYearFromCopyrightString("(c) Copyright 2008 BC GmbH"), is("2008"));
    }

    private Module[] addModule(String name) throws Exception {
        Module[] moduleArray;
        modules.add(generateModule(name));
        moduleArray = modules.toArray(new Module[modules.size()]);
        return moduleArray;
    }

    private ModuleImpl generateModule(String resource) throws Exception {
        final URI uri = getClass().getResource(resource).toURI();
        String xml = uri.getPath();
        ModuleImpl module;
        FileReader fileReader = new FileReader(xml);
        try {
            module = new ModuleManifestParser().parse(fileReader);
        } finally {
            fileReader.close();
        }
        module.setLocation(uri.toURL());
        return module;
    }

    private File generateFileBasedTestInclusionFile() throws IOException {
        final String someResource = getClass().getResource("test_pom.xml").getFile();
        final String resourceDir = new File(someResource).getParent();
        final File inclusionList = new File(resourceDir + File.separator + PLUGINS_LIST_CSV);
        inclusionList.delete();
        inclusionList.createNewFile();
        return inclusionList;
    }
}
