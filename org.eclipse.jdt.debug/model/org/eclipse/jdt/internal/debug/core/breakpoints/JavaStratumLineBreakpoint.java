/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.core.breakpoints;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jdt.debug.core.IJavaStratumLineBreakpoint;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.InvalidLineNumberException;
import com.sun.jdi.Location;
import com.sun.jdi.NativeMethodException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;

/**
 * A line breakpoint identified by its source file
 * name and/or path, and stratum that it is relative to.
 * 
 * @since 3.0
 */
public class JavaStratumLineBreakpoint extends JavaLineBreakpoint implements IJavaStratumLineBreakpoint {
	private static final String PATTERN= "org.eclipse.jdt.debug.pattern"; //$NON-NLS-1$
	private static final String STRATUM= "org.eclipse.jdt.debug.stratum"; //$NON-NLS-1$
	private static final String SOURCE_PATH= "org.eclipse.jdt.debug.source_path"; //$NON-NLS-1$
	private static final String STRATUM_BREAKPOINT= "org.eclipse.jdt.debug.javaStratumLineBreakpointMarker"; //$NON-NLS-1$

	public JavaStratumLineBreakpoint() {
	}

	/**
	 * Creates and returns a line breakpoint identified by its source file
	 * name and/or path, and stratum that it is relative to. 
	 * 
	 * @param resource the resource on which to create the associated breakpoint
	 *  marker
	 * @param stratum the stratum in which the source name, source path and line number
	 *  are relative, or <code>null</code>. If <code>null</code> or if the specified stratum
	 *  is not defined for a type, the source name, source path and line number are
	 * 	relative to the type's default stratum.
	 * @param sourceName the simple name of the source file in which the breakpoint is
	 *  set, or <code>null</code>. The breakpoint will install itself in classes that have a source
	 *  file name debug attribute that matches this value in the specified stratum,
	 *  and satisfies the class name pattern and source path attribute. When <code>null</code>,
	 *  the source file name debug attribute is not considered. 
	 * @param sourcePath the qualified source file name in which the breakpoint is
	 *  set, or <code>null</code>. The breakpoint will install itself in classes that
	 *  have a source file path in the specified stratum that matches this value, and
	 *  satisfies the class name pattern and source name attribute. When <code>null</code>,
	 *  the source path attribute is not considered.
	 * @param classNamePattern the class name pattern to which the breakpoint should
	 *   be restricted, or <code>null</code>. The breakpoint will install itself in each type that
	 *   matches this class name pattern, with a satisfying source name and source path.
	 *   Patterns may begin or end with '*', which matches 0 or more characters. A pattern that
	 *   does not contain a '*' is equivalent to a pattern ending in '*'. Specifying <code>null</code>,
	 *   or an empty string is the equivalent to "*". 
	 * @param lineNumber the lineNumber on which the breakpoint is set - line
	 *   numbers are 1 based, associated with the source file (stratum) in which
	 *   the breakpoint is set
	 * @param charStart the first character index associated with the breakpoint,
	 *   or -1 if unspecified, in the source file in which the breakpoint is set
	 * @param charEnd the last character index associated with the breakpoint,
	 *   or -1 if unspecified, in the source file in which the breakpoint is set
	 * @param hitCount the number of times the breakpoint will be hit before
	 *   suspending execution - 0 if it should always suspend
	 * @param register whether to add this breakpoint to the breakpoint manager
	 * @param attributes a map of client defined attributes that should be assigned
	 *  to the underlying breakpoint marker on creation, or <code>null</code> if none.
	 * @return a stratum breakpoint
	 * @exception CoreException If this method fails. Reasons include:<ul> 
	 *<li>Failure creating underlying marker.  The exception's status contains
	 * the underlying exception responsible for the failure.</li></ul>
	 * @since 3.0
	 */
	public JavaStratumLineBreakpoint(IResource resource, String stratum, String sourceName, String sourcePath, String classNamePattern, int lineNumber, int charStart, int charEnd, int hitCount, boolean register, Map attributes) throws DebugException {
		this(resource, stratum, sourceName, sourcePath, classNamePattern, lineNumber, charStart, charEnd, hitCount, register, attributes, STRATUM_BREAKPOINT);
	}
	
	protected JavaStratumLineBreakpoint(final IResource resource, final String stratum, final String sourceName, final String sourcePath, final String classNamePattern, final int lineNumber, final int charStart, final int charEnd, final int hitCount, final boolean register, final Map attributes, final String markerType) throws DebugException {
		IWorkspaceRunnable wr= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
	
				// create the marker
				setMarker(resource.createMarker(markerType));
				
				// modify pattern
				String pattern = classNamePattern;
				if (pattern != null && pattern.length() == 0) {
					pattern = null;
				}
				
				// add attributes
				addLineBreakpointAttributes(attributes, getModelIdentifier(), true, lineNumber, charStart, charEnd);
				addStratumPatternAndHitCount(attributes, stratum, sourceName, sourcePath, pattern, hitCount);
				// set attributes
				ensureMarker().setAttributes(attributes);
				
				register(register);
			}
		};
		run(null, wr);
	}

	/**
	 * Adds the class name pattern and hit count attributes to the gvien map.
	 */
	protected void addStratumPatternAndHitCount(Map attributes, String stratum, String sourceName, String sourcePath, String pattern, int hitCount) {
		attributes.put(PATTERN, pattern);
		attributes.put(STRATUM, stratum);
		if (sourceName != null) {
			attributes.put(SOURCE_NAME, sourceName);
		}
		if (sourcePath != null) {
			attributes.put(SOURCE_PATH, sourcePath);
		}
		if (hitCount > 0) {
			attributes.put(HIT_COUNT, new Integer(hitCount));
			attributes.put(EXPIRED, Boolean.FALSE);
		}
	}

	/**
	 * Creates the event requests to:<ul>
	 * <li>Listen to class loads related to the breakpoint</li>
	 * <li>Respond to the breakpoint being hit</li>
	 * </ul>
	 * @see org.eclipse.jdt.internal.debug.core.breakpoints.JavaBreakpoint#addToTarget(org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget)
	 */
	public void addToTarget(JDIDebugTarget target) throws CoreException {
		
		// pre-notification
		fireAdding(target);
				
		String referenceTypeName;
		try {
			referenceTypeName = getPattern();
		} catch (CoreException e) {
			JDIDebugPlugin.log(e);
			return;
		}
		
		String classPrepareTypeName= referenceTypeName;
		// create request to listen to class loads
		//name may only be partially resolved
		registerRequest(target.createClassPrepareRequest(classPrepareTypeName), target);
		
		// create breakpoint requests for each class currently loaded
		VirtualMachine vm = target.getVM();
		if (vm == null) {
			target.requestFailed(JDIDebugBreakpointMessages.getString("JavaPatternBreakpoint.Unable_to_add_breakpoint_-_VM_disconnected._1"), null); //$NON-NLS-1$
		}
		List classes = null;
		try {
			classes= vm.allClasses();
		} catch (RuntimeException e) {
			target.targetRequestFailed(JDIDebugBreakpointMessages.getString("JavaPatternBreakpoint.0"), e); //$NON-NLS-1$
		}
		if (classes != null) {
			Iterator iter = classes.iterator();
			while (iter.hasNext()) {
				ReferenceType type= (ReferenceType)iter.next();
				if (installableReferenceType(type, target)) {
					createRequest(target, type);
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.breakpoints.JavaBreakpoint#installableReferenceType(com.sun.jdi.ReferenceType, org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget)
	 */
	protected boolean installableReferenceType(ReferenceType type, JDIDebugTarget target) throws CoreException {

		// check the type name.	
		String typeName= type.name();
		if (!validType(typeName)) {
			return false;
		}
		String stratum = getStratum();
		// check the source name.
		String bpSourceName= getSourceName();
		if (bpSourceName != null) {
			List sourceNames;
			try {
				sourceNames= type.sourceNames(stratum);
			} catch (AbsentInformationException e1) {
				return false;
			}
			if (!containsMatch(sourceNames, bpSourceName)) {
				return false;
			}
		}
		
		String bpSourcePath= getSourcePath();
		if (bpSourcePath != null) {
			// check that source paths match
			List sourcePaths;
			try {
				sourcePaths= type.sourcePaths(stratum);
			} catch (AbsentInformationException e1) {
				return false;
			}
			if (!containsMatch(sourcePaths, bpSourcePath)) {
				return false;
			}
		}
		return queryInstallListeners(target, type);
	}
	
	private boolean containsMatch(List strings, String key) {
		for (Iterator iter = strings.iterator(); iter.hasNext();) {
			if (((String) iter.next()).equals(key)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param typeName
	 * @return
	 */
	private boolean validType(String typeName) throws CoreException {
		String pattern= getPattern();
		if (pattern.charAt(0) == '*') {
			if (pattern.length() == 1) {
				return true;
			}
			return typeName.endsWith(pattern.substring(1));
		}
		int length= pattern.length();
		if (pattern.charAt(length - 1) == '*') {
			return typeName.startsWith(pattern.substring(0, length - 1));
		}
		return typeName.startsWith(pattern);
	}

	/**
	 * Returns a list of locations for the given line number in the given type.
	 * Returns <code>null</code> if a location cannot be determined.
	 */
	protected List determineLocations(int lineNumber, ReferenceType type) {
		List locations;
		String sourcePath;
		try {
			locations= type.locationsOfLine(getStratum(), getSourceName(), lineNumber);
			sourcePath= getSourcePath();
		} catch (AbsentInformationException aie) {
			IStatus status= new Status(IStatus.ERROR, JDIDebugPlugin.getUniqueIdentifier(), NO_LINE_NUMBERS, JDIDebugBreakpointMessages.getString("JavaLineBreakpoint.Absent_Line_Number_Information_1"), null);  //$NON-NLS-1$
			IStatusHandler handler= DebugPlugin.getDefault().getStatusHandler(status);
			if (handler != null) {
				try {
					handler.handleStatus(status, type);
				} catch (CoreException e) {
				}
			}
			return null;
		} catch (NativeMethodException e) {
			return null;
		} catch (InvalidLineNumberException e) {
			//possibly in a nested type, will be handled when that class is loaded
			return null;
		} catch (VMDisconnectedException e) {
			return null;
		} catch (ClassNotPreparedException e) {
			// could be a nested type that is not yet loaded
			return null;
		} catch (RuntimeException e) {
			// not able to retrieve line info
			JDIDebugPlugin.log(e);
			return null;
		} catch (CoreException e) {
			// not able to retrieve line info
			JDIDebugPlugin.log(e);
			return null;
		}
		
		if (sourcePath == null) {
			if (locations.size() > 0) {
				return locations;
			}
		} else {
			for (ListIterator iter = locations.listIterator(); iter.hasNext();) {
				Location location = (Location) iter.next();
				try {
					if (!sourcePath.equals(location.sourcePath())) {
						iter.remove();
					}
				} catch (AbsentInformationException e1) {
					// nothing to do;
				}
			}
			if (locations.size() > 0) {
			    return locations;
			}
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaStratumLineBreakpoint#getPattern()
	 */
	public String getPattern() throws CoreException {
		return ensureMarker().getAttribute(PATTERN, "*"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaStratumLineBreakpoint#getSourceName()
	 */
	public String getSourceName() throws CoreException {
		return (String) ensureMarker().getAttribute(SOURCE_NAME);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaStratumLineBreakpoint#getStratum()
	 */
	public String getStratum() throws CoreException {
		return (String) ensureMarker().getAttribute(STRATUM);		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaStratumLineBreakpoint#getSourcePath()
	 */
	public String getSourcePath() throws CoreException {
		return (String) ensureMarker().getAttribute(SOURCE_PATH);
	}

}
