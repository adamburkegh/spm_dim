package org.processmining.plugins.etm.fitness;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.framework.plugin.annotations.KeepInProMCache;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

import com.fluxicon.slickerbox.components.RoundedPanel;

/**
 * All fitness metrics should extend this abstract Tree Fitness class. They also
 * should either (i) define no constructors or (ii) if they define a constructor
 * also define the public empty constructor that should not fail. If they do
 * this than any fitness metric, even outside this package and currently
 * unknown, will be able to be used by the ETM if ProM has loaded it correctly!
 * 
 * @author jbuijs
 * 
 */
@KeepInProMCache
@FitnessAnnotation
public abstract class TreeFitnessAbstract implements FitnessEvaluator<ProbProcessArrayTree> {

	//jbuijs suggested improvement: make normalized or absolute value required for all fitness 
	// evaluators! e.g. field in this class with methods

	private static Logger LOGGER = LogManager.getLogger();
	
	/**
	 * Each individual fitness metric should define a STATIC public info field
	 */
	public static TreeFitnessInfo info;

	/*-
	public CentralRegistry registry;

	public TreeFitnessAbstract(CentralRegistry registry) {
		this.registry = registry;
	}/**/

	/**
	 * Wrapper around the {@link TreeFitnessInfo#isNatural()} for compliance
	 * with Watchmaker's {@link FitnessEvaluator} interface
	 */
	public boolean isNatural() {
		return getInfo().isNatural();
	}

	/**
	 * Returns the STATIC info for the metric instance
	 * 
	 * @return TreeFitnessInfo Info for the metric
	 * 
	 */
	public abstract TreeFitnessInfo getInfo();

	public static TreeFitnessAbstract deepClone(TreeFitnessAbstract original) {
		return deepClone(original, null);
	}

	/**
	 * This method deeply clones the provided TreeFitnessAbstract. It will
	 * automatically call the copy constructor for the TreeFitnessAbstract
	 * implementing class with the original instance as a parameter. Iff the
	 * original TreeFitnessAbstract indicated that it depends on other
	 * TreeFitnessAbstract instances then a copy-constructor is searched and
	 * called that takes as input parameters of the original instance and the
	 * dependent TreeFitnessAbstract instances, in the order specified in the
	 * TreeFitnessInfo object. The correct cloned instance is taken from the
	 * clonedMap to be provided to this constructor (e.g. the deep clone is
	 * linking to an already cloned instance of that TreeFitnessAbstract class).
	 * NOTE: this map should be maintained OUTSIDE this call. For an example
	 * implementation see the {@link OverallFitness} copy constructor.
	 * 
	 * @param original
	 * @param clonedMap
	 * @return
	 */
	public static TreeFitnessAbstract deepClone(TreeFitnessAbstract original,
			HashMap<Class<TreeFitnessAbstract>, TreeFitnessAbstract> clonedMap) {
		//Don't try the constructor without the original attribute, code becomes messy
		//TODO reduce number of catch statements, smarter structure/cleaner code should be possible

		Class<? extends TreeFitnessAbstract>[] dependsOn = original.getInfo().getDependsOn();

		//FROM: http://stackoverflow.com/questions/7635313/java-class-dynamically-with-constructor-parameter

		@SuppressWarnings("unchecked")
		//Get the class of the original to be cloned
		Class<TreeFitnessAbstract> _tempClass = (Class<TreeFitnessAbstract>) original.getClass();

		//Now find the constructor
		Constructor<TreeFitnessAbstract> ctor = null;
		try {
			//If the class does not depend on anything or if we don't have any map then just call the copy constructor
			if (dependsOn.length == 0 || clonedMap == null || clonedMap.isEmpty()) {
				ctor = _tempClass.getDeclaredConstructor(_tempClass);
				return ctor.newInstance(original);
			} else {
				//We need to find a constructor that also takes the classes it depends on
				//We need to manually check since we can not instantiate a generic array of Class<TreeFitnessAbstract>
				Constructor<?>[] constructors = _tempClass.getDeclaredConstructors();
				for (int i = 0; i < constructors.length; i++) {
					Constructor<?> c = constructors[i];
					//The current constructor is correct until proven otherwise
					boolean correct = true;
					Type[] params = c.getGenericParameterTypes();
					if (params.length == dependsOn.length + 1) {
						for (int j = 0; j < params.length; j++) {
							@SuppressWarnings("unchecked")
							Class<TreeFitnessAbstract> clazz = (Class<TreeFitnessAbstract>) params[j];
							if (j == 0) {
								if (!clazz.equals(_tempClass)) {
									correct = false;
								}
							} else {
								if (!clazz.equals(dependsOn[j - 1])) {
									correct = false;
								}
							}

						}
					} else
						correct = false;

					//Now call that constructor with the correct arguments
					if (correct) {
						TreeFitnessAbstract[] args = new TreeFitnessAbstract[dependsOn.length + 1];
						args[0] = original;
						for (int k = 0; k < dependsOn.length; k++) {
							args[k + 1] = clonedMap.get(dependsOn[k]);
						}
						return (TreeFitnessAbstract) c.newInstance((Object[])args);
					}
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchMethodException e) {
			//Constructor we tried does not exist, try the 'empty' construtor as fall-back
			try {
				ctor = _tempClass.getConstructor();
				return ctor.newInstance();
			} catch (SecurityException e1) {
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				e.printStackTrace();
			} catch (InstantiationException e1) {
				e.printStackTrace();
			} catch (IllegalAccessException e1) {
				e.printStackTrace();
			} catch (InvocationTargetException e1) {
				e.printStackTrace();
			}
			e.printStackTrace();
			return null;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static TreeFitnessAbstract instantiateTreeFitness(CentralRegistry registry,
			Class<TreeFitnessAbstract> desiredClass) {
		//Now find the constructor
		Constructor<TreeFitnessAbstract> ctor = null;

		try {
			//First try to find the constructor that takes a CentralRegistry instance
			//ctor = desiredClass.getDeclaredConstructor(registry.getClass());
			ctor = desiredClass.getDeclaredConstructor(CentralRegistry.class);
			return ctor.newInstance(registry);
		} catch (NoSuchMethodException e) {
			//If that constructor does not exist, try again with the empty constructor
			try {
				ctor = desiredClass.getDeclaredConstructor();
				return ctor.newInstance();
			} catch (Exception e1) {
				LOGGER.error("Couldn't instantiate class with the registry or an empty construtor",e1);
				throw new RuntimeException("Couldn't instantiate class with the registry or an empty construtor");
			}
		} catch (Exception e) {
			LOGGER.error("Error instantiating TreeFitness",e);
			throw new RuntimeException("Error instantiating TreeFitness",e);
		}
	}

	/**
	 * Returns a GUI settings instance for this fitness metric. Metrics that
	 * provide their own GUI should overwrite this method and return their
	 * customized GUI that extends the abstract class.
	 * 
	 * @param param
	 * @return
	 */
	public static TreeFitnessGUISettingsAbstract<? extends TreeFitnessAbstract> getGUISettingsPanel(
			ETMParamAbstract param) {
		return new TreeFitnessGUISettingsEmpty<TreeFitnessAbstract>(param);
	}

	/**
	 * An abstract instance of a GUI interface to set up a {@link TreeFitness}
	 * metric. A metric that requires additional user input, should declare a
	 * (local) public class extending this abstract class and overwrite the
	 * static function getGUISettingsPanel(ETMParam param) from this class and
	 * return their declared GUI instance.
	 * 
	 * @author jbuijs
	 * 
	 */
	public static abstract class TreeFitnessGUISettingsAbstract<F extends TreeFitnessAbstract> extends RoundedPanel {

		private static final long serialVersionUID = 1L;

		protected ETMParamAbstract param;

		protected boolean providesGUI = true;

		public TreeFitnessGUISettingsAbstract(ETMParamAbstract param) {
			this.param = param;
		}

		@SuppressWarnings("unchecked")
		public F getTreeFitnessInstance(CentralRegistry registry, Class<TreeFitnessAbstract> clazz) {
			return (F) TreeFitnessAbstract.instantiateTreeFitness(registry, clazz);
		}

		//public abstract F getTreeFitnessInstance(ETMParamAbstract param);

		/**
		 * A boolean that indicates whether a GUI is actually build for this
		 * TreeFitnessInfo
		 * 
		 * @return
		 */
		public boolean providesGUI() {
			return providesGUI;
		}

		/**
		 * Initialize the GUI when an instance is already available (f.i.
		 * default/suggested settings etc.)
		 * 
		 * @param instance
		 */
		public abstract void init(F instance);
	}

	/**
	 * The 'empty' GUI that is automatically used for fitness metrics that
	 * provide no provide GUI. These metrics must have an empty constructor or a
	 * constructor that only requires a {@link CentralRegistry} instance, and
	 * hence don't require a GUI
	 * 
	 * @author jbuijs
	 * 
	 */
	public static class TreeFitnessGUISettingsEmpty<F extends TreeFitnessAbstract> extends
			TreeFitnessGUISettingsAbstract<F> {

		private static final long serialVersionUID = 1L;

		public TreeFitnessGUISettingsEmpty(ETMParamAbstract param) {
			super(param);
			providesGUI = false;
		}

		public void init(F instance) {
			//Do Nothing :)
		}
	}
}
