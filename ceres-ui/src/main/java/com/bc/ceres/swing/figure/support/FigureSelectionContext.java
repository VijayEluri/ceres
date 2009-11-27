package com.bc.ceres.swing.figure.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.figure.AbstractFigureChangeListener;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.support.SelectionChangeSupport;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class FigureSelectionContext implements SelectionContext {

    private final FigureEditor figureEditor;
    private final FigureCollection figureCollection;
    private final FigureSelection figureSelection;
    private final SelectionChangeSupport selectionChangeSupport;

    public FigureSelectionContext(FigureEditor figureEditor) {
        this(figureEditor,
             new DefaultFigureCollection(),
             new DefaultFigureSelection());
    }

    public FigureSelectionContext(FigureEditor figureEditor,
                                  FigureCollection figureCollection,
                                  FigureSelection figureSelection) {
        Assert.notNull(figureEditor, "figureEditor");
        Assert.notNull(figureCollection, "figureCollection");
        Assert.notNull(figureSelection, "figureSelection");
        this.figureEditor = figureEditor;
        this.figureCollection = figureCollection;
        this.figureSelection = figureSelection;
        this.figureSelection.addListener(new FigureSelectionMulticaster());
        this.selectionChangeSupport = new SelectionChangeSupport(figureEditor);
    }

    public FigureEditor getFigureEditor() {
        return figureEditor;
    }

    public FigureCollection getFigureCollection() {
        return figureCollection;
    }

    public FigureSelection getFigureSelection() {
        return figureSelection;
    }

    @Override
    public Selection getSelection() {
        return figureSelection;
    }

    @Override
    public void setSelection(Selection selection) {
        // todo - implement (select all figures that are equal to the ones in selection)
    }

    @Override
    public void addSelectionChangeListener(SelectionChangeListener listener) {
        selectionChangeSupport.addSelectionChangeListener(listener);
    }

    @Override
    public void removeSelectionChangeListener(SelectionChangeListener listener) {
        selectionChangeSupport.removeSelectionChangeListener(listener);
    }

    @Override
    public SelectionChangeListener[] getSelectionChangeListeners() {
        return selectionChangeSupport.getSelectionChangeListeners();
    }

    @Override
    public void insert(Transferable contents) throws IOException, UnsupportedFlavorException {
        Figure[] figures = (Figure[]) contents.getTransferData(FigureTransferable.FIGURES_DATA_FLAVOR);
        // todo - move to FigureEditor.insert(figures)
        figureEditor.getUndoContext().postEdit(new FigureInsertEdit(figureEditor, figures));
    }

    @Override
    public boolean canDeleteSelection() {
        return !getFigureSelection().isEmpty();
    }

    @Override
    public void deleteSelection() {
        Figure[] figures = getFigureSelection().getFigures();
        // todo - move to FigureEditor.delete(figures)
        figureEditor.getUndoContext().postEdit(new FigureDeleteEdit(figureEditor, figures));
    }

    @Override
    public boolean canInsert(Transferable contents) {
        return contents.isDataFlavorSupported(FigureTransferable.FIGURES_DATA_FLAVOR);
    }

    @Override
    public void selectAll() {
        figureSelection.removeFigures();
        figureSelection.addFigures(getFigureCollection().getFigures());
        figureSelection.setSelectionLevel(figureSelection.getMaxSelectionLevel());
    }

    @Override
    public boolean canSelectAll() {
        return getFigureCollection().getFigureCount() > 0;
    }

    private class FigureSelectionMulticaster extends AbstractFigureChangeListener {
        @Override
        public void figuresAdded(FigureChangeEvent event) {
            selectionChangeSupport.fireSelectionChange(FigureSelectionContext.this, figureSelection);
        }

        @Override
        public void figuresRemoved(FigureChangeEvent event) {
            selectionChangeSupport.fireSelectionChange(FigureSelectionContext.this, figureSelection);
        }
    }
}