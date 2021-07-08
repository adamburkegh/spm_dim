package qut.pm.spm;

import org.simpleframework.xml.Root;

@Root
public enum Measure {
	LOG_EVENT_COUNT("Log Events"), 
	LOG_TRACE_COUNT("Log Traces"), 
	MODEL_ENTITY_COUNT("Entities"), 
	MODEL_EDGE_COUNT("Edges"), 
	NORMALIZED_PETRI_NET_EDGE_SIMPLICITY,
	EARTH_MOVERS_LIGHT_COVERAGE("tEMSC 0.8"), // Leemans 2019
	EARTH_MOVERS_SIMILARITY,
	ENTROPY("H"),
	ENTROPY_PRECISION("H_precision"), 
	ENTROPY_RECALL("H_recall"), // Leemans and Polyvyany 2020 entropy quality measures 
	/** EVENT_RATIO_GOWER should be ACTIVITY_RATIO_GOWER
	 * Maintain older name for experiment result back-compatibility
	 */
	EVENT_RATIO_GOWER("Activity ratios using Gower Similarity"), 
	TRACE_RATIO_GOWER_2("Subtrace ratios (2) using Gower Similarity"),
	TRACE_RATIO_GOWER_3("Subtrace ratios (3) using Gower Similarity"),
	TRACE_RATIO_GOWER_4("Subtrace ratios (4) using Gower Similarity"),
	TRACE_OVERLAP_RATIO("Trace intersection ratio"),
	TRACE_PROBMASS_OVERLAP("Trace probability mass overlap"),
	EARTH_MOVERS_TRACEWISE("uEMSC Trace"),
	ENTROPY_TRACEWISE("H_trace"),
	ENTROPY_FITNESS_TRACEWISE("H_precision_trace"),
	ENTROPY_PRECISION_TRACEWISE("H_fitness_trace"),
	MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY("C_sscunq"),
	STRUCTURAL_SIMPLICITY_ENTITY_COUNT("S_entities"),
	STRUCTURAL_SIMPLICITY_EDGE_COUNT("S_edges"),
	STRUCTURAL_SIMPLICITY_STOCHASTIC("S_sssunq"),
	TRACE_GENERALIZATION_FLOOR_1("gen_q_1"),
	TRACE_GENERALIZATION_FLOOR_5("gen_q_5"),
	TRACE_GENERALIZATION_FLOOR_10("gen_q_10"),
	TRACE_GENERALIZATION_FLOOR_20("gen_q_20"),
	TRACE_GENERALIZATION_DIFF_UNIQ("gen_HB"),
	PROB_PROCESS_TREE_DETERMINISM("PPT_beta");
	
	private String text = null;
	
	private Measure(String text) {
		this.text = text;
	}
	
	private Measure() {
	}
	
	public String getText() {
		if (text == null) {
			String result = name().replace("_", " ");
			return result;
		}
		return text;
	}
}