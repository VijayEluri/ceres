package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.ModuleImpl;
import com.bc.ceres.core.runtime.internal.ModuleManifestParser;
import com.bc.ceres.core.runtime.internal.RuntimeActivator;
import com.bc.ceres.core.runtime.Version;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import junit.framework.Assert;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.text.MessageFormat;

import org.junit.Ignore;

@Ignore
public class TestHelpers {

    public static Module newRepositoryModuleMock(String name, String version, ModuleState state) throws CoreException {
        return newModuleImpl(name, version, state);
    }

    public static ModuleItem newModuleItemMock(String name, String version, ModuleState state) throws CoreException {
        return new ModuleItem(newModuleImpl(name, version, state));
    }

    private static ModuleImpl newModuleImpl(String name, String version, ModuleState state) throws CoreException {
        ModuleImpl module = null;
        String resource = "xml/" + name + ".xml";
        try {
            module = loadModule(resource);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(resource + ": " + e.getMessage());
        }
        module.setVersion(Version.parseVersion(version));
        module.setState(state);
        return module;
    }

    public static ModuleImpl loadModule(String resource) throws IOException, CoreException {
        URL url = ModuleSyncRunnerTest.class.getResource(resource);
        if (url == null) {
            Assert.fail("resource not found: " + resource);
        }
        ModuleImpl module = new ModuleManifestParser().parse(url.openStream());
        module.setLocation(url);
        return module;
    }

    static ModuleManager createModuleManager(final String[] installedResourcePaths,
                                              final String[] repositoryResourcePaths) {
        ModuleContext moduleContext = new RuntimeActivator().getModuleContext();
        return new DefaultModuleManager(moduleContext) {
            private ModuleImpl[] installedModules;
            private Module[] repositoryModules;


            @Override
            public Module[] getInstalledModules() {
                if (installedModules == null) {
                    installedModules = createModules(installedResourcePaths);
                    for (ModuleImpl installedModule : installedModules) {
                        installedModule.setState(ModuleState.ACTIVE);
                    }
                }
                return installedModules;
            }

            @Override
            public Module[] getRepositoryModules(ProgressMonitor pm) throws CoreException {
                if (repositoryModules == null) {
                    repositoryModules = createModules(repositoryResourcePaths);
                }
                return repositoryModules;
            }


            private ModuleImpl[] createModules(String[] moduleResourcePaths) {
                ArrayList<ModuleImpl> moduleList = new ArrayList<ModuleImpl>(moduleResourcePaths.length);
                for (String resourcePath : moduleResourcePaths) {
                    moduleList.add(createModule(resourcePath));
                }
                return moduleList.toArray(new ModuleImpl[moduleList.size()]);
            }

            private ModuleImpl createModule(String moduleResourcePath) {
                ModuleImpl module = null;
                try {
                    module = loadModule(moduleResourcePath);
                } catch (Exception e) {
                    String msgPattern = "Not able to load module descriptor for [{0}] - {1}";
                    Assert.fail(MessageFormat.format(msgPattern, moduleResourcePath, e.getMessage()));
                }
                return module;
            }

        };
    }
}
