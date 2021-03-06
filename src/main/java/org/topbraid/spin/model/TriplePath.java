/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package org.topbraid.spin.model;

import org.apache.jena.rdf.model.RDFNode;


/**
 * A triple path element.
 * 
 * @author Holger Knublauch
 */
public interface TriplePath extends Element {

	/**
	 * Gets the subject.
	 * @return the subject
	 */
	RDFNode getSubject();
	
	
	/**
	 * Gets the object.
	 * @return the object
	 */
	RDFNode getObject();
}
