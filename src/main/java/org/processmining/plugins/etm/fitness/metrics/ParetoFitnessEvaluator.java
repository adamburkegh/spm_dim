package org.processmining.plugins.etm.fitness.metrics;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.processmining.plugins.etm.parameters.ETMParamPareto;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.factory.SlickerFactory;

// FIXME check all class contents
// FIXME Test Class thoroughly
/**
 * This is a special evaluator type that should be called AFTER another META
 * evaluator (f.i. the {@link OverallFitness} that calculates the values for the
 * different dimensions. It is best to only calculate the Pareto fitness when
 * the individual fitness values are know for the whole population.
 * 
 * @author jbuijs
 * 
 */
public class ParetoFitnessEvaluator extends TreeFitnessAbstract {

	public static final TreeFitnessInfo info = new TreeFitnessInfo(
			ParetoFitnessEvaluator.class,
			"PFf",
			"Pareto Fitness",
			"Sets the fitness of a tree as the number of trees that dominate the candidate (integer value) "
					+ "and the relative distance the candidate has to it's nearest neighbour (distance as used in NSGAII) (0 <= value < 1). "
					+ "Hence a value of 0 is best, indicating it has an extreme value for at least one dimension.",
			Dimension.META, false);

	//paretoFront is not final since it could be that we would want to change the reference
	protected ParetoFront paretoFront;

	private final CentralRegistry registry;

	/**
	 * Constructs a new {@link ParetoFitnessEvaluator} instance. NOTE that you
	 * need to call setParetoFront() before you can call getFitness()! Use this
	 * constructor if you need to instantiate the evaluator without having a
	 * {@link ParetoFront} instance yet.
	 * 
	 * @param registry
	 */
	public ParetoFitnessEvaluator(CentralRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Constructs a new {@link ParetoFitnessEvaluator} instance that can be used
	 * directly to evaluate the Pareto fitness.
	 * 
	 * @param registry
	 * @param paretoFront
	 */
	public ParetoFitnessEvaluator(CentralRegistry registry, ParetoFront paretoFront) {
		this.registry = registry;
		this.paretoFront = paretoFront;
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		if (paretoFront == null) {
			throw new IllegalStateException(
					"Before calling the first getFitness() you must set a reference to the Pareto front.");
		}

		/*
		 * The Pareto fitness is the number of trees dominating the candidate +
		 * 1 - the relative distance (e.g. very 'distant' candidates that are
		 * not dominated get the perfect score of 0)
		 */
		double fitness = paretoFront.countDominatorsOf(candidate) + (1 - getRelativeDistance(candidate));

		//We consider dominators in the population too (otherwise order of consideration of trees influences their fitness)
		if (population != null) {
			for (ProbProcessArrayTree populationTree : population) {
				//But only if the tree from the population is not in the PF already
				if (candidate != populationTree && !paretoFront.getFront().contains(populationTree)) {
					//If the candidate is dominated
					if (paretoFront.dominates(populationTree, candidate)) {
						//Add a point to the fitness
						fitness++;
					}
				}
			}
		}

		registry.getFitness(candidate).setOverallFitness(info, fitness);

		//TODO remove debug code
		if (fitness < 0) {
			System.out.println("negative fitness!");
		}

		//TODO remove debug code
		if(fitness == Double.NaN){
			//System.out.println("PFf NaN!");
		}
		
		return fitness;
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}

	public void setParetoFront(ParetoFront paretoFront) {
		this.paretoFront = paretoFront;
	}
	
	public ParetoFront getParetoFront(){
		return this.paretoFront;
	}

	/**
	 * Returns the relative distance of the given tree to that of its neighbours
	 * in the Pareto front. Taken from the NSGAII algorithm. High distance is
	 * considered good since its goal is to improve diversity. Candidates that
	 * are the extremely good or bad candidate for a certain dimension will
	 * always receive the perfect (relative) distance of 1. For the others the
	 * distance is calculated as the average of the distances of all dimensions,
	 * where the distance of one dimension is calculated by dividing the
	 * distance between the values of one candidate better and worse by the
	 * total distance between the best and worst candidate in that dimension.
	 * 
	 * @param tree
	 * @return double value of the relative distance. Bigger is better, 1 is
	 *         perfect, 0 is worst.
	 */
	public double getRelativeDistance(ProbProcessArrayTree tree) {
		return getRelativeDistance(paretoFront, tree);
	}

	/**
	 * Returns the relative distance of the given tree to that of its neighbours
	 * in the Pareto front. Taken from the NSGAII algorithm. High distance is
	 * considered good since its goal is to improve diversity. Candidates that
	 * are the extremely good or bad candidate for a certain dimension will
	 * always receive the perfect (relative) distance of 1. For the others the
	 * distance is calculated as the average of the distances of all dimensions,
	 * where the distance of one dimension is calculated by dividing the
	 * distance between the values of one candidate better and worse by the
	 * total distance between the best and worst candidate in that dimension.
	 * 
	 * @param paretoFront
	 * @param tree
	 * @return double value of the relative distance. Bigger is better, 1 is
	 *         perfect, 0 is worst.
	 */
	public static double getRelativeDistance(ParetoFront paretoFront, ProbProcessArrayTree tree) {
		//If the tree is not in the front its bad for sure
		if (!paretoFront.getFront().contains(tree))
			return 0;

		CentralRegistry registry = paretoFront.getRegistry();

		double distanceSum = 0;

		//For each dimension
		for (TreeFitnessInfo dimension : paretoFront.getDimensions()) {
			//If the current tree is the best or worst we return the best value: 1
			ProbProcessArrayTree bestTree = paretoFront.getBest(dimension);
			ProbProcessArrayTree worstTree = paretoFront.getWorst(dimension);

			if (worstTree.equals(tree) || bestTree.equals(tree))
				return 1;

			//Now get the values of one better and one worse tree
			double better = registry.getFitness(paretoFront.getBetter(dimension, tree)).fitnessValues.get(dimension);
			double worse = registry.getFitness(paretoFront.getWorse(dimension, tree)).fitnessValues.get(dimension);

			//Get the best and worst values
			double best = registry.getFitness(bestTree).fitnessValues.get(dimension);
			double worst = registry.getFitness(worstTree).fitnessValues.get(dimension);

			//Normalize for this dimension
			double distanceForThisDim = Math.min(Math.abs(better - worse) / Math.abs(best - worst), 1);

			//Increase distance sum with relative difference
			distanceSum += distanceForThisDim;
		}

		//Normalize by averaging over all dimensions
		double distanceNormalized = distanceSum / paretoFront.getDimensions().length;

		return distanceNormalized;
	}

	/**
	 * GUI interface for the {@link ParetoFitnessEvaluator} class. NOTE that
	 * this requires at least a {@link ETMParamPareto} instance. Also note that
	 * this GUI will instantiate an {@link OverallFitness} evaluator instance
	 * (within a multithreading environment) with equal weights to populate the
	 * different quality values in the Pareto front. It will also instantiate a
	 * {@link ParetoFitnessEvaluator} instance and add it to the
	 * {@link ETMParamPareto} object.
	 * 
	 * @author jbuijs
	 * 
	 */
	public static class ParetoFitnessEvaluatorGUI extends TreeFitnessGUISettingsAbstract<ParetoFitnessEvaluator> {

		private static final long serialVersionUID = 1L;

		FitnessRegistry fitnessRegistry;

		private JPanel addEvaluatorsPanel;

		//Make our parameter object more specific
		protected ETMParamPareto param;

		private JPanel applyLimitsAtGenerationPanel;
		private ProMTextField applyLimitsAtGenerationTxtField;

		private ProMScrollContainer evaluators;

		public ParetoFitnessEvaluatorGUI(final ETMParamPareto param, PluginContext context) {
			super(param);
			this.param = param;

			fitnessRegistry = new FitnessRegistry(context);

			evaluators = new ProMScrollContainer();

			this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			
			//FIXME add property to set maximum Pareto Front size

			//Add the textfield with the 'apply limits at generation:' setting
			String applyLimitsAtGenerationTooltip = "<html>Applying the (strict) limits set for quality dimension from the beginning is not always a good idea <BR />"
					+ "since a population needs to be created and improved. <BR />"
					+ "Therefore you can specify at which generation the upper and lower limits <BR />"
					+ "should be applied to filter the Pareto front.<br />"
					+ "Note that limits are only enforced when at least one candidate will survive.</html>";
			applyLimitsAtGenerationPanel = new JPanel();
			applyLimitsAtGenerationPanel.setLayout(new BoxLayout(applyLimitsAtGenerationPanel, BoxLayout.X_AXIS));
			applyLimitsAtGenerationPanel.setToolTipText(applyLimitsAtGenerationTooltip);
			JLabel applyLimitsAtGenerationLabel = SlickerFactory.instance().createLabel(
					"Apply the lower and upper limits only after this generation:");
			applyLimitsAtGenerationLabel.setToolTipText(applyLimitsAtGenerationTooltip);
			applyLimitsAtGenerationPanel.add(applyLimitsAtGenerationLabel);
			applyLimitsAtGenerationTxtField = new ProMTextField("0");
			applyLimitsAtGenerationTxtField.setToolTipText(applyLimitsAtGenerationTooltip);
			applyLimitsAtGenerationPanel.add(applyLimitsAtGenerationTxtField);
			this.add(applyLimitsAtGenerationPanel);

			//Then the header panel with the dropdown box and the 'add' button
			addEvaluatorsPanel = new JPanel();
			addEvaluatorsPanel.setLayout(new BoxLayout(addEvaluatorsPanel, BoxLayout.X_AXIS));
			TreeFitnessInfo[] evaluatorsForComboBox = getEvaluators(param);
			final ProMComboBox evaluatorCombobox = new ProMComboBox(evaluatorsForComboBox);
			evaluatorCombobox.setToolTipText("Select an evaluator to include in the algorithm.");
			evaluatorCombobox.setRenderer(new TreeFitnessInfo.TreeFitnessInfoComboboxRenderer(evaluatorCombobox
					.getRenderer()));
			addEvaluatorsPanel.add(evaluatorCombobox);
			JButton addButton = SlickerFactory.instance().createButton("+");
			addButton.setToolTipText("Add this evaluator");
			addEvaluatorsPanel.add(addButton);

			addButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					//We add the currently selected evaluator to the list
					TreeFitnessInfo info = (TreeFitnessInfo) evaluatorCombobox.getSelectedItem();

					EvaluatorProperty property = new EvaluatorProperty(evaluators, param, fitnessRegistry
							.getTreeFitnessInfoByCode(info.getCode()));

					evaluators.addChild(property);
				}
			});

			this.add(addEvaluatorsPanel);
			this.add(evaluators);

			init(param.getParetoFitnessEvaluator());
		}

		private TreeFitnessInfo[] getEvaluators(ETMParamPareto param) {
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

		public void init(ParetoFitnessEvaluator instance) {
			if (instance == null)
				return;

			//We start afresh
			evaluators.clearChildren();

			/*
			 * Extract the OverallFitness evaluator instance, which we assume is
			 * present :)
			 */
			FitnessEvaluator<ProbProcessArrayTree> eval = param.getFitnessEvaluator();
			OverallFitness oF = null;
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
					oF = (OverallFitness) eval;
				}
			}

			//If we actually found an oF instance we can populate our little list
			if (oF != null) {
				for (Entry<TreeFitnessAbstract, Double> entry : oF.getEvaluators().entrySet()) {
					TreeFitnessInfo key = entry.getKey().getInfo();
					double lowerLimit = param.getLowerlimits().containsKey(key) ? param.getLowerlimits().get(key) : -1;
					double upperLimit = param.getUpperlimits().containsKey(key) ? param.getUpperlimits().get(key) : -1;

					EvaluatorProperty evalProperty = new EvaluatorProperty(evaluators, param, entry.getKey().getInfo(),
							lowerLimit, upperLimit);
					evalProperty.init(entry.getKey());
					evaluators.addChild(evalProperty);
				}
			}

			//Set the 'apply at generation' field
			applyLimitsAtGenerationTxtField.setText("" + param.getGenerationWhenLimitsAreApplied());

		}

		/**
		 * Note that this function returns only the {@link OverallFitness}
		 * instance but also sets the specific Pareto fitness evaluator in the
		 * param object!
		 * 
		 * @param param
		 * @return
		 */
		public OverallFitness getTreeFitnessInstance(ETMParamPareto param) {
			LinkedHashMap<TreeFitnessAbstract, Double> alg = new LinkedHashMap<TreeFitnessAbstract, Double>();

			for (ProMScrollContainerChild child : evaluators.getChildren()) {
				EvaluatorProperty evaluatorProperty = (EvaluatorProperty) child;

				TreeFitnessAbstract evaluator = evaluatorProperty.instantiateEvaluator(param.getCentralRegistry());
				alg.put(evaluator, 1.);

				//Copy all the limits
				double lowerlimit = evaluatorProperty.getLowerLimit();
				if (lowerlimit >= 0)
					param.getLowerlimits().put(evaluator.getInfo(), lowerlimit);
				double upperlimit = evaluatorProperty.getUpperLimit();
				if (upperlimit >= 0)
					param.getUpperlimits().put(evaluator.getInfo(), upperlimit);
			}

			//And set the generation when to apply the limits (if any)
			try {
				int applyAtG = Integer.parseInt(applyLimitsAtGenerationTxtField.getText());
				if (applyAtG >= 0) {
					param.setGenerationWhenLimitsAreApplied(applyAtG);
				}
			} catch (Exception e) {
				//Well, don't bother then
			}

			param.setParetoFitnessEvaluator(new ParetoFitnessEvaluator(param.getCentralRegistry()));
			param.addIgnoredDimension(OverallFitness.info);

			return new OverallFitness(param.getCentralRegistry(), alg);
		}

		/**
		 * Visualizes a TreeFitnessInfo object and the corresponding Pareto
		 * fields (upper and lower limit for this dimension)
		 * 
		 * @author jbuijs
		 * 
		 */
		private class EvaluatorProperty extends ProMTitledScrollContainerChild {
			//FIXME implement
			private static final long serialVersionUID = 1L;

			private TreeFitnessInfo evaluatorInfo;

			private TreeFitnessGUISettingsAbstract<TreeFitnessAbstract> panel;
			private ProMTextField upperLimitTxtfield;
			private ProMTextField lowerLimitTxtfield;

			@SuppressWarnings("unchecked")
			public EvaluatorProperty(ProMScrollContainer parent, ETMParamPareto param, TreeFitnessInfo evaluator) {
				this(parent, param, evaluator, -1, -1);
			}

			public EvaluatorProperty(ProMScrollContainer parent, ETMParamPareto param, TreeFitnessInfo evaluator,
					double upperLimit, double lowerLimit) {
				super(evaluator.getName(), parent);
				this.evaluatorInfo = evaluator;

				try {
					Method m = evaluator.getClazz().getMethod("getGUISettingsPanel", ETMParamAbstract.class);
					panel = (TreeFitnessGUISettingsAbstract<TreeFitnessAbstract>) m.invoke(null, param);

					getContentPanel().setLayout(new BoxLayout(getContentPanel(), BoxLayout.Y_AXIS));

					if (panel.providesGUI()) {
						getContentPanel().add(panel);
						getContentPanel().invalidate();
						this.setPreferredSize(new java.awt.Dimension(panel.getPreferredSize().width, panel
								.getPreferredSize().width + 60));
					} else {
						//Reduce the size of ourselves to neatly fit 2 rows of textfields (the 120 is important)
						this.setPreferredSize(new java.awt.Dimension(300, 120));
					}

					String lowerLimitTooltipText = "<html>Setting this to a (positive) value applies filtering to the Pareto Front, <BR />"
							+ "not adding candidates with a value for this quality dimension <BR />"
							+ "which is lower than the provided value (if any). </html>";
					String upperLimitTooltipText = "<html>Setting this to a (positive) value applies filtering to the Pareto Front, <BR />"
							+ "not adding candidates with a value for this quality dimension <BR />"
							+ "which is higher than the provided value (if any). </html>";

					RoundedPanel propPanel = new RoundedPanel();
					propPanel.setLayout(new GridLayout(2, 2));

					JLabel lowerLimitLabel = SlickerFactory.instance().createLabel("Ignore models with values below: ");
					lowerLimitLabel.setToolTipText(lowerLimitTooltipText);
					propPanel.add(lowerLimitLabel);
					lowerLimitTxtfield = new ProMTextField("" + lowerLimit);
					lowerLimitTxtfield.setToolTipText(lowerLimitTooltipText);
					propPanel.add(lowerLimitTxtfield);

					JLabel upperLimitLabel = SlickerFactory.instance().createLabel("Ignore models with values above: ");
					upperLimitLabel.setToolTipText(upperLimitTooltipText);
					propPanel.add(upperLimitLabel);
					upperLimitTxtfield = new ProMTextField("" + upperLimit);
					upperLimitTxtfield.setToolTipText(upperLimitTooltipText);
					propPanel.add(upperLimitTxtfield);

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

			public double getLowerLimit() {
				try {
					return Double.parseDouble(lowerLimitTxtfield.getText());
				} catch (Exception e) {
					return -1;
				}
			}

			public double getUpperLimit() {
				try {
					return Double.parseDouble(upperLimitTxtfield.getText());
				} catch (Exception e) {
					return -1;
				}
			}
		}
	}

}
