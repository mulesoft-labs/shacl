package org.topbraid.spin.util;

import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementExists;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementNotExists;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.jena.update.Update;

/**
 * Utility to checks whether a given Query contains "nested" elements such as UNIONs or sub-SELECTs.
 * 
 * @author Holger Knublauch
 */
public class NestedQueries {
	
	/**
	 * Checks if a given query uses any non-trivial blocks such as UNIONs, nested { ... }
	 * or GRAPH blocks.
	 * Future versions may limit this check to cases where the variable ?this is used anywhere.
	 * @param queryPattern  the WHERE clause of the Query to check
	 * @return true if nested blocks exist
	 */
	public static boolean hasNestedBlocksUsingThis(Element queryPattern) {
		final boolean[] result = { false };
		ElementVisitor visitor = new ElementVisitorBase() {

			@Override
			public void visit(ElementExists el) {
				result[0] = true;
			}

			@Override
			public void visit(ElementGroup el) {
				if(queryPattern != el) {
					result[0] = true;
				}
			}

			@Override
			public void visit(ElementNamedGraph el) {
				result[0] = true;
			}

			@Override
			public void visit(ElementNotExists el) {
				result[0] = true;
			}

			@Override
			public void visit(ElementService el) {
				result[0] = true;
			}

			@Override
			public void visit(ElementSubQuery el) {
				result[0] = true;
			}

			@Override
			public void visit(ElementUnion el) {
				result[0] = true;
			}
		};
		ElementWalker.walk(queryPattern, visitor);
		return result[0];
	}

	
	public static boolean hasNestedBlocksUsingThis(Update update) {
		if(update instanceof UpdateModify) {
			return hasNestedBlocksUsingThis(((UpdateModify)update).getWherePattern());
		}
		else {
			return false;
		}
	}
}
