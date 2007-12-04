package com.bc.ceres.swing.update;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CanceledException;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.runtime.Dependency;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.Version;
import com.bc.ceres.core.runtime.internal.ModuleImpl;
import com.bc.ceres.swing.SwingHelper;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ModuleManagerPane extends JPanel {
    public static final Runnable NO_DONE_HANDLER = new Runnable() {
        public void run() {
        }
    };

    static final String TITLE = "Module Manager";

    private ModuleManager moduleManager;

    private SyncAction syncAction;
    private ClearAction clearAction;
    private InstallAction installAction;
    private UpdateAction updateAction;
    private UninstallAction uninstallAction;

    private JTable[] modulesTables;

    private ModuleItem[] installedModuleItems;
    private ModuleItem[] updatableModuleItems;
    private ModuleItem[] availableModuleItems;

    private ModuleTableModel installedModulesTableModel;
    private ModuleTableModel updatableModulesTableModel;
    private ModuleTableModel availableModulesTableModel;

    private boolean syncPerformed;

    private JComboBox categories;
    private Set<CaselessKey> categoriesSet;
    private InfoPane infoPane;
    private JTabbedPane tabbedPane;
    private boolean confirmed;

    private String repositoryTroubleShootingMessage;
    private SelectAllAction selectAllAction;

    public ModuleManagerPane() {
        this(new DefaultModuleManager());
    }

    public ModuleManagerPane(ModuleManager moduleManager) {
        super(new BorderLayout(2, 2));
        Assert.notNull(moduleManager, "moduleManager");

        this.moduleManager = moduleManager;

        this.installedModuleItems = toModuleItems(this.moduleManager.getInstalledModules());
        this.updatableModuleItems = new ModuleItem[0];
        this.availableModuleItems = new ModuleItem[0];

        this.syncAction = new SyncAction();
        this.clearAction = new ClearAction();
        this.installAction = new InstallAction();
        this.updateAction = new UpdateAction();
        this.uninstallAction = new UninstallAction();
        this.selectAllAction = new SelectAllAction();
        this.categoriesSet = new HashSet<CaselessKey>(20);

        repositoryTroubleShootingMessage = "";

        initUi();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getRepositoryTroubleShootingMessage() {
        return repositoryTroubleShootingMessage;
    }

    public void setRepositoryTroubleShootingMessage(String repositoryTroubleShootingMessage) {
        Assert.notNull(repositoryTroubleShootingMessage);
        this.repositoryTroubleShootingMessage = repositoryTroubleShootingMessage;
    }

    public boolean showDialog(final Window parent,
                              final String title,
                              final Runnable doneHandler,
                              final HelpHandler helpHandler) {
        Assert.notNull(parent, "parent");
        Assert.notNull(title, "title");
        final JDialog dialog = createDialog(parent, title, doneHandler, helpHandler);
        dialog.setSize(640, 512);
        SwingHelper.centerComponent(dialog, parent);
        dialog.setVisible(true);
        dialog.dispose();
        return isConfirmed();
    }

    public JDialog createDialog(final Window parent,
                                final String title,
                                final Runnable doneHandler,
                                final HelpHandler helpHandler) {
        Assert.notNull(parent, "parent");
        Assert.notNull(title, "title");
        final JDialog dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.setName("okButton");
        okButton.setMnemonic('O');
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOk(dialog, doneHandler);
            }
        });
        buttonPane.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        okButton.setName("cancelButton");
        cancelButton.setMnemonic('C');
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel(dialog, doneHandler);
            }
        });
        buttonPane.add(cancelButton);

        if (helpHandler != null) {
            JButton helpButton = new JButton("Help");
            cancelButton.setMnemonic('H');
            helpHandler.configureHelpButton(helpButton);
            helpButton.setName("helpButton");
            buttonPane.add(helpButton);
        }

        JPanel contentPane = new JPanel(new BorderLayout(4, 4));
        contentPane.add(this, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        contentPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        okButton.setDefaultCapable(true);
        //dialog.getRootPane().setDefaultButton(okButton);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel(dialog, doneHandler);
            }
        });
        dialog.setContentPane(contentPane);
        setConfirmed(false);
        return dialog;
    }

    private void onOk(JDialog dialog, Runnable doneHandler) {
        if (performUpdateActions(doneHandler)) {
            setConfirmed(true);
            dialog.setVisible(false);
        } else {
            setConfirmed(false);
            doneHandler.run();
        }
    }

    private void onCancel(JDialog dialog, Runnable doneHandler) {
        setConfirmed(false);
        dialog.setVisible(false);
        doneHandler.run();
    }


    private void initUi() {

        installedModulesTableModel = new ModuleTableModel(installedModuleItems,
                                                          new String[]{
                                                                  "Name",
                                                                  "Version",
                                                                  "State",
                                                                  "Action"});

        updatableModulesTableModel = new ModuleTableModel(updatableModuleItems,
                                                          new String[]{
                                                                  "Name",
                                                                  "Version",
                                                                  "New Version",
                                                                  "Date",
                                                                  "Size",
                                                                  "Action"});

        availableModulesTableModel = new ModuleTableModel(availableModuleItems,
                                                          new String[]{
                                                                  "Name",
                                                                  "Version",
                                                                  "Date",
                                                                  "Size",
                                                                  "Action"});

        JTable installedModulesTable = createModuleTable(installedModulesTableModel);
        installedModulesTable.setName("installedModulesTable");
        JTable updatableModulesTable = createModuleTable(updatableModulesTableModel);
        updatableModulesTable.setName("updatableModulesTable");
        JTable availableModulesTable = createModuleTable(availableModulesTableModel);
        availableModulesTable.setName("availableModulesTable");

        modulesTables = new JTable[]{
                installedModulesTable,
                updatableModulesTable,
                availableModulesTable
        };

        tabbedPane = new JTabbedPane();
        tabbedPane.setName("tabbedPane");
        Component installTab = tabbedPane.add("Installed Modules", new JScrollPane(installedModulesTable));
        installTab.setName("InstallTab");
        Component updateTab = tabbedPane.add("Module Updates", new JScrollPane(updatableModulesTable));
        updateTab.setName("UpdateTab");
        Component availableTab = tabbedPane.add("Available Modules", new JScrollPane(availableModulesTable));
        availableTab.setName("AvailableTab");
        tabbedPane.setSelectedIndex(0);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                onTabChanged();
            }
        });

        JToolBar actionBar = new JToolBar();
        actionBar.setName("actionBar");
        actionBar.setRollover(true);
        actionBar.setFloatable(false);
        JButton installButton = new JButton(installAction);
        installButton.setName("installButton");
        actionBar.add(installButton);
        JButton updateButton = new JButton(updateAction);
        updateButton.setName("updateButton");
        actionBar.add(updateButton);
        JButton uninstallButton = new JButton(uninstallAction);
        uninstallButton.setName("uninstallButton");
        actionBar.add(uninstallButton);
        JButton clearButton = new JButton(clearAction);
        clearButton.setName("clearButton");
        actionBar.add(clearButton);

        categories = new JComboBox();
        categories.setName("categoriesComboBox");
        categories.setEditable(true);
        categories.setSelectedItem("");
        categories.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateModuleTable();
                updateUiState();
            }
        });

        JPanel categoriesBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        categoriesBar.add(new JLabel("Filter category:"));
        categoriesBar.add(categories);

        final JPanel tableActionBar = new JPanel(new BorderLayout(4, 4));
        tableActionBar.add(actionBar, BorderLayout.WEST);
        tableActionBar.add(categoriesBar, BorderLayout.EAST);

        infoPane = new InfoPane();

        JPanel tablePane = new JPanel(new BorderLayout(2, 2));
        tablePane.add(tableActionBar, BorderLayout.NORTH);
        tablePane.add(tabbedPane, BorderLayout.CENTER);

        JSplitPane contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        contentPane.setContinuousLayout(true);
        contentPane.setResizeWeight(0.6);
        contentPane.setTopComponent(tablePane);
        contentPane.setBottomComponent(infoPane);
        contentPane.setBorder(BorderFactory.createEmptyBorder());
        contentPane.setUI(createPlainDividerSplitPaneUI());

        add(contentPane, BorderLayout.CENTER);

        setPreferredSize(new Dimension(400, 400));

        registerCategories(installedModuleItems);
        updateCategories();

        updateUiState();
    }

    private void onTabChanged() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != 0) {
            if (!syncPerformed) {
                syncAction.run();
                syncPerformed = true;
            }
        }
        updateUiState();
    }

    private boolean performUpdateActions(final Runnable doneHandler) {
        final ArrayList<ModuleItem> actionList = createActionList();
        if (actionList.isEmpty()) {
            doneHandler.run();
            return true;
        }

        MissingModuleDependency[] missingModuleDependencies = getMissingModuleDependencies();
        if (missingModuleDependencies.length > 0) {
            String missingModuleDependenciesMessage = createMissingModuleDependenciesMessage(missingModuleDependencies);
            showErrorDialog(missingModuleDependenciesMessage);
            return false;
        }

        String m = ModuleTextFactory.getActionListText(actionList);
        int a = showOkCancelDialog(m);
        if (a != JOptionPane.OK_OPTION) {
            return false;
        }

        ActionPerformer performer = new ActionPerformer(actionList) {
            @Override
            protected void done() {
                boolean success;
                try {
                    try {
                        success = get();
                    } catch (InterruptedException e) {
                        showErrorDialog("Update action(s) failed:\n" + e.getMessage() + "\n" +
                                "No changes will be performed.");
                        return;
                    } catch (ExecutionException e) {
                        showErrorDialog("Update action(s) failed:\n" + e.getCause().getMessage() + "\n" +
                                "No changes will be performed.");
                        return;
                    }

                    if (success) {
                        showInfoDialog("Changes will be effective after restart.");
                    } else {
                        showErrorDialog("Update action(s) canceled.\n" +
                                "No changes will be performed.");
                    }
                } finally {
                    doneHandler.run();
                }
            }
        };

        performer.execute();

        return true;
    }

    private ArrayList<ModuleItem> createActionList() {
        final ArrayList<ModuleItem> actionList = new ArrayList<ModuleItem>(10);
        for (ModuleItem moduleItem : installedModuleItems) {
            if (moduleItem.getAction() == ModuleItem.Action.UNINSTALL) {
                actionList.add(moduleItem);
            }
        }
        for (ModuleItem moduleItem : updatableModuleItems) {
            if (moduleItem.getAction() == ModuleItem.Action.UPDATE) {
                actionList.add(moduleItem);
            }
        }
        for (ModuleItem moduleItem : availableModuleItems) {
            if (moduleItem.getAction() == ModuleItem.Action.INSTALL) {
                actionList.add(moduleItem);
            }
        }
        return actionList;
    }


    private BasicSplitPaneUI createPlainDividerSplitPaneUI() {
        return new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    /**
                     * Overridden in order to do nothing (instead of painting an ugly default divider)
                     *
                     * @param g the graphics object
                     */
                    @Override
                    public void paint(Graphics g) {
                        // do nothing
                    }
                };
            }
        };
    }

    private JTable createModuleTable(ModuleTableModel dm) {
        final JTable table = new JTable(dm);
        //table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateUiState();
                infoPane.setSelectedModuleItems(getSelectedModuleItems());
            }
        });
        table.addMouseListener(new MouseAdapter() {
            private JPopupMenu popupMenu;

            @Override
            public void mouseClicked(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            private void checkPopup(final MouseEvent me) {
                if (me.isPopupTrigger()) {
                    popupMenu = new JPopupMenu();
                    if (installAction.isEnabled()) {
                        popupMenu.add(installAction);
                    } else if (updateAction.isEnabled()) {
                        popupMenu.add(updateAction);
                    } else if (uninstallAction.isEnabled()) {
                        popupMenu.add(uninstallAction);
                    } else if (clearAction.isEnabled()) {
                        popupMenu.add(clearAction);
                    }
                    if (popupMenu.getComponentCount() > 0) {
                        popupMenu.addSeparator();
                    }
                    popupMenu.add(selectAllAction);
                    popupMenu.show(me.getComponent(), me.getX(), me.getY());
                }
            }
        });
        return table;
    }


    private void updateModuleTable() {
        String filter = categories.getSelectedItem().toString();
        updatableModuleItems = extractUpdates(installedModuleItems);
        installedModulesTableModel.setModuleItems(filterModuleItems(installedModuleItems, filter));
        updatableModulesTableModel.setModuleItems(filterModuleItems(updatableModuleItems, filter));
        availableModulesTableModel.setModuleItems(filterModuleItems(availableModuleItems, filter));
        registerCategories(availableModuleItems);
        updateCategories();
    }

    private static ModuleItem[] extractUpdates(ModuleItem[] moduleItems) {
        ArrayList<ModuleItem> updates = new ArrayList<ModuleItem>(moduleItems.length);
        for (ModuleItem installedModuleItem : moduleItems) {
            if (installedModuleItem.getRepositoryModule() != null) {
                updates.add(installedModuleItem);
            }
        }
        return updates.toArray(new ModuleItem[updates.size()]);
    }

    static ModuleItem[] filterModuleItems(ModuleItem[] moduleItems, String filter) {
        if (!filter.isEmpty()) {
            ArrayList<ModuleItem> filteredModuleItems = new ArrayList<ModuleItem>(moduleItems.length);
            for (ModuleItem moduleItem : moduleItems) {
                if (matchesCategory(moduleItem, filter)) {
                    filteredModuleItems.add(moduleItem);
                }
            }
            moduleItems = filteredModuleItems.toArray(new ModuleItem[filteredModuleItems.size()]);
        }

        Arrays.sort(moduleItems);
        return moduleItems;
    }

    private static boolean matchesCategory(ModuleItem moduleItem, String filter) {
        boolean found = false;
        String filterLC = filter.toLowerCase();
        String[] categories = moduleItem.getModule().getCategories();
        for (String category : categories) {
            if (category.toLowerCase().startsWith(filterLC)) {
                found = true;
                break;
            }
        }
        return found;
    }

    protected static boolean isCategory(Module module, String category) {
        String categoryLC = category.toLowerCase();
        String[] categories = module.getCategories();
        for (String someCategory : categories) {
            if (someCategory.toLowerCase().contains(categoryLC)) {
                return true;
            }
        }
        return false;
    }

    private void updateCategories() {
        categoriesSet.add(new CaselessKey(""));
        CaselessKey[] keys = categoriesSet.toArray(new CaselessKey[categoriesSet.size()]);
        Arrays.sort(keys);
        Object selectedItem = categories.getSelectedItem().toString();
        categories.setModel(new DefaultComboBoxModel(keys));
        categories.setSelectedItem(selectedItem);
    }

    private void registerCategories(ModuleItem[] moduleItems) {
        for (ModuleItem moduleItem : moduleItems) {
            String[] categories = moduleItem.getModule().getCategories();
            for (String category : categories) {
                categoriesSet.add(new CaselessKey(category));
            }
        }
    }

    private int showOkCancelDialog(String m) {
        return JOptionPane.showConfirmDialog(this, m, TITLE, JOptionPane.OK_CANCEL_OPTION,
                                             JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorDialog(String m) {
        JOptionPane.showMessageDialog(this, m, TITLE, JOptionPane.ERROR_MESSAGE);
    }

    private void showInfoDialog(String m) {
        JOptionPane.showMessageDialog(this, m, TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleRepositoryConnectionFailed(String message) {
        String m = MessageFormat.format("Failed to download module list from module repository:\n{0}\n\n{1}",
                                        message,
                                        getRepositoryTroubleShootingMessage());
        showErrorDialog(m);
    }

    private void updateUiState() {

        boolean clearEnabled = false;
        boolean installEnabled = false;
        boolean updateEnabled = false;
        boolean uninstallEnabled = false;

        ModuleItem[] selectedModuleItems = getSelectedModuleItems();
        if (selectedModuleItems.length != 0) {
            installEnabled = true;
            updateEnabled = true;
            uninstallEnabled = true;
            for (ModuleItem selectedModuleItem : selectedModuleItems) {
                if (!selectedModuleItem.getAction().equals(ModuleItem.Action.NONE)) {
                    clearEnabled = true;
                }
                if (!isAvailableModule(selectedModuleItem)) {
                    installEnabled = false;
                }
                if (!isModuleUpdate(selectedModuleItem)) {
                    updateEnabled = false;
                }
                if (isAvailableModule(selectedModuleItem)
                        || selectedModuleItem.getModule().getState().equals(ModuleState.UNINSTALLED)
                        || isCategory(selectedModuleItem.getModule(), "System")) {
                    uninstallEnabled = false;
                }
            }
            installEnabled = confirmActionEnabled(ModuleItem.Action.INSTALL, installEnabled);
            updateEnabled = confirmActionEnabled(ModuleItem.Action.UPDATE, updateEnabled);
            uninstallEnabled = confirmActionEnabled(ModuleItem.Action.UNINSTALL, uninstallEnabled);
        }

        clearAction.setEnabled(clearEnabled);
        installAction.setEnabled(installEnabled);
        updateAction.setEnabled(updateEnabled);
        uninstallAction.setEnabled(uninstallEnabled);
        syncAction.setEnabled(true);

        infoPane.setSelectedModuleItems(getSelectedModuleItems());
    }

    private boolean confirmActionEnabled(ModuleItem.Action action, boolean enabled) {
        if (enabled) {
            enabled = false;
            ModuleItem[] selectedModuleItems = getSelectedModuleItems();
            for (ModuleItem moduleItem : selectedModuleItems) {
                if (!moduleItem.getAction().equals(action)) {
                    return true;
                }
            }
        }
        return enabled;
    }

    private static boolean isAvailableModule(ModuleItem selectedModuleItem) {
        return selectedModuleItem.getRepositoryModule() != null &&
                selectedModuleItem.getModule() == selectedModuleItem.getRepositoryModule();
    }

    private static boolean isModuleUpdate(ModuleItem selectedModuleItem) {
        return selectedModuleItem.getRepositoryModule() != null &&
                selectedModuleItem.getModule() != selectedModuleItem.getRepositoryModule();
    }

    private ModuleItem[] getSelectedModuleItems() {
        return getSelectedModuleItems(getSelectedTable());
    }

    private JTable getSelectedTable() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        return modulesTables[selectedIndex];
    }

    private static ModuleItem[] getSelectedModuleItems(JTable table) {
        final int[] rows = table.getSelectedRows();
        ModuleItem[] moduleItems = new ModuleItem[rows.length];
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            moduleItems[i] = ((ModuleTableModel) table.getModel()).getModuleItem(row);
        }
        return moduleItems;
    }


    private MissingModuleDependency[] getMissingModuleDependencies() {
        Map<String, Module> currentModuleMap = getCurrentModuleMap();
        Collection<Module> modules = currentModuleMap.values();
        Map<String, MissingModuleDependency> missingModuleDependencyMap = new HashMap<String, MissingModuleDependency>(10);
        for (Module module : modules) {
            Dependency[] declaredDependencies = module.getDeclaredDependencies();
            for (Dependency dependency : declaredDependencies) {
                if (isMandatoryModuleDependency(dependency)) {
                    String requiredVersionStr = dependency.getVersion();
                    Version requiredVersion = null;
                    if (requiredVersionStr != null) {
                        requiredVersion = Version.parseVersion(requiredVersionStr);
                    }
                    Module dependencyModule = findModule(currentModuleMap, dependency.getModuleSymbolicName(),
                                                         requiredVersion);
                    if (dependencyModule == null) {
                        String key = dependency.getModuleSymbolicName();
                        if (requiredVersionStr != null) {
                            key += "-" + requiredVersionStr;
                        }
                        MissingModuleDependency missingModuleDependency = missingModuleDependencyMap.get(key);
                        if (missingModuleDependency == null) {
                            missingModuleDependency = new MissingModuleDependency(dependency);
                            missingModuleDependencyMap.put(key, missingModuleDependency);
                        }
                        missingModuleDependency.modules.add(module);
                    }
                }
            }
        }

        return missingModuleDependencyMap.values().toArray(new MissingModuleDependency[missingModuleDependencyMap.size()]);
    }

    private static boolean isMandatoryModuleDependency(Dependency dependency) {
        return dependency.getModuleSymbolicName() != null && !dependency.isOptional();
    }

    private static String createMissingModuleDependenciesMessage(
            MissingModuleDependency[] missingModuleDependencieses) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("Inconsistent module set detected.\n\n");
        int lineCount = 0;
        for (MissingModuleDependency missingModuleDependency : missingModuleDependencieses) {
            Dependency dependency = missingModuleDependency.dependency;
            List<Module> missingDependencyList = missingModuleDependency.modules;


            sb.append(MessageFormat.format(
                    "Unresolved module [{0}], version {1} required by module(s)\n",
                    ModuleTextFactory.getText(dependency.getModuleSymbolicName()),
                    ModuleTextFactory.getText(dependency.getVersion())));
            lineCount++;
            for (Module module : missingDependencyList) {
                sb.append("-   ");
                if (lineCount <= 8) {
                    sb.append(MessageFormat.format("{0} [{1}], version {2}\n",
                                                   ModuleTextFactory.getNameText(module),
                                                   ModuleTextFactory.getText(module.getSymbolicName()),
                                                   ModuleTextFactory.getVersionText(module)));
                    lineCount++;
                } else {
                    sb.append("more...\n");
                    break;
                }
            }
        }
        sb.append("\n");
        sb.append("Please check related install, update and uninstall actions.");
        return sb.toString();
    }

    private Map<String, Module> getCurrentModuleMap() {
        Map<String, Module> modules = new HashMap<String, Module>(37);
        for (ModuleItem installedModuleItem : installedModuleItems) {
            if (installedModuleItem.getAction().equals(ModuleItem.Action.NONE)
                    || installedModuleItem.getAction().equals(ModuleItem.Action.UPDATE)) {
                ModuleImpl module = installedModuleItem.getModule();
                modules.put(module.getSymbolicName(), module);
            }
        }
        for (ModuleItem availableModuleItem : availableModuleItems) {
            if (availableModuleItem.getAction().equals(ModuleItem.Action.INSTALL)) {
                ModuleImpl module = availableModuleItem.getModule();
                modules.put(module.getSymbolicName(), module);
            }
        }
        return modules;
    }

    private static Module findModule(Map<String, Module> modules, String symbolicName, Version requiredVersion) {
        Module module = modules.get(symbolicName);
        if (requiredVersion == null) {
            return module;
        }
        if (versionMatches(module.getVersion(), requiredVersion)) {
            return module;
        }
        return null;
    }

    private static boolean versionMatches(Version availableVersion, Version requiredVersion) {
        return availableVersion.getMajor() == requiredVersion.getMajor()
                && availableVersion.getMinor() >= requiredVersion.getMinor();
    }

    private void setActionId(ModuleItem.Action action) {
        ModuleItem[] moduleItems = getSelectedModuleItems();
        for (ModuleItem moduleItem : moduleItems) {
            moduleItem.setAction(action);
        }
        onActionChanged();
    }

    private void onActionChanged() {
        installedModulesTableModel.fireTableDataChanged();
        updatableModulesTableModel.fireTableDataChanged();
        availableModulesTableModel.fireTableDataChanged();
        updateUiState();
    }

    private static ModuleItem[] toModuleItems(Module[] modules) {
        ModuleItem[] items = new ModuleItem[modules.length];
        for (int i = 0; i < modules.length; i++) {
            ModuleImpl module = (ModuleImpl) modules[i];
            items[i] = new ModuleItem(module);
        }
        return items;
    }

    private class SyncAction extends AbstractAction {

        public SyncAction() {
            putValue(ACTION_COMMAND_KEY, getClass().getName());
            putValue(NAME, "Synchronize");
            putValue(SHORT_DESCRIPTION, "Synchronize with remote repository");
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("icons/system-software-update.png")));
        }

        public void actionPerformed(ActionEvent event) {
            run();
        }

        public void run() {
            new ModuleListDownloader().execute();
        }
    }

    private class ClearAction extends AbstractAction {

        public ClearAction() {
            putValue(ACTION_COMMAND_KEY, getClass().getName());
            putValue(NAME, "Clear");
            putValue(MNEMONIC_KEY, (int) 'C');
            putValue(SHORT_DESCRIPTION, "Clear a previous install, update or uninstall action");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource("icons/edit-clear.png")));
            putValue(LARGE_ICON_KEY, getValue(SMALL_ICON));
        }

        public void actionPerformed(ActionEvent e) {
            setActionId(ModuleItem.Action.NONE);
        }

    }

    private class InstallAction extends AbstractAction {

        public InstallAction() {
            putValue(ACTION_COMMAND_KEY, getClass().getName());
            putValue(NAME, "Install");
            putValue(MNEMONIC_KEY, (int) 'I');
            putValue(SHORT_DESCRIPTION, "Install the selected modules");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource("icons/list-add.png")));
            putValue(LARGE_ICON_KEY, getValue(SMALL_ICON));
        }

        public void actionPerformed(ActionEvent e) {
            setActionId(ModuleItem.Action.INSTALL);
        }
    }

    private class UpdateAction extends AbstractAction {

        public UpdateAction() {
            putValue(ACTION_COMMAND_KEY, getClass().getName());
            putValue(NAME, "Update");
            putValue(MNEMONIC_KEY, (int) 'U');
            putValue(SHORT_DESCRIPTION, "Update selected modules");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource("icons/view-refresh.png")));
            putValue(LARGE_ICON_KEY, getValue(SMALL_ICON));
        }

        public void actionPerformed(ActionEvent e) {
            setActionId(ModuleItem.Action.UPDATE);
        }
    }

    private class UninstallAction extends AbstractAction {

        public UninstallAction() {
            putValue(ACTION_COMMAND_KEY, getClass().getName());
            putValue(NAME, "Uninstall");
            putValue(MNEMONIC_KEY, (int) 'N');
            putValue(SHORT_DESCRIPTION, "Uninstall selected modules");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource("icons/list-remove.png")));
            putValue(LARGE_ICON_KEY, getValue(SMALL_ICON));
        }

        public void actionPerformed(ActionEvent e) {
            setActionId(ModuleItem.Action.UNINSTALL);
        }

    }


    private class SelectAllAction extends AbstractAction {

        public SelectAllAction() {
            putValue(ACTION_COMMAND_KEY, getClass().getName());
            putValue(NAME, "Select All");
            putValue(MNEMONIC_KEY, (int) 'A');
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl A"));
            putValue(SHORT_DESCRIPTION, "Select all items in table");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource("icons/edit-select-all.png")));
            putValue(LARGE_ICON_KEY, getValue(SMALL_ICON));
        }

        public void actionPerformed(ActionEvent ae) {
            getSelectedTable().selectAll();
        }
    }

    private class ModuleListDownloader extends ProgressMonitorSwingWorker<Module[], Void> {

        public ModuleListDownloader() {
            super(ModuleManagerPane.this, TITLE);
        }

        @Override
        protected Module[] doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
            Module[] availableModules = moduleManager.getRepositoryModules(pm);
            if (pm.isCanceled()) {
                throw new CanceledException();
            }
            return availableModules;
        }

        @Override
        protected void done() {
            if (isCancelled()) {
                return;
            }
            try {
                availableModuleItems = ModuleSyncRunner.sync(installedModuleItems, get());
            } catch (InterruptedException e) {
                handleRepositoryConnectionFailed(e.getMessage());
                return;
            } catch (ExecutionException e) {
                handleRepositoryConnectionFailed(e.getCause().getMessage());
                return;
            }

            updateModuleTable();
            updateUiState();
        }
    }


    static class MissingModuleDependency {

        Dependency dependency;
        List<Module> modules = new ArrayList<Module>(10);

        public MissingModuleDependency(Dependency dependency) {
            this.dependency = dependency;
        }
    }

    private class ActionPerformer extends ProgressMonitorSwingWorker<Boolean, Boolean> {

        private final List<ModuleItem> actionListList;

        public ActionPerformer(List<ModuleItem> actionListList) {
            super(ModuleManagerPane.this, ModuleManagerPane.TITLE);
            this.actionListList = actionListList;
        }

        @Override
        protected Boolean doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
            pm.beginTask("Performing update actions", actionListList.size());

            try {
                moduleManager.startTransaction();
                for (ModuleItem actionItem : actionListList) {
                    String actionText = ModuleTextFactory.getActionItemText(actionItem);
                    pm.setTaskName(actionText);
                    if (actionItem.getAction() == ModuleItem.Action.INSTALL) {
                        moduleManager.installModule(actionItem.getRepositoryModule(),
                                                    SubProgressMonitor.create(pm, 1));
                    } else if (actionItem.getAction() == ModuleItem.Action.UPDATE) {
                        moduleManager.updateModule(actionItem.getModule(),
                                                   actionItem.getRepositoryModule(),
                                                   SubProgressMonitor.create(pm, 1));
                    } else if (actionItem.getAction() == ModuleItem.Action.UNINSTALL) {
                        moduleManager.uninstallModule(actionItem.getModule(),
                                                      SubProgressMonitor.create(pm, 1));
                    }
                    if (pm.isCanceled()) {
                        throw new CanceledException();
                    }
                }
            } catch (CoreException e) {
                moduleManager.rollbackTransaction();
                throw e;
            } finally {
                pm.done();
            }

            moduleManager.endTransaction();
            return true;
        }
    }

    public static interface HelpHandler {
        void configureHelpButton(JButton helpButton);
    }

}
