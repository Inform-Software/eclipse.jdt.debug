package org.eclipse.jdt.internal.debug.eval.ast.instructions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.debug.eval.model.IObject;
 
/**
 * Pushes the 'null' onto the stack.
 */
public class PushNull extends SimpleInstruction {
	
	public void execute() {
		pushNullValue();
	}

	public String toString() {
		return "push 'null'";
	}
}

