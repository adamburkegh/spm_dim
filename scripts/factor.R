library(dplyr)
library(data.table)
library(corrplot)
library(ggplot2)
library(nFactors) 
library(psych)
library(readr)

###

workingPath = "c:/Users/burkeat/bpm/bpm-dimensions-lab/var/"
resultsPath = "c:/Users/burkeat/bpm/bpm-dimensions-lab/results/"


exportPic = TRUE

### 


norm01 <- function(vals,valfloor=0)
{
	mnv <- min(vals)-valfloor
	mxv <- max(vals)
	(vals-mnv)/(mxv-mnv)
}

imgname <- function(prefix,logname)
{
	paste(prefix,"_",logname,".png",sep="")
}


prepfig <- function(fprefix,logname)
{
    if (exportPic){
        par(mar=c(1,1,1,1))
        png( paste(workingPath,fprefix,"_",logname,".png",sep=""), 
			res=300, width=30, height=20, units='cm')
    }
}

postfig <- function()
{
    if (exportPic){
        dev.off()
    }
}

filter_for_BPIC2013_closed <- function(rd){
	# Based on the correlation matrix, these columns are identical
	# TRACE_OVERLAP_RATIO TOR == TRACE_PROBMASS_OVERLAP TMO 
	#  == EARTH_MOVERS_TRACES_nz EMT
	# TRACE_GENERALIZATION_FLOOR_1 TGF1 == TRACE_GENERALIZATION_FLOOR_5 TGF5 
	#  == TRACE_GENERALIZATION_FLOOR_10 TGF10
	# As this is an invalid matrix for factor analysis, exclude.
	#
	result <- rd %>% select (
				  TRACE_PROBMASS_OVERLAP, 
			        ACTIVITY_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  ENTROPY_PRECISION_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TRACE_GENERALIZATION_FLOOR_1,
				  TRACE_GENERALIZATION_DIFF_UNIQ  )

}

filter_for_Sepsis <- function(rd){
	# Based on the correlation matrix, these columns are identical
	# TRACE_OVERLAP_RATIO TOR == TRACE_PROBMASS_OVERLAP TMO 
	#  == EARTH_MOVERS_TRACES_nz EMT == ENTROPY_PRECISION_TRACEWISE EPT
	#  == ENTROPY_FITNESS_TRACEWISE EFT
	# TRACE_GENERALIZATION_FLOOR_5 TGF5 == TRACE_GENERALIZATION_FLOOR_10 TGF10
	# As this is an invalid matrix for factor analysis, exclude.
	#
	result <- rd %>% select (
				  TRACE_PROBMASS_OVERLAP, 
			        ACTIVITY_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TRACE_GENERALIZATION_FLOOR_1,
				  TRACE_GENERALIZATION_FLOOR_5,
				  TRACE_GENERALIZATION_DIFF_UNIQ  )

}


filter_for_all_cutdown <- function(rd){
	# Based on the correlation matrix, these columns are identical
	# TRACE_OVERLAP_RATIO TOR == TRACE_PROBMASS_OVERLAP TMO 
	# As this is an invalid matrix for factor analysis, exclude.
	# Also exclude low-KMO
	#
	result <- rd %>% select (
				  # TRACE_PROBMASS_OVERLAP, 
			        ACTIVITY_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,
				  TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  ENTROPY_PRECISION_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,,
				  TRACE_GENERALIZATION_FLOOR_1,
				  # TRACE_GENERALIZATION_FLOOR_5,
				  TRACE_GENERALIZATION_FLOOR_10 )# ,
				  # TRACE_GENERALIZATION_DIFF_UNIQ  )
}
  

filter_for_all <- function(rd){
	# Based on the correlation matrix, these columns are identical
	# TRACE_OVERLAP_RATIO TOR == TRACE_PROBMASS_OVERLAP TMO 
	# As this is an invalid matrix for factor analysis, exclude.
	#
	result <- rd %>% select (
				  TRACE_PROBMASS_OVERLAP, 
			        ACTIVITY_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  ENTROPY_PRECISION_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TRACE_GENERALIZATION_FLOOR_1,
				  TRACE_GENERALIZATION_FLOOR_5,
				  TRACE_GENERALIZATION_FLOOR_10,
				  TRACE_GENERALIZATION_DIFF_UNIQ  )
}

dev.off()
dev.new()

rundata = read.csv( paste(resultsPath,"hpc.psv", sep=""),
            sep ="|", strip.white=TRUE)

logstats <- read.csv( paste(resultsPath,"logstats.csv",sep=""))

rundata <- merge(rundata,logstats)


# ctlog <- 'BPIC2013 closed'
# ctlog <- 'BPIC2013 incidents'
# ctlog <- 'BPIC2018 control'
# ctlog <- 'Sepsis'

# ctlogns <- gsub(" ","_",ctlog)

# ctlog <- 'BPIC2013_closed and Sepsis'
ctlog <- 'BPIC2013 closed & incidents, BPIC2018 control & reference, and Sepsis'

# ctlogns <- 'b2013c_sepsis'  
ctlogns <- 'all'

nfactors <- 3

cutdown <- TRUE

resfile <-  paste(workingPath,"sfa",nfactors,ctlogns,".txt",sep="") 

write_lines(paste(ctlogns,"n=",nfactors),resfile)

pf <- function(msg){
    write_lines(msg,resfile,append=TRUE)
    write_lines(" ",resfile,append=TRUE)
}

# fa_fm = "pa"    # principal axis factoring
fa_fm = "ols"   # OLS == ordinary least squares
# fa_fm = "wls"   # WLS == weighted least squares
# fa_fm = "gls"   # GLS == generalized least squares
# fa_fm = "ml"    # ML == maximum likelihood
# fa_fm = "minres"  # minres == minimum residual



creators <- unique(rundata$Artifact.Creator)

pf("Creators")
pf(creators)

#  Excluding LOG_EVENT_COUNT,LOG_TRACE_COUNT

rundata$EARTH_MOVERS_TRACES_nz <- ifelse(rundata$EARTH_MOVERS_TRACEWISE < 0, 0, 
				rundata$EARTH_MOVERS_TRACEWISE)

rundata <- rename (rundata, ACTIVITY_RATIO_GOWER=EVENT_RATIO_GOWER)

#rd <- rundata %>% filter (Log == ctlog) %>% 
# rd <- rundata %>% filter (Log == 'BPIC2013 closed' || Log == 'Sepsis') %>% 
#		      filter ( !grepl('setm',Artifact.Creator) ) %>%
#		      filter ( !grepl('rando',Artifact.Creator) ) %>%

rdo <- rundata %>% filter ( Log == 'BPIC2013 closed' || Log == 'BPIC2013 incidents'
                        || Log == 'BPIC2018 control' || 'BPIC2018 reference'
				|| Log == 'Sepsis' ) %>%
		      select (Model.Run,
				  Log,
				  Log.Trace.Count,
				  Log.Event.Count,
				  TRACE_OVERLAP_RATIO,
				  TRACE_PROBMASS_OVERLAP,
				  EARTH_MOVERS_TRACES_nz,
			        ACTIVITY_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  ENTROPY_PRECISION_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TRACE_GENERALIZATION_FLOOR_1,
				  TRACE_GENERALIZATION_FLOOR_5,
				  TRACE_GENERALIZATION_FLOOR_10,
				  TRACE_GENERALIZATION_DIFF_UNIQ
				)

rd <- rdo %>% select (	  Log.Trace.Count,
				  Log.Event.Count,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  TRACE_GENERALIZATION_FLOOR_1,
				  TRACE_GENERALIZATION_FLOOR_5,
				  TRACE_GENERALIZATION_FLOOR_10,
				  ENTROPY_FITNESS_TRACEWISE,
				  ENTROPY_PRECISION_TRACEWISE,
				  TRACE_OVERLAP_RATIO,
				  TRACE_PROBMASS_OVERLAP,
				  EARTH_MOVERS_TRACES_nz,
				  TRACE_GENERALIZATION_DIFF_UNIQ,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC ,
			        ACTIVITY_RATIO_GOWER  )

rdshortname <- rename(rd, 
				  LTC = Log.Trace.Count,
				  LEC = Log.Event.Count,
				  TOR = TRACE_OVERLAP_RATIO ,
				  TMO = TRACE_PROBMASS_OVERLAP,
				  EMT = EARTH_MOVERS_TRACES_nz,
				  TRG2 = TRACE_RATIO_GOWER_2,
				  TRG3 = TRACE_RATIO_GOWER_3,
				  TRG4 = TRACE_RATIO_GOWER_4,
				  EPT = ENTROPY_PRECISION_TRACEWISE,
				  EFT = ENTROPY_FITNESS_TRACEWISE,
				  TGF1 = TRACE_GENERALIZATION_FLOOR_1,
				  TGF5 = TRACE_GENERALIZATION_FLOOR_5,
				  TGF10 = TRACE_GENERALIZATION_FLOOR_10,
				  TGDU = TRACE_GENERALIZATION_DIFF_UNIQ ,
				  SSENC = STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  SSEDC = STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        SSS = STRUCTURAL_SIMPLICITY_STOCHASTIC,
			        ARG = ACTIVITY_RATIO_GOWER )

pf(paste("Sample size n =",nrow(rd)))

rdcor <- cor(rdshortname)

prepfig("cor",ctlogns)
corrplot(rdcor, method="number")
postfig()

if (ctlog == 'BPIC2013 closed'){
	rd <- filter_for_BPIC2013_closed(rd)
}

if (ctlog == 'Sepsis'){
	rd <- filter_for_Sepsis(rd)
}

if (ctlogns == 'all'){
	if (cutdown){
		rd <- filter_for_all_cutdown(rd)
	}else{
		rd <- filter_for_all(rd)
	}
}


bt <- bartlett.test(rd)

pf(capture.output(bt))

km <- KMO(r=cor(rd))

pf(capture.output(km))

# Scree
ev <- eigen(cor(rd))
ap <- parallel(subject=nrow(rd),var=ncol(rd), rep=100,cent=0.5)
ns <- nScree(x=ev$values, aparallel=ap$eigen$qevpea)

prepfig("scree",ctlogns)
plotnScree(ns, main=paste("Scree Test Solutions for",ctlog) )
postfig()




fa_data <- fa(r=rd,nfactors=nfactors ,fm=fa_fm,max.iter=500,rotate="varimax",cor="cor")
#fa_data <- fa(r=rd,nfactors=nfactors ,fm=fa_fm,max.iter=500,rotate="oblimin")


prepfig(paste("fa",nfactors,sep=""),ctlogns)
fa.diagram(fa_data, 
	     main=paste("Factor analysis", 
                       paste("(",toupper(fa_fm),")", sep=""), 
                       ctlog) )
postfig()

pf(capture.output(fa_data))

# factanal
# rundata_cor <- cor(rundata)

errmsg <- function(e){
	warning(paste("factanal couldn't factor this data for n=", 
			   nfactors, " no plot produced", sep=""))
	pf(paste("factanal couldn't factor this data for n=", 
			   nfactors, " no plot produced", sep=""))
	print(e)
}

tryCatch(
	expr = {
		# message("test")
		factors_data <- factanal(rd, factors = nfactors, lower=0.1)
		pf(capture.output(factors_data))

		if (nfactors == 2){
			prepfig(paste("faa",nfactors,sep=""),ctlogns)
			plot(factors_data$loadings[,1], 
			     factors_data$loadings[,2],
			     xlab = "Factor 1", 
			     ylab = "Factor 2", 
			     ylim = c(-1,1),
			     xlim = c(-1,1),
			     main = paste("Factor analysis max likelihood (Varimax rotation)",
						 ctlog))
			text(factors_data$loadings[,1]-0.08, 
			     factors_data$loadings[,2]+0.08,
			      colnames(rd),
			      col="blue")
			abline(h = 0, v = 0)
			postfig()
		}
	},
	warning = errmsg,
	error = errmsg	
	)




