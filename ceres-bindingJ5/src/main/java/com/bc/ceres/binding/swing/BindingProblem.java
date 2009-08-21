package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.BindingException;

/**
 * Represents a problem of a {@link com.bc.ceres.binding.swing.Binding} which may occur
 * when transferring data from a Swing component into the the bound property
 * ({@link com.bc.ceres.binding.ValueModel ValueModel}).
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.10
 */
public interface BindingProblem {
    /**
     * @return The binding which has (or had) this problem.
     */
    Binding getBinding();

    /**
     * @return The cause of the problem.
     */
    BindingException getCause();
}
