package org.eclipse.jdt.launching.sourcelookup;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.internal.launching.LaunchingPlugin;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;


/**
 * Locates source for a Java debug session by searching
 * a configurable set of source locations.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * <p>
 * Note: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 *
 * @see org.eclipse.debug.core.model.ISourceLocator
 */
public class JavaSourceLocator implements ISourceLocator {

	/**
	 * A collection of the source locations to search
	 */
	private IJavaSourceLocation[] fLocations;

	/**
	 * Constructs a new JavaSourceLocator that searches the
	 * specified set of source locations for source elements.
	 * 
	 * @param locations the source locations to search for
	 *  source, in the order they should be searched
	 */
	public JavaSourceLocator(IJavaSourceLocation[] locations) {
		setSourceLocations(locations);
	}
	
	/**
	 * Sets the locations that will be searched, in the order
	 * to be searched.
	 * 
	 * @param locations the locations that will be searched, in the order
	 *  to be searched
	 */
	public void setSourceLocations(IJavaSourceLocation[] locations) {
		fLocations = locations;
	}
	
	/**
	 * Returns the locations that this source locator is currently
	 * searching, in the order that they are searched.
	 * 
	 * @return the locations that this source locator is currently
	 * searching, in the order that they are searched
	 */
	public IJavaSourceLocation[] getSourceLocations() {
		return fLocations;
	}
			
	/**
	 *@see ISourceLocator#getSourceElement
	 */
	public Object getSourceElement(IStackFrame stackFrame) {
		if (stackFrame instanceof IJavaStackFrame) {
			IJavaStackFrame frame = (IJavaStackFrame)stackFrame;
			String name = null;
			try {
				String sourceName = frame.getSourceName();
				if (sourceName == null) {
					// no debug attributes, guess at source name
					name = frame.getDeclaringTypeName();
				} else {
					// build source name from debug attributes using
					// the source file name and the package of the declaring
					// type
					String declName= frame.getDeclaringTypeName();
					int index = declName.lastIndexOf('.');
					if (index >= 0) {
						name = declName.substring(0, index + 1);
					} else {
						name = ""; //$NON-NLS-1$
					}
					index = sourceName.lastIndexOf('.');
					if (index >= 0) {
						name += sourceName.substring(0, index) ;
					}					
				}
				IJavaSourceLocation[] locations = getSourceLocations();
				for (int i = 0; i < locations.length; i++) {
					Object sourceElement = locations[i].findSourceElement(name);
					if (sourceElement != null) {
						return sourceElement;
					}
				}
			} catch (CoreException e) {
				LaunchingPlugin.log(e);
			}
		}
		return null;
	}
	
	/**
	 * Adds all projects required by <code>proj</code> to the list
	 * <code>res</code>
	 * 
	 * @param proj the project for which to compute required
	 *  projects
	 * @param res the list to add all required projects too
	 */
	protected static void collectRequiredProjects(IJavaProject proj, ArrayList res) throws JavaModelException {
		if (!res.contains(proj)) {
			res.add(proj);
			
			IJavaModel model= proj.getJavaModel();
			
			IClasspathEntry[] entries= proj.getRawClasspath();
			for (int i= 0; i < entries.length; i++) {
				IClasspathEntry curr= entries[i];
				if (curr.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
					IJavaProject ref= model.getJavaProject(curr.getPath().segment(0));
					if (ref.exists()) {
						collectRequiredProjects(ref, res);
					}
				}
			}
		}
	}	
	
	/**
	 * Returns a default collection of source locations for
	 * the given Java project. Default source locations consist
	 * of the given project and all of its required projects .
	 * If the project uses an alternate runtime JRE than it has
	 * been built with, the alternate JRE source zip is added
	 * as the first source location.
	 * 
	 * @param project Java project
	 * @return a collection of source locations for all required
	 *  projects
	 * @exception CoreException if an exception occurrs reading
	 *  the classpath of the given or any required project
	 */
	public static IJavaSourceLocation[] getDefaultSourceLocations(IJavaProject project) throws CoreException {
		IVMInstall runtimeJRE = JavaRuntime.getVMInstall(project);
		ArrayList list = new ArrayList();
		collectRequiredProjects(project,list);
		int size = list.size();
		int offset = 0;
		ArchiveSourceLocation jreSource = null;
		if (runtimeJRE != null) {
			LibraryLocation library = runtimeJRE.getLibraryLocation();
			if (library == null) {
				library = runtimeJRE.getVMInstallType().getDefaultLibraryLocation(runtimeJRE.getInstallLocation());
			}
			if (library != null) {
				IPath path = library.getSystemLibrarySourcePath();
				if (!path.isEmpty()) {
					try {
						jreSource = new ArchiveSourceLocation(path.toOSString(), library.getPackageRootPath().toString());
					} catch (IOException e) {
						throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
					}
					size++;
					offset++;
				}
			}
		}
		IJavaSourceLocation[] locations = new IJavaSourceLocation[size];
		if (jreSource != null) {
			locations[0] = jreSource;
		}		
		for (int i = 0; i < list.size(); i++) {
			locations[offset] = new JavaProjectSourceLocation((IJavaProject)list.get(i));
			offset++;
		}
		return locations;
	}
}