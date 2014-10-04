package it.unitn.limosine.util;

import it.unitn.limosine.types.syntax.ConstituencyTree;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.uimafit.util.JCasUtil;

public class JCasUtility {
	
	public static final String UIMA_BUILTIN_JCAS_PREFIX = "org.apache.uima.jcas.";
	
	/***
	 * Inspired by UimaFit util 
	 * Get annotation list of Class type that spans a given context
	 * @param cas
	 * @param type
	 * @param context
	 * @return
	 */
	public static List<AnnotationFS> selectCovered(JCas cas, Class<?> type, Annotation context) {
		List<AnnotationFS> list = new ArrayList<AnnotationFS>();
		FSIterator<Annotation> it = cas.getAnnotationIndex(getType(cas,type.getName())).iterator();

		int begin = context.getBegin();
		int end = context.getEnd();
		
		// Skip annotations whose start is before the start parameter.
		while (it.isValid() && (it.get()).getBegin() < begin) {
			it.moveToNext();
		}

		boolean strict = true;
		while (it.isValid()) {
			AnnotationFS a = it.get();
			// If the start of the current annotation is past the end parameter, we're done.
			if (a.getBegin() > end) {
				break;
			}
			it.moveToNext();
			if (strict && a.getEnd() > end) {
				continue;
			}

			assert (a.getBegin() >= begin) : "Illegal begin " + a.getBegin() + " in [" + begin
					+ ".." + end + "]";

			assert (a.getEnd() <= end) : "Illegal end " + a.getEnd() + " in [" + begin + ".."
					+ end + "]";

			list.add(a);
		}

		return list;
		
	}
	
	/**
	 * Get the CAS type for the given name.
	 * -- method taken from Uimafit
	 * @param aCas
	 *            the CAS hosting the type system.
	 * @param aTypename
	 *            the fully qualified type name.
	 * @return the CAS type.
	 */
	public static Type getType(JCas aCas, String aTypename) {
		String typeName = aTypename;
		if (typeName.startsWith(UIMA_BUILTIN_JCAS_PREFIX)) {
			typeName = "uima." + typeName.substring(UIMA_BUILTIN_JCAS_PREFIX.length());
		}
		final Type type = aCas.getTypeSystem().getType(typeName);
		if (type == null) {
			throw new IllegalArgumentException("Undeclared type [" + aTypename + "]");
		}
		return type;
	}
	
	public static String mergeRawParses(JCas cas) {
		String fullTree = "";
		
		for(ConstituencyTree tree : JCasUtil.select(cas, ConstituencyTree.class)) {
			fullTree += " " + tree.getRawParse();
		}
		
		return "(ROOT " + fullTree.trim() + ")";
	}

}
