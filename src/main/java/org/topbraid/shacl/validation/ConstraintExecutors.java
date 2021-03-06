package org.topbraid.shacl.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.topbraid.shacl.engine.Constraint;
import org.topbraid.shacl.validation.js.JSConstraintExecutor;
import org.topbraid.shacl.validation.js.JSValidationLanguage;
import org.topbraid.shacl.validation.sparql.SPARQLConstraintExecutor;
import org.topbraid.shacl.validation.sparql.SPARQLValidationLanguage;
import org.topbraid.shacl.vocabulary.DASH;
import org.topbraid.shacl.vocabulary.SH;

/**
 * Singleton managing the available ValidationLanguage instances.
 * 
 * @author Holger Knublauch
 */
public class ConstraintExecutors {

	private final static ConstraintExecutors singleton = new ConstraintExecutors();
	
	public static ConstraintExecutors get() {
		return singleton;
	}
	
	private List<ValidationLanguage> languages = new ArrayList<>();
	
	private Map<Resource,SpecialConstraintExecutorFactory> specialExecutors = new HashMap<>();

	
	public ConstraintExecutors() {
		addSpecialExecutor(SH.PropertyConstraintComponent, new AbstractSpecialConstraintExecutorFactory() {
			@Override
			public ConstraintExecutor create(Constraint constraint) {
				return new PropertyConstraintExecutor();
			}
		});
		addSpecialExecutor(DASH.ParameterConstraintComponent, new AbstractSpecialConstraintExecutorFactory() {
			@Override
			public ConstraintExecutor create(Constraint constraint) {
				return new PropertyConstraintExecutor();
			}
		});
		addSpecialExecutor(SH.JSConstraintComponent, new AbstractSpecialConstraintExecutorFactory() {
			@Override
			public ConstraintExecutor create(Constraint constraint) {
				return new JSConstraintExecutor();
			}
		});
		addSpecialExecutor(SH.SPARQLConstraintComponent, new AbstractSpecialConstraintExecutorFactory() {
			@Override
			public ConstraintExecutor create(Constraint constraint) {
				return new SPARQLConstraintExecutor(constraint);
			}
		});
		addSpecialExecutor(SH.ExpressionConstraintComponent, new AbstractSpecialConstraintExecutorFactory() {
			@Override
			public ConstraintExecutor create(Constraint constraint) {
				return new ExpressionConstraintExecutor();
			}
		});
		
		addLanguage(SPARQLValidationLanguage.get());
		addLanguage(JSValidationLanguage.get());
	}
	
	
	protected void addLanguage(ValidationLanguage language) {
		languages.add(language);
	}
	
	
	public void addSpecialExecutor(Resource constraintComponent, SpecialConstraintExecutorFactory executor) {
		specialExecutors.put(constraintComponent, executor);
	}
	
	
	public ConstraintExecutor getExecutor(Constraint constraint, ValidationEngine engine) {

		SpecialConstraintExecutorFactory special = specialExecutors.get(constraint.getComponent());
		if(special != null && special.canExecute(constraint, engine)) {
			return special.create(constraint);
		}
		
		for(ValidationLanguage language : languages) {
			if(language.canExecute(constraint, engine)) {
				return language.createExecutor(constraint, engine);
			}
		}

		return null;
	}
	
	
	/**
	 * Can be used to make the JavaScript engine the preferred implementation over SPARQL.
	 * By default, SPARQL is preferred.
	 * In cases where a constraint component has multiple validators, it would then chose
	 * the JavaScript one.
	 * @param value  true to make JS
	 */
	public void setJSPreferred(boolean value) {
		languages.remove(0);
		languages.remove(0);
		if(value) {
			languages.add(0, JSValidationLanguage.get());
			languages.add(1, SPARQLValidationLanguage.get());
		}
		else {
			languages.add(0, SPARQLValidationLanguage.get());
			languages.add(1, JSValidationLanguage.get());
		}
	}
}
