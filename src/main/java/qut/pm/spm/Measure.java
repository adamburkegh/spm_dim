package qut.pm.spm;

import org.simpleframework.xml.Root;

@Root
public enum Measure {
	LOG_EVENT_COUNT("Log Events"), 
	LOG_TRACE_COUNT("Log Traces"), 
	MODEL_ENTITY_COUNT("Entities"), 
	MODEL_EDGE_COUNT("Edges"), 
	NORMALIZED_PETRI_NET_EDGE_SIMPLICITY,
	ALPHA_PRECISION_UNRESTRICTED_ZERO("alpha-precision UR 0"), // Depaire 2022
	ALPHA_PRECISION_UNRESTRICTED_1_PCT("alpha-precision UR 0.01"), 
	ALPHA_PRECISION_UNRESTRICTED_2_PCT("alpha-precision UR 0.02"), 
	ALPHA_PRECISION_UNRESTRICTED_5_PCT("alpha-precision UR 0.05"), 
	// Alpha precision restricted is equivalent to unrestricted 
	// when alpha-sig = 0. So there is no ALPHA_PRECISION_RESTRICTED_1_PCT
	ALPHA_PRECISION_RESTRICTED_1_PCT("alpha-precision RES 0.01"), // Depaire 2022
	ALPHA_PRECISION_RESTRICTED_2_PCT("alpha-precision RES 0.02"), 
	ALPHA_PRECISION_RESTRICTED_5_PCT("alpha-precision RES 0.05"),
	EXISTENTIAL_PRECISION,
	EARTH_MOVERS_LIGHT_COVERAGE("tEMSC 0.8"), // Leemans 2019
	EARTH_MOVERS_SIMILARITY,
	ENTROPIC_RELEVANCE_UNIFORM("HRU"),
	ANTI_ENTROPIC_RELEVANCE_ZERO_ORDER_BRUTE("AHRUBR"),
	ENTROPIC_RELEVANCE_ZERO_ORDER_BRUTE("HRUBR"),
	ENTROPIC_RELEVANCE_ZERO_ORDER("HRZ"),
	ENTROPIC_RELEVANCE_RESTRICTED_ZO("HRR"),
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
	ENTROPY_FITNESS_TRACEWISE("H_fitness_trace"),
	ENTROPY_FITNESS_TRACEPROJECT("H_fitness_tproj"),
	ENTROPY_PRECISION_TRACEWISE("H_precision_trace"),
	ENTROPY_PRECISION_TRACEPROJECT("H_precision_tprjo"),
	MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY("C_sscunq"),
	STRUCTURAL_SIMPLICITY_ENTITY_COUNT("S_entities"),
	STRUCTURAL_SIMPLICITY_EDGE_COUNT("S_edges"),
	STRUCTURAL_SIMPLICITY_STOCHASTIC("S_sssunq"),
	STRUCTURAL_SIMPLICITY_EDGE_COUNT_SMALL_LOG("S_edges_sl"),
	TRACE_GENERALIZATION_FLOOR_1("gen_q_1"),
	TRACE_GENERALIZATION_FLOOR_5("gen_q_5"),
	TRACE_GENERALIZATION_FLOOR_10("gen_q_10"),
	TRACE_GENERALIZATION_FLOOR_20("gen_q_20"),
	TRACE_GENERALIZATION_DIFF_UNIQ("gen_HB"),
	PROB_PROCESS_TREE_DETERMINISM("PPT_beta"),
	ADHESION_DR("ADM"),
	RELEVANCE_DR("RDM"),
	SIMPLICITY_DR("SDM"),
	ADHESION_DR_SL,
	RELEVANCE_DR_SL,
	SIMPLICITY_DR_SL("SDM_SL") ; 
	
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