
rundata = read.csv( paste(workingPath,"hpc1.2.2.psv", sep=""),
            sep ="|", strip.white=TRUE)

grando <- rundata %>% filter ( !grepl('predef',Artifact.Creator) )

disco = read.csv( paste(workingPath,"eval2.psv", sep=""),
            sep ="|", strip.white=TRUE)

disco$ENTSUM = disco$ENTROPY_PRECISION+disco$ENTROPY_RECALL
disco$ADSIM  = disco$EARTH_MOVERS_LIGHT_COVERAGE + disco$STRUCTURAL_SIMPLICITY_STOCHASTIC
disco$ENTSIM  = disco$ENTSUM + disco$STRUCTURAL_SIMPLICITY_STOCHASTIC

# paradigm models


# High adhesion and simplicity (by earth movers), low entropy

fs <- disco %>% 
	arrange(-ADSIM,ENTSUM) %>% 
	select(EARTH_MOVERS_LIGHT_COVERAGE,STRUCTURAL_SIMPLICITY_STOCHASTIC,ENTSUM,
	       ENTROPY_PRECISION,ENTROPY_RECALL,
	       Run.file)

# Row 7: mrun_BPIC2018_reference_predef-aplh-inductive_20210630-023627.xml


# Low fitness (by earth movers), high entropy and simplicity

ps <- disco %>% arrange(-ENTSIM,EARTH_MOVERS_LIGHT_COVERAGE)  %>% 
		      select(EARTH_MOVERS_LIGHT_COVERAGE,STRUCTURAL_SIMPLICITY_STOCHASTIC,
			  	 ENTROPY_PRECISION,ENTROPY_RECALL,ENTSUM,Run.file) %>% 
			filter(EARTH_MOVERS_LIGHT_COVERAGE>0)

# mrun_BPIC2013_incidents_predef-aprh-inductive_20210628-033448.xml
# mid ARG



# High fitness (by earth-movers) and precision, low simplicity

fp <- disco %>% 
		arrange(-EARTH_MOVERS_LIGHT_COVERAGE,-ENTROPY_PRECISION,STRUCTURAL_SIMPLICITY_ENTITY_COUNT) %>% 
		select(ENTROPY_FITNESS_TRACEWISE,EARTH_MOVERS_LIGHT_COVERAGE,STRUCTURAL_SIMPLICITY_ENTITY_COUNT,ENTROPY_PRECISION,EVENT_RATIO_GOWER,Run.file)  


# osmodel_align-fodina_BPIC2018_control.pnml ??
# 
# mrun_BPIC2018_control_predef-msaprh-split_20210627-052222.xml


# High fitness (by entropy fitness tracewise) , high ARG, low simplicity

fptg <- grando %>% 
		arrange(-ENTROPY_FITNESS_TRACEWISE,-EVENT_RATIO_GOWER,STRUCTURAL_SIMPLICITY_ENTITY_COUNT) %>% 
		select(ENTROPY_FITNESS_TRACEWISE,STRUCTURAL_SIMPLICITY_ENTITY_COUNT,EVENT_RATIO_GOWER,Run.file,Model.Run)  

fptd <- disco %>%
		arrange(-ENTROPY_FITNESS_TRACEWISE,-EVENT_RATIO_GOWER,STRUCTURAL_SIMPLICITY_ENTITY_COUNT) %>% 
		select(EARTH_MOVERS_LIGHT_COVERAGE,ENTROPY_FITNESS_TRACEWISE,ENTROPY_PRECISION,STRUCTURAL_SIMPLICITY_ENTITY_COUNT,EVENT_RATIO_GOWER,Run.file,Model.Run)  



# mrun_BPIC2018_control_predef-msaprh-split_20210627-052222.xml


# High fitness (by entropy fitness tracewise), simplicity, precision, low ARG
# mrun_BPIC2018_reference_predef-msaprh-inductive_20210630-023438.xml

# High fitness (by entropy fitness tracewise), mid simplicity, high ARG
# Non-predef setm-s2ts20210624-052411-g92.pnml

# Low fitness, low simplicity, high ARG

fptlsg <- grando %>% 
		arrange(ENTROPY_FITNESS_TRACEWISE,-STRUCTURAL_SIMPLICITY_ENTITY_COUNT,-EVENT_RATIO_GOWER) %>% 
		select(ENTROPY_FITNESS_TRACEWISE,STRUCTURAL_SIMPLICITY_ENTITY_COUNT,EVENT_RATIO_GOWER,Run.file,Model.Run)  %>%
		filter(STRUCTURAL_SIMPLICITY_ENTITY_COUNT>0) %>%
		filter(ENTROPY_FITNESS_TRACEWISE>0)

#   ENTROPY_FITNESS_TRACEWISE STRUCTURAL_SIMPLICITY_ENTITY_COUNT EVENT_RATIO_GOWER                                             Run.file   Model.Run
#  mrun_BPIC2013_incidents_rando-s2_20210628-023617.xml rando-s2m43

