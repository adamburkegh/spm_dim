package org.processmining.plugins.etm.mutation;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.processmining.framework.plugin.PluginContext;

/**
 * This class gets all known fitness metrics using the {@link
 * @TreeMutationAnnotation} annotation from the ProM framework and then allows
 * certain queries to be run on it. It is recommended to instantiate this class
 * as few times as possible since at each instantiation it needs to load the
 * list of fitness metrics, which will not chance during runtime.
 * 
 * @author jbuijs
 * 
 */
public class TreeMutationRegistry {

	private final PluginContext context;

	private List<TreeMutationInfo> knownInfos;

	/**
	 * Instantiates the registry of all known {@link TreeMutationAnnotation}
	 * instances.
	 * 
	 * @param context
	 */
	public TreeMutationRegistry(final PluginContext context) {
		this.context = context;
		getAllMetricInfos();
	}

	/**
	 * Returns the list of all metric instances
	 * 
	 * @return
	 */
	public List<TreeMutationInfo> getAllMetricInfos() {
		if (knownInfos == null) {
			Set<Class<?>> coverageEstimatorClasses = context.getPluginManager().getKnownClassesAnnotatedWith(
					TreeMutationAnnotation.class);
			knownInfos = new LinkedList<TreeMutationInfo>();
			if (coverageEstimatorClasses != null) {
				for (Class<?> coverClass : coverageEstimatorClasses) {

					try {
						java.lang.reflect.Field field = coverClass.getDeclaredField("info");
						Object obj = field.get(null);
						if (obj instanceof TreeMutationInfo) {
							TreeMutationInfo info = (TreeMutationInfo) obj;
							knownInfos.add(info);
						}
					} catch (Exception e) {
						//Catch and ignore all exceptions to be resistant to external faults. 
						//e.printStackTrace();
					} 
				}

			}
		}

		return knownInfos;
	}

	/**
	 * Returns the {@link TreeFitnessInfo} instance with the provided coded or
	 * NULL if no such info exists.
	 * 
	 * @param code
	 * @return
	 */
	public TreeMutationInfo getTreeFitnessInfoByCode(String code) {
		for (TreeMutationInfo info : knownInfos) {
			if (info.getCode().equalsIgnoreCase(code)) {
				return info;
			}
		}

		return null;
	}

}
