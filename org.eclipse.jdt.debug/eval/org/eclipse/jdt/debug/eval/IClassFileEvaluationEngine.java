package org.eclipse.jdt.debug.eval;

import java.io.File;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaThread;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * An evaluation engine that performs evaluations by
 * deploying and executing class files locally.
 * 
 * @since 2.0
 */ 
public interface IClassFileEvaluationEngine extends IEvaluationEngine {
	/**
	 * Returns the import declarations for this evaluation context. An empty
	 * list indicates there are no imports. The syntax for the import corresponds to a 
	 * fully qualified type name, or to an on-demand package name as defined by
	 * ImportDeclaration (JLS2 7.5). For example, <code>"java.util.Hashtable"</code>
	 * or <code>"java.util.*"</code>.
	 *
	 * @param imports the list of import names
	 */
	public String[] getImports();
	
	/**
	 * Sets the import declarations for this evaluation context. An empty
	 * list indicates there are no imports. The syntax for the import corresponds to a 
	 * fully qualified type name, or to an on-demand package name as defined by
	 * ImportDeclaration (JLS2 7.5). For example, <code>"java.util.Hashtable"</code>
	 * or <code>"java.util.*"</code>.
	 *
	 * @param imports the list of import names
	 */
	public void setImports(String[] imports);
		
	/**
	 * Asynchronously evaluates the given snippet in the specified
	 * target thread, reporting the result back to the given listener.
	 * The snippet is evaluated in the context of the Java
	 * project this evaluation engine was created on. If the
	 * snippet is determined to be a valid expression, the expression
	 * is evaluated in the specified thread, which resumes its
	 * execution from the location at which it is currently suspended.
	 * When the evaluation completes, the thread will be suspened
	 * at this original location.
	 * 
	 * @param snippet code snippet to evaluate
	 * @param thread the thread in which to run the evaluation,
	 *   which must be suspended
	 * @param listener the listener that will receive notification
	 *   when/if the evalaution completes
	 * @exception DebugException if this method fails.  Reasons include:<ul>
	 * <li>Failure communicating with the VM.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * <li>The specified thread is not currently suspended</li>
	 * <li>The specified thread is not contained in the debug target
	 *   associated with this evaluation engine</li>
	 * <li>The specified thread is suspended in the middle of
	 *  an evaluation that has not completed. It is not possible
	 *  to perform nested evaluations</li>
	 * </ul>
	 */
	public void evaluate(String snippet, IJavaThread thread, IEvaluationListener listener) throws DebugException;
	
	
}

