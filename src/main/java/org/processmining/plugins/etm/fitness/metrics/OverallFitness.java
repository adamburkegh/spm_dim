package org.processmining.plugins.etm.fitness.metrics;

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMScrollContainer;
import org.processmining.framework.util.ui.widgets.ProMScrollContainerChild;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.framework.util.ui.widgets.ProMTitledScrollContainerChild;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.FitnessRegistry;
import org.processmining.plugins.etm.fitness.TreeFitness;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * Meta fitness calculator. Calculates multiple fitnesses and then calculates
 * the weighted average.
 */
public class OverallFitness extends TreeFitnessAbstract {

	public static final TreeFitnessInfo info = new TreeFitnessInfo(OverallFitness.class, "Of", "Overall Fitness",
			"This fitness metric can call a list of other metrics and calculate a weighted average.", Dimension.META,
			true);

	/**
	 * We require the fitness alg. in a particular order (e.g. replay fitness
	 * before precision!). Hence we require a linkedhashmap
	 */
	private LinkedHashMap<TreeFitnessAbstract, Double> evaluators;

	private final CentralRegistry registry;

	/**
	 * If set to true the fitness values of the other dimension are multiplied
	 * by the Fr fitness value. E.g. with an Fr fitness of 0.5, precision can be
	 * 0.5 max too. This is to emphasize the importance of Fr over the other
	 * dimensions. Weights are still applied.
	 */
	public boolean fitnessMultiplication = false;

	protected OverallFitness() {
		this.registry = null;
		evaluators = new LinkedHashMap<TreeFitnessAbstract, Double>();
	}

	/**
	 * An empty constructor that creates and empty evaluators list
	 */
	public OverallFitness(CentralRegistry registry) {
		this(registry, new LinkedHashMap<TreeFitnessAbstract, Double>());
	}

	/**
	 * Constructor which is provided a list of instantiated fitness algorithms
	 * and their weights
	 * 
	 * @param seed
	 * 
	 * @param alg
	 */
	public OverallFitness(CentralRegistry registry, LinkedHashMap<TreeFitnessAbstract, Double> alg) {
		evaluators = alg;
		this.registry = registry;
	}

	@SuppressWarnings("unchecked")
	public OverallFitness(OverallFitness original) {
		this(original.registry);

		this.fitnessMultiplication = original.fitnessMultiplication;

		//Map from class to the cloned instance
		HashMap<Class<TreeFitnessAbstract>, TreeFitnessAbstract> clonedMap = new HashMap<Class<TreeFitnessAbstract>, TreeFitnessAbstract>();
		//Now process the evaluators, deep clone them too!
		for (Map.Entry<TreeFitnessAbstract, Double> evaluator : original.evaluators.entrySet()) {
			TreeFitnessAbstract eval = evaluator.getKey();
			TreeFitnessAbstract clonedEval = TreeFitnessAbstract.deepClone(eval, clonedMap);

			assert clonedEval != null;

			clonedMap.put((Class<TreeFitnessAbstract>) eval.getClass(), clonedEval);

			evaluators.put(clonedEval, evaluator.getValue());
		}
	}

	public void addEvaluator(TreeFitnessAbstract evaluator, double weight) {
		evaluators.put(evaluator, new Double(weight));
	}

	/**
	 * Returns the currently set fitness evaluators and their weights. This
	 * linkedHashMap can be updated if desired.
	 * 
	 * @return
	 */
	public LinkedHashMap<TreeFitnessAbstract, Double> getEvaluators() {
		return evaluators;
	}

	public synchronized double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		double overallFitness = 0;
		double normalization = 0;

		if (registry.isFitnessKnown(candidate)) {
			double oF = info.getWorstFitnessValue();
			try {
				oF = registry.getFitness(candidate).getOverallFitnessValue();
			} catch (Exception e) {
				System.out.println("ERROR");
			}

			//TODO remove debug code
			if (oF < 0) {
				System.out.println("ERROR");
			}

			//TODO remove debug code Fr ~~ 0
			if (oF != 0 && registry.getFitness(candidate).fitnessValues.get(FitnessReplay.info) < 0.01) {
				TObjectDoubleHashMap<TreeFitnessInfo> fitValues = registry.getFitness(candidate).fitnessValues;
				//System.out.println("Fr of ~0 detected!");
				//Recalculate the values
				double total = 0;
				double weight = 0;
				for (Entry<TreeFitnessAbstract, Double> dimension : evaluators.entrySet()) {
					total += fitValues.get(dimension.getKey().getInfo()) * dimension.getValue();
					weight += dimension.getValue();
				}
				double recalcOf = total / weight;
				if (oF != recalcOf) {
					System.out.println(String.format("Of: %1.3f newOf: %1.3f", oF, recalcOf));
				}
			}

			return oF;
		}

		//Call each evaluator since fitness is not (yet) known
		for (Map.Entry<TreeFitnessAbstract, Double> evaluator : evaluators.entrySet()) {
			double weight = evaluator.getValue();
			TreeFitnessAbstract fitnessAlg = evaluator.getKey();
			double fitness = fitnessAlg.getFitness(candidate, population);

			//TODO remove debug code
			if (fitness < 0) {
				System.out.println("Negative fitness value encountered");
				fitness = 0;
			}

			//TODO remove debug code
			/*-
			long dur = new Date().getTime() - start;
			if (dur > 1000) {
				System.out.println("Fitness evaluation of " + dur + " for " + fitnessAlg.toString());
				System.out.println("Tree: " + TreeUtils.toString(candidate, registry.getEventClasses()));
			}/**/

			if (fitnessAlg.getInfo().isNatural()) {
				overallFitness += fitness * weight;
			} else {
				//FIXME check if this works always, f.i. for absolute #uselessNodes etc.
				//In the case that less is better we assume that the value is also normalized... hence invert fitness

				overallFitness += fitness * weight;
				//TODO FIX: fails indeed for non-normalized values where smaller is better
				//overallFitness += (1 - fitness) * weight;
			}
			normalization += weight;

			registry.getFitness(candidate).fitnessValues.put(fitnessAlg.getInfo(), fitness);

			//If the evaluator is the FitnessReplay evaluator
			if (fitnessAlg.getInfo().equals(FitnessReplay.info)) {
				FitnessReplay fr = (FitnessReplay) fitnessAlg;
				//Check if the last result is reliable, if not
				if (!fr.isLastResultReliable()) {
					//Return a bad bad value
					overallFitness = info.getWorstFitnessValue();
					//And prevent other evaluators to be executed
					break;
				}
			}//Reliable result so continue with next evaluator
		}

		if (normalization == 0) {
			overallFitness = 0;
		} else if (fitnessMultiplication) {
			//We want to do Fr * (SUM(weight*value) / SUM(weight)) of all other dims, e.g. Fr is really most important
			double frFitness = -1;
			double frWeight = -1;
			//First remove the Fr fitness from the fitness and weights
			for (Map.Entry<TreeFitnessAbstract, Double> evaluator : evaluators.entrySet()) {
				if (evaluator.getKey() instanceof FitnessReplay) {
					frFitness = registry.getFitness(candidate).fitnessValues.get(FitnessReplay.info);
					frWeight = evaluator.getValue();
					break;
				}
			}
			overallFitness -= frFitness * frWeight;
			normalization -= frWeight;

			//Overall fitness now is:
			overallFitness = normalization == 0 ? overallFitness : frFitness * (overallFitness / normalization);
		} else {
			overallFitness /= normalization;
		}

		/*
		 * FIXME in case of configuration calculations, another thread may be
		 * busy with the same resulting tree, therefore the following assertion
		 * had to be disabled.
		 */
		/*
		 * This can be fixed by keeping a blocked tree list/set in the
		 * centralRegistry since waiting for a running evaluation is ~always
		 * better than starting it again.
		 */
		//assert !registry.isFitnessKnown(candidate);

		if (registry.getFitness(candidate).getOverallFitnessValue() != TreeFitness.NOVALUE
				&& registry.getFitness(candidate).getOverallFitnessValue() != overallFitness) {
			boolean isknown = registry.isFitnessKnown(candidate);
			System.err.println("Oops, overall fitness already know for this tree... :S");
		}

		registry.getFitness(candidate).setOverallFitness(info, overallFitness);
		registry.getFitness(candidate).setInGeneration(registry.getCurrentGeneration());

		//TODO remove debug code
		if (overallFitness < 0 || Double.isNaN(overallFitness)) {
			System.out.println("Breaky breaky");
		}

		return overallFitness;
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}

	/**
	 * GUI class that provides a GUI interface (for the ProM Wizard) to this
	 * fitness evaluator.
	 * 
	 * @author jbuijs
	 * 
	 */
	public static class OverallFitnessGUI extends TreeFitnessGUISettingsAbstract<OverallFitness> {

		private static final long serialVersionUID = 1L;

		FitnessRegistry fitnessRegistry;

		public JPanel addEvaluatorsPanel;

		private ProMScrollContainer evaluators;

		public OverallFitnessGUI(final ETMParamAbstract<ProbProcessArrayTree,ProbProcessArrayTree> param, PluginContext context) {
			super(param);

			fitnessRegistry = new FitnessRegistry(context);

			evaluators = new ProMScrollContainer();

			//first the header panel with the dropdown box and the 'add' button
			this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			addEvaluatorsPanel = new JPanel();
			//addEvaluatorsPanel.setLayout(new GridBagLayout());
			//addEvaluatorsPanel.setLayout(new FlowLayout());
			addEvaluatorsPanel.setLayout(new BoxLayout(addEvaluatorsPanel, BoxLayout.X_AXIS));
			TreeFitnessInfo[] evaluatorsForComboBox = getEvaluators(param);
			final ProMComboBox evaluatorCombobox = new ProMComboBox(evaluatorsForComboBox);
			evaluatorCombobox.setToolTipText("Select an evaluator to include in the algorithm.");
			evaluatorCombobox.setRenderer(new TreeFitnessInfo.TreeFitnessInfoComboboxRenderer(evaluatorCombobox
					.getRenderer()));
			//evaluatorCombobox.setMaximumSize(new java.awt.Dimension(100,25));
			addEvaluatorsPanel.add(evaluatorCombobox);
			JButton addButton = SlickerFactory.instance().createButton("+");
			addButton.setToolTipText("Add this evaluator");
			addEvaluatorsPanel.add(addButton);
			//addEvaluatorsPanel.setMaximumSize(new java.awt.Dimension(200, 25));

			addButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					//We add the currently selected evaluator to the list
					TreeFitnessInfo info = (TreeFitnessInfo) evaluatorCombobox.getSelectedItem();

					EvaluatorProperty property = new EvaluatorProperty(evaluators, param, fitnessRegistry
							.getTreeFitnessInfoByCode(info.getCode()));

					evaluators.addChild(property);
				}
			});

			//Then the scroll container showing all evaluators
			//evaluators.addChild(new EvaluatorProperty(evaluators, param, FitnessReplay.info, 1d));

			this.add(addEvaluatorsPanel);
			this.add(evaluators);

			FitnessEvaluator<ProbProcessArrayTree> eval = param.getFitnessEvaluator();
			if (eval != null) {
				//If it is a multie threading thing, extract one of the underlying ones
				if (eval instanceof MultiThreadedFitnessEvaluator) {
					MultiThreadedFitnessEvaluator mtfe = (MultiThreadedFitnessEvaluator) eval;
					if (mtfe.getEvaluators().length > 0) {
						eval = mtfe.getEvaluators()[0];
					}
				}
				//Now, if it is an Overallfitness then...
				if (eval instanceof OverallFitness) {
					OverallFitness oF = (OverallFitness) eval;
					init(oF);
				}
			}
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
	}/**/
}