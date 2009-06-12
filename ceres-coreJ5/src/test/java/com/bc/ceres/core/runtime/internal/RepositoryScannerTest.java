package com.bc.ceres.core.runtime.internal;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import junit.framework.TestCase;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.NullProgressMonitor;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ProxyConfig;

/**
 * Unit test for simple ModuleReader.
 */
public class RepositoryScannerTest
        extends TestCase {

    Logger NO_LOGGER = Logger.getAnonymousLogger();
    ProgressMonitor NO_PM = ProgressMonitor.NULL;
    ProxyConfig NO_PROXY = ProxyConfig.NULL;

    public void testNullArgConvention() throws IOException, CoreException {
        URL NO_URL = new File("").getAbsoluteFile().toURI().toURL();

        try {
            new RepositoryScanner(null, NO_URL, NO_PROXY);
            fail();
        } catch (NullPointerException e) {
        }

        try {
            new RepositoryScanner(NO_LOGGER, null, NO_PROXY);
            fail();
        } catch (NullPointerException e) {
        }
        try {
            new RepositoryScanner(NO_LOGGER, NO_URL, null);
            fail();
        } catch (NullPointerException e) {
        }
        try {
            new RepositoryScanner(NO_LOGGER, NO_URL, NO_PROXY).scan(null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    public void testRepository() throws IOException, CoreException {
        File repositoryDir = Config.getRepositoryDir();

        RepositoryScanner rs = new RepositoryScanner(NO_LOGGER, repositoryDir.toURI().toURL(), NO_PROXY);
        Module[] repositoryModules = rs.scan(NO_PM);
        assertEquals(5, repositoryModules.length);

        Module rm;
        rm = findModule(repositoryModules, "module-a");
        assertNotNull(rm);
        assertEquals(true, rm.getContentLength() > 0);
        assertEquals(true, rm.getLastModified() > 0);
        assertNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-b");
        assertNotNull(rm);
        assertEquals(true, rm.getContentLength() > 0);
        assertEquals(true, rm.getLastModified() > 0);
        assertNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-c");
        assertNotNull(rm);
        assertEquals(true, rm.getContentLength() > 0);
        assertEquals(true, rm.getLastModified() > 0);
        assertNotNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-d");
        assertNotNull(rm);
        assertEquals(true, rm.getContentLength() > 0);
        assertEquals(true, rm.getLastModified() > 0);
        assertNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-e");
        assertNotNull(rm);
        assertEquals(true, rm.getContentLength() > 0);
        assertEquals(true, rm.getLastModified() > 0);
        assertNotNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-f");
        assertNull(rm);

        rm = findModule(repositoryModules, "module-g");
        assertNull(rm);
    }

    private Module findModule(Module[] modules, String name) {
        for (Module module : modules) {
            if (name.equals(module.getSymbolicName())) {
                return module;
            }
        }
        return null;
    }

}
