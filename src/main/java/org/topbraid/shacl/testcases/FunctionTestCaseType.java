/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 */
package org.topbraid.shacl.testcases;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.ARQ;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.topbraid.shacl.testcases.context.JSPreferredTestCaseContext;
import org.topbraid.shacl.testcases.context.SPARQLPreferredTestCaseContext;
import org.topbraid.shacl.testcases.context.TestCaseContext;
import org.topbraid.shacl.testcases.context.TestCaseContextFactory;
import org.topbraid.shacl.vocabulary.DASH;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.arq.SPINThreadFunctionRegistry;
import org.topbraid.spin.arq.SPINThreadFunctions;
import org.topbraid.spin.util.JenaUtil;

public class FunctionTestCaseType implements TestCaseType {
	
	private static List<TestCaseContextFactory> contextFactories = new LinkedList<>();
	static {
		registerContextFactory(SPARQLPreferredTestCaseContext.getTestCaseContextFactory());
		registerContextFactory(JSPreferredTestCaseContext.getTestCaseContextFactory());
	}
	
	public static void registerContextFactory(TestCaseContextFactory factory) {
		contextFactories.add(factory);
	}


	@Override
	public Collection<TestCase> getTestCases(Model model, Resource graph) {
		List<TestCase> results = new LinkedList<TestCase>();
		for(Resource resource : JenaUtil.getAllInstances(model.getResource(DASH.FunctionTestCase.getURI()))) {
			results.add(new FunctionTestCase(graph, resource));
		}
		return results;
	}

	
	private static class FunctionTestCase extends TestCase {
		
		FunctionTestCase(Resource graph, Resource resource) {
			super(graph, resource);
		}

		
		@Override
		public void run(Model results) {
			Resource testCase = getResource();
			
			FunctionRegistry oldFR = FunctionRegistry.get();
			SPINThreadFunctionRegistry threadFR = new SPINThreadFunctionRegistry(oldFR);
			FunctionRegistry.set(ARQ.getContext(), threadFR);
			SPINThreadFunctions old = SPINThreadFunctionRegistry.register(testCase.getModel());

			try {
				for(TestCaseContextFactory contextFactory : contextFactories) {
					TestCaseContext context = contextFactory.createContext();
					String expression = JenaUtil.getStringProperty(testCase, DASH.expression);
					Statement expectedResultS = testCase.getProperty(DASH.expectedResult);
					String queryString = "SELECT (" + expression + " AS ?result) WHERE {}";
					Query query = ARQFactory.get().createQuery(testCase.getModel(), queryString);
					context.setUpTestContext();
					try(QueryExecution qexec = ARQFactory.get().createQueryExecution(query, testCase.getModel())) {
					    ResultSet rs = qexec.execSelect();
					    if(!rs.hasNext()) {
					        if(expectedResultS != null) {
					            createFailure(results,
					                          "Expression returned no result, but expected: " + expectedResultS.getObject(),
					                          context);
					            return;
					        }
					    }
					    else {
					        RDFNode result = rs.next().get("result");
					        if(expectedResultS == null) {
					            if(result != null) {
					                createFailure(results,
					                              "Expression returned a result, but none expected: " + result, context);
					                return;
					            }
					        }
					        else if(!expectedResultS.getObject().equals(result)) {
					            createFailure(results,
					                          "Mismatching result. Expected: " + expectedResultS.getObject() + ". Found: " + result, context);
					            return;
					        }
					    }
					}
					finally {
						context.tearDownTestContext();
					}
				}
			}
			finally {
				SPINThreadFunctionRegistry.unregister(old);
				FunctionRegistry.set(ARQ.getContext(), oldFR);
			}
			
			createResult(results, DASH.SuccessTestCaseResult);
		}
	}
}
