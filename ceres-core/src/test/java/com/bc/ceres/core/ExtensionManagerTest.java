package com.bc.ceres.core;

import junit.framework.TestCase;

public class ExtensionManagerTest extends TestCase {

    public void testExtensionManagement() {
        ExtensionManager em = ExtensionManager.getInstance();
        assertNotNull(em);

        ExtensionFactory<Model>[] ml = em.getExtensionFactories(Model.class);
        assertNotNull(ml);
        assertEquals(0, ml.length);

        DefaultModelGuiFactory df = new DefaultModelGuiFactory();
        em.register(Model.class, df);
        ml = em.getExtensionFactories(Model.class);
        assertNotNull(ml);
        assertEquals(1, ml.length);
        assertSame(df, ml[0]);

        DefaultModelGuiFactory df2 = new DefaultModelGuiFactory();
        em.register(Model.class, df2);
        ml = em.getExtensionFactories(Model.class);
        assertNotNull(ml);
        assertEquals(2, ml.length);
        assertSame(df, ml[0]);
        assertSame(df2, ml[1]);

        em.register(Model.class, df2);
        ml = em.getExtensionFactories(Model.class);
        assertNotNull(ml);
        assertEquals(2, ml.length);
        assertSame(df, ml[0]);
        assertSame(df2, ml[1]);

        SlimModelGuiFactory sf = new SlimModelGuiFactory();
        em.register(SlimModel.class, sf);
        ExtensionFactory<SlimModel>[] sl = em.getExtensionFactories(SlimModel.class);
        assertNotNull(sl);
        assertEquals(1, sl.length);
        assertSame(sf, sl[0]);

        RichModelGuiFactory rf = new RichModelGuiFactory();
        em.register(RichModel.class, rf);
        ExtensionFactory<RichModel>[] rl = em.getExtensionFactories(RichModel.class);
        assertNotNull(rl);
        assertEquals(1, rl.length);
        assertSame(rf, rl[0]);

        em.unregister(Model.class, df);
        em.unregister(Model.class, df2);
        em.unregister(SlimModel.class, sf);
        em.unregister(RichModel.class, rf);

        assertEquals(0, em.getExtensionFactories(Model.class).length);
        assertEquals(0, em.getExtensionFactories(SlimModel.class).length);
        assertEquals(0, em.getExtensionFactories(RichModel.class).length);
    }

    public void testModelWithIndependentGui() {
        ExtensionManager em = ExtensionManager.getInstance();
        assertNotNull(em);

        // These models will be dynamically extended by special GUIs
        Model someModel = new SomeModel();
        Model slimModel = new SlimModel();
        Model richModel = new RichModel();

        // The GUI factories
        DefaultModelGuiFactory defaultModelGuiFactory = new DefaultModelGuiFactory();
        SlimModelGuiFactory slimModelGuiFactory = new SlimModelGuiFactory();
        RichModelGuiFactory richModelGuiFactory = new RichModelGuiFactory();

        /////////////////////////////////////////////////////////////////////
        // Model --> null
        // SomeModel --> null
        // SlimModel --> null
        // RichModel --> null

        testFail(someModel, ModelGui.class);
        testFail(slimModel, ModelGui.class);
        testFail(richModel, ModelGui.class);

        testFail(someModel, DefaultModelGui.class);
        testFail(slimModel, DefaultModelGui.class);
        testFail(richModel, DefaultModelGui.class);

        testFail(someModel, SlimModelGui.class);
        testFail(slimModel, SlimModelGui.class);
        testFail(richModel, SlimModelGui.class);

        testFail(someModel, RichModelGui.class);
        testFail(slimModel, RichModelGui.class);
        testFail(richModel, RichModelGui.class);

        /////////////////////////////////////////////////////////////////////
        // Model --> DefaultModelGui
        // SomeModel --> DefaultModelGui
        // SlimModel --> DefaultModelGui
        // RichModel --> DefaultModelGui

        em.register(Model.class, defaultModelGuiFactory);

        testSuccess(someModel, ModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, ModelGui.class, DefaultModelGui.class);
        testSuccess(richModel, ModelGui.class, DefaultModelGui.class);

        testSuccess(someModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(richModel, DefaultModelGui.class, DefaultModelGui.class);

        testFail(someModel, SlimModelGui.class);
        testFail(slimModel, SlimModelGui.class);
        testFail(richModel, SlimModelGui.class);

        testFail(someModel, RichModelGui.class);
        testFail(slimModel, RichModelGui.class);
        testFail(richModel, RichModelGui.class);

        /////////////////////////////////////////////////////////////////////
        // Model --> DefaultModelGui
        // SomeModel --> DefaultModelGui
        // SlimModel --> SlimModelGui
        // RichModel --> DefaultModelGui

        em.register(SlimModel.class, slimModelGuiFactory);

        testSuccess(someModel, ModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, ModelGui.class, SlimModelGui.class);
        testSuccess(richModel, ModelGui.class, DefaultModelGui.class);

        testSuccess(someModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(richModel, DefaultModelGui.class, DefaultModelGui.class);

        testFail(someModel, SlimModelGui.class);
        testSuccess(slimModel, SlimModelGui.class, SlimModelGui.class);
        testFail(richModel, SlimModelGui.class);

        testFail(someModel, RichModelGui.class);
        testFail(slimModel, RichModelGui.class);
        testFail(richModel, RichModelGui.class);

        /////////////////////////////////////////////////////////////////////
        // Any Model --> DefaultModelGui
        // SomeModel --> DefaultModelGui
        // SlimModel --> SlimModelGui
        // RichModel --> RichModelGui

        em.register(RichModel.class, richModelGuiFactory);

        testSuccess(someModel, ModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, ModelGui.class, SlimModelGui.class);
        testSuccess(richModel, ModelGui.class, RichModelGui.class);

        testSuccess(someModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(slimModel, DefaultModelGui.class, DefaultModelGui.class);
        testSuccess(richModel, DefaultModelGui.class, DefaultModelGui.class);

        testFail(someModel, SlimModelGui.class);
        testSuccess(slimModel, SlimModelGui.class, SlimModelGui.class);
        testFail(richModel, SlimModelGui.class);

        testFail(someModel, RichModelGui.class);
        testFail(slimModel, RichModelGui.class);
        testSuccess(richModel, RichModelGui.class, RichModelGui.class);

        /////////////////////////////////////////////////////////////////////
        // Model --> null
        // SomeModel --> null
        // SlimModel --> null
        // RichModel --> null

        em.unregister(Model.class, defaultModelGuiFactory);
        em.unregister(SlimModel.class, slimModelGuiFactory);
        em.unregister(RichModel.class, richModelGuiFactory);

        testFail(someModel, ModelGui.class);
        testFail(slimModel, ModelGui.class);
        testFail(richModel, ModelGui.class);

        testFail(someModel, DefaultModelGui.class);
        testFail(slimModel, DefaultModelGui.class);
        testFail(richModel, DefaultModelGui.class);

        testFail(someModel, SlimModelGui.class);
        testFail(slimModel, SlimModelGui.class);
        testFail(richModel, SlimModelGui.class);

        testFail(someModel, RichModelGui.class);
        testFail(slimModel, RichModelGui.class);
        testFail(richModel, RichModelGui.class);
    }

    private static void testSuccess(Model model, Class<? extends ModelGui> requestedType, Class<? extends ModelGui> expectedType) {
        ModelGui modelGui = model.getExtension(requestedType);
        assertNotNull(modelGui);
        assertEquals(expectedType, modelGui.getClass());
        assertSame(model, modelGui.getModel());
    }

    private static void testFail(Model model, Class<? extends ModelGui> extensionType) {
        ModelGui modelGui = model.getExtension(extensionType);
        assertNull(modelGui);
    }

    static interface Model extends Extensible {
    }

    static class SomeModel extends ExtensibleObject implements Model {
    }

    static class SlimModel extends ExtensibleObject implements Model {
    }

    static class RichModel extends ExtensibleObject implements Model {
    }

    static abstract class ModelGui {
        Model model;

        protected ModelGui(Model model) {
            this.model = model;
        }

        public Model getModel() {
            return model;
        }
    }

    static class DefaultModelGui extends ModelGui {
        DefaultModelGui(Model model) {
            super(model);
        }
    }

    static class SlimModelGui extends ModelGui {
        SlimModelGui(Model model) {
            super(model);
        }
    }

    static class RichModelGui extends ModelGui {
        RichModelGui(Model model) {
            super(model);
        }
    }

    class DefaultModelGuiFactory implements ExtensionFactory<Model> {
        @Override
        public <E> E getExtension(Model model, Class<E> extensionType) {

            if (ModelGui.class.isAssignableFrom(extensionType)) {
                return (E) new DefaultModelGui(model);
            }
            return null;
        }

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[]{DefaultModelGui.class};
        }
    }

    class SlimModelGuiFactory implements ExtensionFactory<SlimModel> {
        @Override
        public <E> E getExtension(SlimModel model, Class<E> extensionType) {
            if (ModelGui.class.isAssignableFrom(extensionType)) {
                return (E) new SlimModelGui(model);
            }
            return null;
        }

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[]{SlimModelGui.class};
        }
    }

    class RichModelGuiFactory implements ExtensionFactory<RichModel> {
        @Override
        public <E> E getExtension(RichModel model, Class<E> extensionType) {
            if (ModelGui.class.isAssignableFrom(extensionType)) {
                return (E) new RichModelGui(model);
            }
            return null;
        }

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[]{RichModelGui.class};
        }
    }
}