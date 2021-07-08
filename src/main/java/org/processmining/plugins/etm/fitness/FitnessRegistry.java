package org.processmining.plugins.etm.fitness;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.etm.fitness.metrics.FitnessReplay;
import org.processmining.plugins.etm.fitness.metrics.Generalization;
import org.processmining.plugins.etm.fitness.metrics.PrecisionEscEdges;
import org.processmining.plugins.etm.fitness.metrics.SimplicityMixed;
import org.processmining.plugins.etm.fitness.metrics.SimplicityUselessNodes;

/**
 * This class gets all known fitness metrics using the {@link
 * @FitnessAnnotation} annotation from the ProM framework and then allows
 * certain queries to be run on it. It is recommended to instantiate this class
 * as few times as possible since at each instantiation it needs to load the
 * list of fitness metrics, which will not chance during runtime.
 * 
 * @author jbuijs
 * 
 */
//FIXME check all class contents
public class FitnessRegistry {

	private final PluginContext context;

	private List<TreeFitnessInfo> knownInfos;

	public TreeFitnessInfo[] knownInfosArray;

	/**
	 * A STATIC (!!!) list of tree fitness infos that are always known, since
	 * they are implemented and part of the Evolutionary Tree Miner Package.
	 */
	public static TreeFitnessInfo[] defaultKnownInfos = new TreeFitnessInfo[] { FitnessReplay.info,
			PrecisionEscEdges.info, Generalization.info, SimplicityMixed.info, SimplicityUselessNodes.info };

	/**
	 * Instantiates the registry of all known {@link TreeFitnessAbstract}
	 * instances.
	 * 
	 * @param context
	 */
	public FitnessRegistry(final PluginContext context) {
		this.context = context;
		getAllMetricInfos();
	}

	/**
	 * Returns the list of all metric instances
	 * 
	 * @return
	 */
	public List<TreeFitnessInfo> getAllMetricInfos() {
		if (knownInfos == null) {
			Set<Class<?>> coverageEstimatorClasses = context.getPluginManager().getKnownClassesAnnotatedWith(
					FitnessAnnotation.class);
			knownInfos = new LinkedList<TreeFitnessInfo>();
			if (coverageEstimatorClasses != null) {
				for (Class<?> coverClass : coverageEstimatorClasses) {

					try {
						java.lang.reflect.Field field = coverClass.getDeclaredField("info");
						Object obj = field.get(null);
						if (obj instanceof TreeFitnessInfo) {
							TreeFitnessInfo info = (TreeFitnessInfo) obj;
							knownInfos.add(info);
						}
					} catch (Exception e) {
						//Catch and ignore all exceptions to be resistant to external faults. 
						//e.printStackTrace();
					}
				}
			}

			knownInfosArray = new TreeFitnessInfo[knownInfos.size()];
			knownInfos.toArray(knownInfosArray);
		}

		return knownInfos;
	}

	public TreeFitnessInfo[] getAllMetricInfosAsArray() {
		getAllMetricInfos();
		return knownInfosArray;
	}

	/**
	 * Returns the {@link TreeFitnessInfo} instance with the provided coded or
	 * NULL if no such info exists.
	 * 
	 * @param code
	 * @return
	 */
	public TreeFitnessInfo getTreeFitnessInfoByCode(String code) {
		return getTreeFitnessInfoByCode(code, knownInfosArray);
	}

	public static TreeFitnessInfo getTreeFitnessInfoByCode(String code, TreeFitnessInfo[] infos) {
		for (TreeFitnessInfo info : infos) {
			if (info.getCode().equalsIgnoreCase(code)) {
				return info;
			}
		}

		return null;
	}

}
