package org.processmining.plugins.etm.mutation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes that have this annotation should extend (a subclass of) the
 * {@link TreeMutationAbstract} class. This allows f.i. the ETM GUI to include,
 * yet unknown, and externally defined tree mutators to be shown and used.
 * 
 * @author jbuijs
 * 
 * Probably not needed - burkeat
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface TreeMutationAnnotation {
	//TODO check if we need more information
}
