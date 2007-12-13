package com.bc.ceres.swing.progress;

import com.bc.ceres.core.Assert;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;

/**
 * A {@link com.bc.ceres.core.ProgressMonitor} which uses a
 * Swing's {@link javax.swing.ProgressMonitor} to display progress.
 */
public class DialogProgressMonitor implements com.bc.ceres.core.ProgressMonitor {

    private ProgressDialog progressDialog;
    private JLabel messageLabel;
    private double currentWork;
    private double totalWork;

    private int totalWorkUI;
    private int currentWorkUI;
    private int lastWorkUI;

    /**
     * Constructs an instance of this class.
     *
     * @param parentComponent The parent component.
     * @param title           The dialog's title.
     * @param modalityType    The modality type.
     */
    public DialogProgressMonitor(Component parentComponent, String title, Dialog.ModalityType modalityType) {
        messageLabel = new JLabel();
        progressDialog = new ProgressDialog(parentComponent);
        progressDialog.setTitle(title);
        progressDialog.setMinimum(0);
        progressDialog.setMaximum(250);
        progressDialog.setModalityType(modalityType);
        progressDialog.setMessageComponent(messageLabel);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param progressDialog The progress dialog to be used.
     */
    public DialogProgressMonitor(ProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
        JComponent messageComponent = progressDialog.getMessageComponent();
        if (messageComponent instanceof JLabel) {
            messageLabel = (JLabel) messageComponent;
        }
    }

    /**
     * Notifies that the main task is beginning.  This must only be called once
     * on a given progress monitor instance.
     *
     * @param name      the name (or description) of the main task
     * @param totalWork the total number of work units into which
     *                  the main task is been subdivided. If the value is <code>UNKNOWN</code>
     *                  the implementation is free to indicate progress in a way which
     *                  doesn't require the total number of work units in advance.
     */
    public void beginTask(String name, int totalWork) {
        Assert.notNull(name, "name");
        Assert.argument(totalWork > 0, "totalWork > 0");
        this.currentWork = 0.0;
        this.totalWork = totalWork;
        this.currentWorkUI = 0;
        this.lastWorkUI = 0;
        this.totalWorkUI = progressDialog.getMaximum() - progressDialog.getMinimum();
        if (messageLabel != null) {
            this.messageLabel.setText(name);
        }
        runInUI(new Runnable() {
            public void run() {
                if (progressDialog != null) {
                    if (currentWorkUI < totalWorkUI) {
                        progressDialog.show();
                    } else {
                        progressDialog = null; // no longer used                        
                    }
                }
            }
        });
    }

    /**
     * Notifies that the work is done; that is, either the main task is completed
     * or the user canceled it. This method may be called more than once
     * (implementations should be prepared to handle this case).
     */
    public void done() {
        runInUI(new Runnable() {
            public void run() {
                if (progressDialog != null) {
                    progressDialog.close();
                    progressDialog = null;
                }
            }
        });
    }


    /**
     * Internal method to handle scaling correctly. This method
     * must not be called by a client. Clients should
     * always use the method </code>worked(int)</code>.
     *
     * @param work the amount of work done
     */
    public void internalWorked(double work) {
        currentWork += work;
        currentWorkUI = (int) (totalWorkUI * currentWork / totalWork);
        if (currentWorkUI > lastWorkUI) {
            lastWorkUI = currentWorkUI;
            runInUI(new Runnable() {
                public void run() {
                    if (progressDialog != null) {
                        int progress = progressDialog.getMinimum() + currentWorkUI;
                        progressDialog.setProgress(progress);
                    }
                }
            });
        }
    }

    /**
     * Returns whether cancelation of current operation has been requested.
     * Long-running operations should poll to see if cancelation
     * has been requested.
     *
     * @return <code>true</code> if cancellation has been requested,
     *         and <code>false</code> otherwise
     * @see #setCanceled(boolean)
     */
    public boolean isCanceled() {
        return progressDialog != null && progressDialog.isCanceled();
    }

    /**
     * Sets the cancel state to the given value.
     *
     * @param canceled <code>true</code> indicates that cancelation has
     *                 been requested (but not necessarily acknowledged);
     *                 <code>false</code> clears this flag
     * @see #isCanceled()
     */
    public void setCanceled(boolean canceled) {
        if (canceled) {
            if (progressDialog != null) {
                progressDialog.cancel();
                if (progressDialog.isCanceled()) {
                    done();
                }
            }
        }
    }

    /**
     * Sets the task name to the given value. This method is used to
     * restore the task label after a nested operation was executed.
     * Normally there is no need for clients to call this method.
     *
     * @param name the name (or description) of the main task
     * @see #beginTask(String, int)
     */
    public void setTaskName(final String name) {
        runInUI(new Runnable() {
            public void run() {
                if (messageLabel != null) {
                    messageLabel.setText(name);
                }
            }
        });
    }

    /**
     * Notifies that a subtask of the main task is beginning.
     * Subtasks are optional; the main task might not have subtasks.
     *
     * @param name the name (or description) of the subtask
     */
    public void setSubTaskName(final String name) {
        runInUI(new Runnable() {
            public void run() {
                if (progressDialog != null) {
                    progressDialog.setNote(name);
                }
            }
        });
    }

    /**
     * Notifies that a given number of work unit of the main task
     * has been completed. Note that this amount represents an
     * installment, as opposed to a cumulative amount of work done
     * to date.
     *
     * @param work the number of work units just completed
     */
    public void worked(int work) {
        internalWorked(work);
    }

    ////////////////////////////////////////////////////////////////////////
    // Stuff to be performed in Swing's event-dispatching thread

    private static void runInUI(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

}
