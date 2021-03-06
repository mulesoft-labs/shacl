/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package org.topbraid.spin.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.Command;
import org.topbraid.spin.model.Module;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.print.PrintContext;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.util.ModulesUtil;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;


public class ModuleImpl extends AbstractSPINResourceImpl implements Module {
	
	
	public ModuleImpl(Node node, EnhGraph eg) {
		super(node, eg);
	}

	
	@Override
    public List<Argument> getArguments(boolean ordered) {
		List<Argument> results = new ArrayList<Argument>();
		StmtIterator it = null;
		JenaUtil.setGraphReadOptimization(true);
		try {
			Set<Resource> classes = JenaUtil.getAllSuperClasses(this);
			classes.add(this);
			for(Resource cls : classes) {
				it = cls.listProperties(SPIN.constraint);
				while(it.hasNext()) {
					Statement s = it.nextStatement();
					addArgumentFromConstraint(s, results);
				}
			}
		}
		finally {
			if (it != null) {
				it.close();
			}
			JenaUtil.setGraphReadOptimization(false);
		}
		
		if(ordered) {
			Collections.sort(results, new Comparator<Argument>() {
				@Override
                public int compare(Argument o1, Argument o2) {
					Property p1 = o1.getPredicate();
					Property p2 = o2.getPredicate();
					if(p1 != null && p2 != null) {
						return p1.getLocalName().compareTo(p2.getLocalName());
					}
					else {
						return 0;
					}
				}
			});
		}
		
		return results;
	}

	/**
	 * 
	 * @param constaint is a statement whose subject is a class, and whose predicate is SPIN.constraint
	 * @param results
	 */
	private void addArgumentFromConstraint(Statement constaint, List<Argument> results) {
		if(constaint.getObject().isAnon()) {
			// Optimized case to avoid walking up class hierarchy
			StmtIterator types = constaint.getResource().listProperties(RDF.type);
			while(types.hasNext()) {
				Statement typeS = types.next();
				if(typeS.getObject().isURIResource()) {
					if(SPL.Argument.equals(typeS.getObject())) {
						results.add(constaint.getResource().as(Argument.class));
					}
					else if(!SPL.Attribute.equals(typeS.getObject())) {
						if(JenaUtil.hasSuperClass(typeS.getResource(), SPL.Argument.inModel(typeS.getModel()))) {
							results.add(constaint.getResource().as(Argument.class));
						}
					}
				}
			}
		}
		else if(constaint.getObject().isURIResource() && JenaUtil.hasIndirectType(constaint.getResource(), SPL.Argument.inModel(constaint.getModel()))) {
			results.add(constaint.getResource().as(Argument.class));
		}
	}


	@Override
    public Map<String, Argument> getArgumentsMap() {
		Map<String,Argument> results = new HashMap<String,Argument>();
		for(Argument argument : getArguments(false)) {
			Property property = argument.getPredicate();
			if(property != null) {
				results.put(property.getLocalName(), argument);
			}
		}
		return results;
	}


	@Override
    public Command getBody() {
		RDFNode node = ModulesUtil.getBody(this);
		if(node instanceof Resource) {
			return SPINFactory.asCommand((Resource)node);
		}
		else {
			return null;
		}
	}
	
	
	@Override
    public String getComment() {
		return getString(RDFS.comment);
	}


	@Override
    public boolean isAbstract() {
		return SPINFactory.isAbstract(this);
	}


	@Override
    public void print(PrintContext p) {
		// TODO Auto-generated method stub

	}
}
