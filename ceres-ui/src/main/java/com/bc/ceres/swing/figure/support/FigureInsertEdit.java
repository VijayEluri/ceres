package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureEditor;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;


public class FigureInsertEdit extends AbstractUndoableEdit {

    private FigureEditor figureEditor;
    private Figure[] addedFigures;

    public FigureInsertEdit(FigureEditor figureEditor, Figure... figuresToInsert) {
        this.figureEditor = figureEditor;
        this.addedFigures = figureEditor.getFigureCollection().addFigures(figuresToInsert);

        figureEditor.getFigureSelection().removeFigures();
        figureEditor.getFigureSelection().setSelectionLevel(0);

// todo - check, this may be ok, if SelectInteraction is auto-activated
//        figureEditor.getFigureSelection().addFigures(addedFigures);
//        figureEditor.getFigureSelection().setSelectionLevel(1);
    }

    @Override
    public String getPresentationName() {
        return addedFigures.length == 1 ? "Insert Figure" : "Insert Figures";
    }

    @Override
    public void die() {
        super.die();
        figureEditor = null;
        addedFigures = null;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        figureEditor.getFigureSelection().removeFigures();
        figureEditor.getFigureSelection().setSelectionLevel(0);
        figureEditor.getFigureCollection().removeFigures(addedFigures);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        figureEditor.getFigureSelection().removeFigures();
        figureEditor.getFigureSelection().setSelectionLevel(0);
        figureEditor.getFigureCollection().addFigures(addedFigures);
    }
}