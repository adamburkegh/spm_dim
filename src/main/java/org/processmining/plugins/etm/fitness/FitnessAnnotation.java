package org.processmining.plugins.etm.fitness;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes that have this annotation should extend (a subclass of) the
 * {@link TreeFitnessAbstract} class. This allows f.i. the ETM GUI to include,
 * yet unknown, and externally defined fitness metrics to be shown and used.
 * 
 * @author jbuijs
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface FitnessAnnotation {
	//TODO check if we need more information
}
