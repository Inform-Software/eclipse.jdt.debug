/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.ui.contentassist;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.internal.debug.core.JavaDebugUtils;
import org.eclipse.jdt.internal.debug.eval.ast.engine.ASTEvaluationEngine;
import org.eclipse.jdt.internal.debug.eval.ast.engine.ArrayRuntimeContext;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

public class CurrentValueContext extends CurrentFrameContext {

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.debug.ui.contentassist.IJavaDebugContentAssistContext#getType()
     */
    public IType getType() throws CoreException {
    	IJavaValue value = resolveValue();
    	if (value == null) {
    		// no object selected, use the frame
    		return super.getType();
    	}
    	if (value instanceof IJavaArray) {
    		// completion in context of Object
    		return JavaDebugUtils.resolveType("java.lang.Object", value.getLaunch()); //$NON-NLS-1$
    	}
    	IType type = JavaDebugUtils.resolveType(value);
    	if (type == null) {
    		unableToResolveType();
    	}
    	return type;
     }
    
    /**
     * Returns the value for which completions are to be computed for, or <code>null</code> if none.
     * 
     * @return the value for which completions are to be computed for, or <code>null</code> if none
     * @throws CoreException
     */
    protected IJavaValue resolveValue() throws CoreException {
        IJavaStackFrame stackFrame= getStackFrame();
        if (stackFrame == null) {
            unableToResolveType();
        }
        
        IWorkbenchWindow window= JDIDebugUIPlugin.getActiveWorkbenchWindow();
        if (window == null) {
            unableToResolveType();
        }
        IWorkbenchPage page= window.getActivePage();
        if (page == null) {
            unableToResolveType();
        }
        IDebugView view= (IDebugView)page.getActivePart();
        if (view == null) {
            unableToResolveType();
        }
        ISelection selection= view.getViewer().getSelection();
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            IStructuredSelection viewerSelection= (IStructuredSelection)selection;
            if (viewerSelection.size() > 1) {
                unableToResolveType();
            }
            Object element= viewerSelection.getFirstElement();  
            
            IValue value= null;
            if (element instanceof IVariable) {
                IVariable variable = (IVariable)element;
                if (!variable.getName().equals("this")) { //$NON-NLS-1$
                	value= variable.getValue();
                }
            } else if (element instanceof IExpression) {
                value= ((IExpression)element).getValue();   
            }
            if (value instanceof IJavaValue) {
    			return (IJavaValue) value;
    		}
        }
        return null;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.ui.contentassist.IJavaDebugContentAssistContext#getLocalVariables()
	 */
	public String[][] getLocalVariables() throws CoreException {
		IJavaValue value = resolveValue();
		if (value instanceof IJavaArray) {
			// do a song and dance to fake 'this' as an array receiver
			return new String[][]{new String[] {ArrayRuntimeContext.ARRAY_THIS_VARIABLE}, new String[] {value.getJavaType().getName()}};
		}
		return super.getLocalVariables();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.ui.contentassist.IJavaDebugContentAssistContext#getSnippet(java.lang.String)
	 */
	public String getSnippet(String snippet) throws CoreException {
		IJavaValue value = resolveValue();
		if (value instanceof IJavaArray) {
			return ASTEvaluationEngine.replaceThisReferences(snippet);
		}
		return super.getSnippet(snippet);
	}

    
}
