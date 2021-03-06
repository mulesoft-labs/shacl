package org.topbraid.shacl.arq.functions;

import java.net.URI;
import java.util.Collections;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.DatasetImpl;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.engine.ShapesGraph;
import org.topbraid.shacl.util.FailureLog;
import org.topbraid.shacl.validation.DefaultShapesGraphProvider;
import org.topbraid.shacl.validation.ValidationEngineFactory;
import org.topbraid.shacl.validation.sparql.AbstractSPARQLExecutor;
import org.topbraid.shacl.vocabulary.DASH;
import org.topbraid.shacl.vocabulary.SH;
import org.topbraid.spin.arq.AbstractFunction3;
import org.topbraid.spin.util.JenaDatatypes;

/**
 * The implementation of the tosh:hasShape function.
 * 
 * @author Holger Knublauch
 */
public class HasShapeFunction extends AbstractFunction3 {
	
	private static ThreadLocal<Boolean> recursionIsErrorFlag = new ThreadLocal<Boolean>();
	
	private static ThreadLocal<Model> resultsModelTL = new ThreadLocal<>();
	
	private static ThreadLocal<URI> shapesGraph = new ThreadLocal<URI>();
	
	public static Model getResultsModel() {
		return resultsModelTL.get();
	}
	
	public static URI getShapesGraph() {
		return shapesGraph.get();
	}
	
	public static void setResultsModel(Model value) {
		resultsModelTL.set(value);
	}
	
	public static void setShapesGraph(URI uri) {
		shapesGraph.set(uri);
	}

	
	@Override
	protected NodeValue exec(Node focusNode, Node shapeNode, Node recursionIsError, FunctionEnv env) {

		Boolean oldFlag = recursionIsErrorFlag.get();
		if(JenaDatatypes.TRUE.asNode().equals(recursionIsError)) {
			recursionIsErrorFlag.set(true);
		}
		try {
			if(SHACLRecursionGuard.start(focusNode, shapeNode)) {
				if(JenaDatatypes.TRUE.asNode().equals(recursionIsError) || (oldFlag != null && oldFlag)) {
					String message = "Unsupported recursion";
					Model resultsModel = resultsModelTL.get();
					Resource failure = resultsModel.createResource(DASH.FailureResult);
					failure.addProperty(SH.resultMessage, message);
					failure.addProperty(SH.focusNode, resultsModel.asRDFNode(focusNode));
					failure.addProperty(SH.sourceShape, resultsModel.asRDFNode(shapeNode));
					FailureLog.get().logFailure(message);
					throw new ExprEvalException("Unsupported recursion");
				}
				else {
					SHACLRecursionGuard.end(focusNode, shapeNode);
					return NodeValue.TRUE;
				}
			}
			else {
				
				try {
					Model model = ModelFactory.createModelForGraph(env.getActiveGraph());
					RDFNode resource = model.asRDFNode(focusNode);
					Dataset dataset = DatasetImpl.wrap(env.getDataset());
					Resource shape = (Resource) dataset.getDefaultModel().asRDFNode(shapeNode);
					Model results = doRun(resource, shape, dataset);
					if(resultsModelTL.get() != null) {
						resultsModelTL.get().add(results);
					}
					if(results.contains(null, RDF.type, DASH.FailureResult)) {
						throw new ExprEvalException("Propagating failure from nested shapes");
					}

					if(AbstractSPARQLExecutor.createDetails) {
						boolean result = true;
						for(Resource r : results.listSubjectsWithProperty(RDF.type, SH.ValidationResult).toList()) {
							if(!results.contains(null, SH.detail, r)) {
								result = false;
								break;
							}
						}
						return NodeValue.makeBoolean(result);
					}
					else {
						boolean result = !results.contains(null, RDF.type, SH.ValidationResult);
						return NodeValue.makeBoolean(result);
					}
				}
				finally {
					SHACLRecursionGuard.end(focusNode, shapeNode);
				}
			}
		}
		finally {
			recursionIsErrorFlag.set(oldFlag);
		}
	}


	private Model doRun(RDFNode focusNode, Resource shape, Dataset dataset) {
		URI shapesGraphURI = shapesGraph.get();
		if(shapesGraphURI == null) {
			shapesGraphURI = DefaultShapesGraphProvider.get().getDefaultShapesGraphURI(dataset);
		}
		Model shapesModel = dataset.getNamedModel(shapesGraphURI.toString());
		ShapesGraph vsg = new ShapesGraph(shapesModel);
		return ValidationEngineFactory.get().create(dataset, shapesGraphURI, vsg, null).validateNodesAgainstShape(
				Collections.singletonList(focusNode), shape.asNode()).getModel();
	}
}
