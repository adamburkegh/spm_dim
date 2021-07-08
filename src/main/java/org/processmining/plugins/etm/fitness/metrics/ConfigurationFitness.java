package org.processmining.plugins.etm.fitness.metrics;

import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JLabel;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.widgets.ProMScrollContainer;
import org.processmining.framework.util.ui.widgets.ProMScrollContainerChild;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.framework.util.ui.widgets.ProMTitledScrollContainerChild;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.fitness.FitnessRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;

import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.factory.SlickerFactory;

public class ConfigurationFitness extends TreeFitnessAbstract {

	//jbuijs suggestion - implement configuration caching

	private CentralRegistryConfigurable registry;

	private TreeFitnessAbstract[] logFitnessList;

	private double alpha;

	private int[] logSizes;

	private int totalTraces;

	/**
	 * Whether the configuration fitness is absolute (f.i. nr. of config
	 * settings) or relative (fraction of config settings).
	 */
	private boolean absoluteCount = true;

	//FIXME change true/false depending on relative/absolute!!!
	//E.g. absolute => false
	@SuppressWarnings("unchecked")
	public static final TreeFitnessInfo info = new TreeFitnessInfo(ConfigurationFitness.class, "Cf",
			"Configuration Fitness", "This fitness metric calls a fitness metric for each log, "
					+ "aggregates these values and combines this value with the "
					+ "configuration fitness of the process tree.", Dimension.META, false);

	public ConfigurationFitness(ConfigurationFitness original) {
		this.registry = original.registry;
		this.alpha = original.alpha;
		this.absoluteCount = original.absoluteCount;
		this.logSizes = original.logSizes.clone();
		this.totalTraces = original.totalTraces;

		this.logFitnessList = new TreeFitnessAbstract[original.logFitnessList.length];
		for (int i = 0; i < logFitnessList.length; i++) {
			logFitnessList[i] = TreeFitnessAbstract.deepClone(original.logFitnessList[i]);
		}
	}

	/**
	 * Constructor for the configuration fitness evaluator. Note that this
	 * constructor expects exactly one OverallFitness per event log, in the
	 * correct order.
	 * 
	 * @param registry
	 *            The central registry containing information about the event
	 *            logs etc.
	 * @param alpha
	 *            How to weigh the configuration quality against the average
	 *            overall fitness. Fitness will be alpha*configQ + (1-alpha)
	 *            avg(Of)
	 * @param absolute
	 *            If TRUE then an absolute configuration fitness is provided, if
	 *            FALSE a normalized value between 0 and 1 is produced
	 * @param logFitness
	 *            List of overall fitness evaluators, one for each log in the
	 *            registry
	 */
	public ConfigurationFitness(CentralRegistryConfigurable registry, double alpha, boolean absolute,
			TreeFitnessAbstract... logFitness) {
		if (logFitness.length != registry.getNrLogs()) {
			throw new IllegalArgumentException(
					"The number of provided fitness evaluators is not the same as the number of event logs");
		}

		this.registry = registry;

		this.alpha = alpha;
		this.absoluteCount = absolute;

		this.logFitnessList = logFitness;

		//Get the log sizes once for each log for performance reasons
		this.logSizes = new int[registry.getNrLogs()];
		this.totalTraces = 0;
		for (int i = 0; i < registry.getNrLogs(); i++) {
			int nrTraces = registry.getRegistry(i).getLogInfo().getNumberOfTraces();
			logSizes[i] = nrTraces;
			totalTraces += nrTraces;
		}
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		double overallFitness = 0;
		ProbProcessArrayTree[] configuredTrees = new ProbProcessArrayTree[logSizes.length];
		for (int i = 0; i < logSizes.length; i++) {
			configuredTrees[i] = candidate.applyConfiguration(i);
			overallFitness += (logFitnessList[i].getFitness(configuredTrees[i], population) * logSizes[i]);
		}
		overallFitness /= totalTraces;

		double configFitness = getConfigFitness(candidate);

		double finalFitness = (alpha * configFitness) + ((1 - alpha) * overallFitness);

		//This is a special case where the 'individual' value is different from the overall one :)
		registry.getFitness(candidate).setOverallFitness(info, finalFitness);
		registry.getFitness(candidate).fitnessValues.put(info, configFitness);

		//Now also put the more detailed fitnesses in the configurable registry
		Set<TreeFitnessInfo> keys = null;
		for (int i = 0; i < registry.getNrLogs(); i++) {
			if (registry.getRegistry(i).isFitnessKnown(configuredTrees[i])) {
				keys = registry.getRegistry(i).getFitness(configuredTrees[i]).fitnessValues.keySet();
				break;
			}
		}

		if (keys != null) {
			for (TreeFitnessInfo key : keys) {
				//Put the weighted average into the fitness cache
				double thisFitness = 0;
				for (int i = 0; i < logSizes.length; i++) {
					thisFitness += registry.getRegistry(i).getFitness(configuredTrees[i]).fitnessValues.get(key)
							* logSizes[i];
				}
				thisFitness /= totalTraces;
				registry.getFitness(candidate).fitnessValues.put(key, thisFitness);
			}
		}

		//FIXME also add the behavior counter information to the configurable registry cache

		return finalFitness;
	}

	public double getConfigFitness(ProbProcessArrayTree tree) {
		return countConfiguredColums(tree, absoluteCount);
		//return countAllConfigSettings(tree);
		//return countUselessConfigSettings(tree);
	}

	public static double countConfiguredColums(ProbProcessArrayTree tree, boolean absoluteCount) {
		/*
		 * We count the number of nodes that have a configuration. That way we
		 * don't punish for the second log configuring the same node
		 */

		double nrConfigSettings = 0;
		for (int i = 0; i < tree.size(); i++) {
			for (int c = 0; c < tree.getNumberOfConfigurations(); c++) {
				byte config = tree.getNodeConfiguration(c, i);
				if (config != Configuration.NOTCONFIGURED) {
					nrConfigSettings++;
					break; //We break this node in the tree and counted it as one
				}
			}
		}

		//less options is better
		double fitness;
		if (absoluteCount) {
			return nrConfigSettings;
		} else {
			if (nrConfigSettings == 0) {
				fitness = 1;
			} else {
				fitness = 1 - (nrConfigSettings / tree.size());
			}
		}

		return fitness;
	}

	public double countAllConfigSettings(ProbProcessArrayTree tree) {
		/*
		 * This is because precision should punish for too much behavior, which
		 * is permanently disabled when it is blocked/hidden. Then again,
		 * fitness prevents too much from beeing blocked/hidden. This should
		 * work, right? :D
		 */

		double nrConfigSettings = 0;
		for (int c = 0; c < tree.getNumberOfConfigurations(); c++) {
			for (int i = 0; i < tree.size(); i++) {
				byte config = tree.getNodeConfiguration(c, i);
				if (config != Configuration.NOTCONFIGURED) {
					nrConfigSettings++;
				}
			}
		}

		//less options is better
		double fitness;
		if (nrConfigSettings == 0) {
			fitness = 1;
		} else {
			fitness = 1 - (nrConfigSettings / (tree.size() * Math.max(tree.getNumberOfConfigurations(),
					registry.getNrLogs())));
		}

		return fitness;
	}

	public static double countUselessConfigSettings(ProbProcessArrayTree tree) {
		int nrConfigSettings = 0;
		//COUNT ONLY USELESS CONFIG SETTINGS:
		for (int i = 0; i < tree.size(); i++) {
			byte lastOne = tree.getNodeConfiguration(0, i);

			for (int c = 1; c < tree.getNumberOfConfigurations(); c++) {
				if (tree.getNodeConfiguration(c, i) != Configuration.NOTCONFIGURED
						&& tree.getNodeConfiguration(c, i) != lastOne) {
					nrConfigSettings++;
					//We only punish once per configuration
					break;
				}
			}
		}

		//less options is better
		double fitness;
		if (nrConfigSettings == 0) {
			fitness = 1;
		} else {
			fitness = 1 - (nrConfigSettings / tree.size());
		}

		return fitness;
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}

	public TreeFitnessAbstract[] getFitnessList() {
		return logFitnessList;
	}

	/**
	 * @return the alpha
	 */
	public double getAlpha() {
		return alpha;
	}

	/**
	 * @param alpha the alpha to set
	 */
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	/**
	 * GUI class that provides a GUI interface (for the ProM Wizard) to this
	 * fitness evaluator.
	 * 
	 * @author jbuijs
	 * 
	 */
	public static class ConfigurationFitnessGUI extends TreeFitnessGUISettingsAbstract<ConfigurationFitness> {

		private static final long serialVersionUID = 1L;

		FitnessRegistry fitnessRegistry;

		private ProMTextField alphaTxtfield;

		private ProMScrollContainer evaluators;

		public ConfigurationFitnessGUI(final ETMParamAbstract param, PluginContext context) {
			super(param);

			ConfigurationFitness configFitness = (ConfigurationFitness) param.getFitnessEvaluator();
			
			fitnessRegistry = new FitnessRegistry(context);

			//first the header panel with the dropdown box and the 'add' button
			this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			RoundedPanel configAlpha = new RoundedPanel();
			configAlpha.setLayout(new BoxLayout(configAlpha, BoxLayout.LINE_AXIS));

			JLabel alphaLabel = SlickerFactory.instance().createLabel("Configuration Alpha: ");
			String alphaTooltipText = "";
			alphaLabel.setToolTipText(alphaTooltipText);
			configAlpha.add(alphaLabel);
			alphaTxtfield = new ProMTextField("" + configFitness.getAlpha());
			alphaTxtfield.setToolTipText(alphaTooltipText);

			this.add(alphaTxtfield);
			
			//FIXME add overallFitnessGUI once, but update all overall fitnesses for all logs... grmbl... 
		}

		private TreeFitnessInfo[] getEvaluators(ETMParamAbstract param) {
			List<TreeFitnessInfo> treeFitnessInfo = fitnessRegistry.getAllMetricInfos();
			//Now remove the META ones...
			List<TreeFitnessInfo> treeFitnessInfoNonMeta = new ArrayList<TreeFitnessInfo>();
			for (TreeFitnessInfo info : treeFitnessInfo) {
				if (info.getDimension() != org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension.META) {
					treeFitnessInfoNonMeta.add(info);
				}
			}

			TreeFitnessInfo[] array = treeFitnessInfoNonMeta
					.toArray(new TreeFitnessInfo[treeFitnessInfoNonMeta.size()]);
			Arrays.sort(array);
			return array;
		}

		public void init(OverallFitness instance) {
			if (instance == null)
				return;

			//We start afresh
			evaluators.clearChildren();

			for (Entry<TreeFitnessAbstract, Double> entry : instance.getEvaluators().entrySet()) {
				EvaluatorProperty evalProperty = new EvaluatorProperty(evaluators, param, entry.getKey().getInfo(),
						entry.getValue());
				evalProperty.init(entry.getKey());
				evaluators.addChild(evalProperty);
			}
		}

		public OverallFitness getTreeFitnessInstance(ETMParamAbstract param) {
			LinkedHashMap<TreeFitnessAbstract, Double> alg = new LinkedHashMap<TreeFitnessAbstract, Double>();

			for (ProMScrollContainerChild child : evaluators.getChildren()) {
				EvaluatorProperty evaluatorProperty = (EvaluatorProperty) child;

				alg.put(evaluatorProperty.instantiateEvaluator(param.getCentralRegistry()),
						evaluatorProperty.getWeight());
			}

			return new OverallFitness(param.getCentralRegistry(), alg);
		}

		/**
		 * Visualizes a TreeFitnessInfo object and a weight textfield
		 * 
		 * @author jbuijs
		 * 
		 */
		private class EvaluatorProperty extends ProMTitledScrollContainerChild {
			private static final long serialVersionUID = 1L;

			private TreeFitnessInfo evaluatorInfo;

			private TreeFitnessGUISettingsAbstract<TreeFitnessAbstract> panel;
			private ProMTextField weightTxtfield;

			@SuppressWarnings("unchecked")
			public EvaluatorProperty(ProMScrollContainer parent, ETMParamAbstract param, TreeFitnessInfo evaluator) {
				this(parent, param, evaluator, 1.0);
			}

			public EvaluatorProperty(ProMScrollContainer parent, ETMParamAbstract param, TreeFitnessInfo evaluator,
					double weight) {
				super(evaluator.getName(), parent);
				this.evaluatorInfo = evaluator;

				try {
					//FIXME fail for ETMPareto, and does not seem to work for ETMParam
					Method m = evaluator.getClazz().getMethod("getGUISettingsPanel", ETMParamAbstract.class);
					panel = (TreeFitnessGUISettingsAbstract<TreeFitnessAbstract>) m.invoke(null, param);

					getContentPanel().setLayout(new BoxLayout(getContentPanel(), BoxLayout.Y_AXIS));

					if (panel.providesGUI()) {
						getContentPanel().add(panel);
					} else {
						//Reduce the size of ourselves to neatly fit 1 row of textfield (the 90 is important)
						this.setPreferredSize(new java.awt.Dimension(300, 90));
					}

					String weightTooltipText = "<html>Provide the weight to calculate the weighted average overall fitness. <br />"
							+ "If set to 0 then the value is calculated for each candidate but not included in the overall fitness value.</html>";

					RoundedPanel propPanel = new RoundedPanel();
					propPanel.setLayout(new GridLayout(1, 2));
					JLabel weightLabel = SlickerFactory.instance().createLabel("Weight: ");
					weightLabel.setToolTipText(weightTooltipText);
					propPanel.add(weightLabel);
					weightTxtfield = new ProMTextField("" + weight);
					weightTxtfield.setToolTipText(weightTooltipText);
					propPanel.add(weightTxtfield);
					getContentPanel().add(propPanel);
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}

			}

			/**
			 * Initialize the TreeFitness GUI specific panel with the current
			 * instance of the TreeFitnessAbstract
			 * 
			 * @param key
			 */
			public void init(TreeFitnessAbstract key) {
				if (panel == null) {
					panel = new TreeFitnessGUISettingsEmpty(param);
				}
				panel.init(key);
			}

			public TreeFitnessAbstract instantiateEvaluator(CentralRegistry registry) {
				return panel.getTreeFitnessInstance(registry, (Class<TreeFitnessAbstract>) evaluatorInfo.getClazz());
			}

			public double getWeight() {
				return Double.parseDouble(weightTxtfield.getText());
			}
		}

		public void init(ConfigurationFitness instance) {
			// TODO Auto-generated method stub
			
		}
	}/**/
}
