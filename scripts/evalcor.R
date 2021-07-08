library(dplyr)
library(data.table)
library(corrplot)

workingPath = "c:/Users/burkeat/bpm/bpm-dimensions-lab/var/"
resultsPath = "c:/Users/burkeat/bpm/bpm-dimensions-lab/results/"


exportPic = TRUE


imgname <- function(prefix,logname)
{
	paste(prefix,"_",logname,".png",sep="")
}


prepfig <- function(fprefix,logname, width=30, height=20)
{
    if (exportPic){
        par(mar=c(1,1,1,1))
        png( paste(workingPath,fprefix,"_",logname,".png",sep=""), 
			res=300, width=width, height=height, units='cm')
    }
}

postfig <- function()
{
    if (exportPic){
        dev.off()
    }
}

corheatmap <- function(pcain,mname,source,
		 		dimlabels=c("S1-Fitness","Precision","Simplicity","Trace Profile"))
{
      pcv <- get_pca_var(pcain)
	# pci <- get_pca_ind(pcain)

	pv <- pcv$contrib[,1:nfactors]
	colnames(pv) <- dimlabels

	prepfig("hmevalcont", mname)
	col= colorRampPalette(brewer.pal(8, "Blues"))(25)
	heatmap(pv, Colv = NA, Rowv = NA,margins=c(10,21), col= col) #,
		  # main=paste(source,"contributions") )
	postfig()

	pv <- pcv$cos2[,1:nfactors]
	colnames(pv) <- dimlabels

	prepfig("hmevalcos2",mname)
	col= colorRampPalette(brewer.pal(8, "Blues"))(25)
	heatmap(pv, Colv = NA, Rowv = NA,margins=c(10,21), col= col) #,
		  # main=paste(source,"cos^2"))
	postfig()
}

ctlog <- 'BPIC2013 closed & incidents, BPIC2018 control & reference, and Sepsis'

ctlogns <- 'all'

nfactors <- 4

scree <- TRUE

dev.off()
dev.new()

resfile <-  paste(workingPath,"eval",nfactors,ctlogns,".txt",sep="") 

write_lines(paste(ctlogns,"n=",nfactors),resfile)

pf <- function(msg){
    write_lines(msg,resfile,append=TRUE)
    write_lines(" ",resfile,append=TRUE)
}


pfc <- function(msg){
    write_lines(capture.output(msg),resfile,append=TRUE)
    write_lines(" ",resfile,append=TRUE)
}

rundata = read.csv( paste(workingPath,"eval.psv", sep=""),
            sep ="|", strip.white=TRUE)

logstats <- read.csv( paste(resultsPath,"logstats.csv",sep=""))

rundata <- merge(rundata,logstats)


rundata$EARTH_MOVERS_TRACES_nz <- ifelse(rundata$EARTH_MOVERS_TRACEWISE < 0, 0, 
				rundata$EARTH_MOVERS_TRACEWISE)

#rd <- rundata %>% filter (Log == ctlog) %>% 
# rd <- rundata %>% filter (Log == 'BPIC2013 closed' || Log == 'Sepsis') %>% 
#		      filter ( !grepl('setm',Artifact.Creator) ) %>%
#		      filter ( !grepl('rando',Artifact.Creator) ) %>%

rdo <- rundata %>% select (# Model.Run,
				  # Log,
				  # Log.Trace.Count,
				  # Log.Event.Count,
				  TRACE_GENERALIZATION_FLOOR_10,
				  TRACE_GENERALIZATION_FLOOR_5,
				  TRACE_GENERALIZATION_FLOOR_1,
			        EVENT_RATIO_GOWER,
				  TRACE_OVERLAP_RATIO,
				  TRACE_PROBMASS_OVERLAP,
				  TRACE_GENERALIZATION_DIFF_UNIQ,
				  EARTH_MOVERS_TRACES_nz,
				  ENTROPY_RECALL,
				  ENTROPY_PRECISION,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  ENTROPY_PRECISION_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,
				  EARTH_MOVERS_LIGHT_COVERAGE,
				  TRACE_RATIO_GOWER_4,
				  TRACE_RATIO_GOWER_3,TRACE_RATIO_GOWER_2
				)

rdo <- rename(rdo,EARTH_MOVERS_TRACES=EARTH_MOVERS_TRACES_nz)


rdearth <- rdo %>% filter ( !is.na(EARTH_MOVERS_LIGHT_COVERAGE ) ) %>%
		       select (-ENTROPY_PRECISION,-ENTROPY_RECALL)

rdent <- rdo %>% filter ( !is.na(ENTROPY_PRECISION) ) %>%
		       select (-EARTH_MOVERS_LIGHT_COVERAGE)

rdee <- rdo %>% filter (!is.na(ENTROPY_PRECISION) ) %>% 
		    filter (!is.na(EARTH_MOVERS_LIGHT_COVERAGE))

emshortname <- rename(rdearth, 
				  TOR = TRACE_OVERLAP_RATIO ,
				  TMO = TRACE_PROBMASS_OVERLAP,
				  EMT = EARTH_MOVERS_TRACES,
				  EM = EARTH_MOVERS_LIGHT_COVERAGE,
			        ERG = EVENT_RATIO_GOWER,
				  TRG2 = TRACE_RATIO_GOWER_2,
				  TRG3 = TRACE_RATIO_GOWER_3,
				  TRG4 = TRACE_RATIO_GOWER_4,
				  EPT = ENTROPY_PRECISION_TRACEWISE,
				  EFT = ENTROPY_FITNESS_TRACEWISE,
				  SSENC = STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  SSEDC = STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        SSS = STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TGF1 = TRACE_GENERALIZATION_FLOOR_1,
				  TGF5 = TRACE_GENERALIZATION_FLOOR_5,
				  TGF10 = TRACE_GENERALIZATION_FLOOR_10,
				  TGDU = TRACE_GENERALIZATION_DIFF_UNIQ  )

emcor <- cor(emshortname)

pf("Earth movers")
pf(paste("Sample size n =",nrow(rdearth)))
pfc(emcor)

prepfig("evalcorem",ctlogns, width=30, height=10)
corrplot(emcor[7:8,], method="number")
postfig()


rdent <- rdo %>% filter ( !is.na(ENTROPY_PRECISION) ) %>%
		       select (-EARTH_MOVERS_LIGHT_COVERAGE)

entshortname <- rename(rdent, 
				  TOR = TRACE_OVERLAP_RATIO ,
				  TMO = TRACE_PROBMASS_OVERLAP,
				  EMT = EARTH_MOVERS_TRACES,
			        ERG = EVENT_RATIO_GOWER,
				  TRG2 = TRACE_RATIO_GOWER_2,
				  TRG3 = TRACE_RATIO_GOWER_3,
				  TRG4 = TRACE_RATIO_GOWER_4,
				  EPT = ENTROPY_PRECISION_TRACEWISE,
				  EP = ENTROPY_PRECISION,
				  EFT = ENTROPY_FITNESS_TRACEWISE,
			        ER  = ENTROPY_RECALL,
				  SSENC = STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  SSEDC = STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        SSS = STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TGF1 = TRACE_GENERALIZATION_FLOOR_1,
				  TGF5 = TRACE_GENERALIZATION_FLOOR_5,
				  TGF10 = TRACE_GENERALIZATION_FLOOR_10,
				  TGDU = TRACE_GENERALIZATION_DIFF_UNIQ  )

eeshortname <- rename(rdee, 
				  TOR = TRACE_OVERLAP_RATIO ,
				  TMO = TRACE_PROBMASS_OVERLAP,
				  EMT = EARTH_MOVERS_TRACES,
				  EM = EARTH_MOVERS_LIGHT_COVERAGE,
			        ERG = EVENT_RATIO_GOWER,
				  TRG2 = TRACE_RATIO_GOWER_2,
				  TRG3 = TRACE_RATIO_GOWER_3,
				  TRG4 = TRACE_RATIO_GOWER_4,
				  EPT = ENTROPY_PRECISION_TRACEWISE,
				  EP = ENTROPY_PRECISION,
				  EFT = ENTROPY_FITNESS_TRACEWISE,
			        ER  = ENTROPY_RECALL,
				  SSENC = STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  SSEDC = STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        SSS = STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TGF1 = TRACE_GENERALIZATION_FLOOR_1,
				  TGF5 = TRACE_GENERALIZATION_FLOOR_5,
				  TGF10 = TRACE_GENERALIZATION_FLOOR_10,
				  TGDU = TRACE_GENERALIZATION_DIFF_UNIQ  )

entcor <- cor(entshortname)

pf("Entropy")
pf(paste("Sample size n =",nrow(rdent)))
pfc(entcor)

prepfig("evalcorent",ctlogns, width=30, height=10)
corrplot(entcor[c(8:11),], method="number")
postfig()

pcaearth <- prcomp(rdearth, scale = scalepca)

pcaent <- prcomp(rdent, scale = scalepca)

pcaee <- prcomp(rdee, scale=scalepca)

eecor <- cor(eeshortname)

pf("All eval")
pf(paste("Sample size n =",nrow(rdee)))
pfc(emcor)

prepfig("evalcor",ctlogns, width=30, height=30)
corrplot(eecor, method="number")
postfig()



pcaPlots <- FALSE
if (pcaPlots){

	var <- pcaearth

	# var <- pcaent


	fviz_pca_ind(var,
             col.ind = "cos2", # Color by the quality of representation
             gradient.cols =  colours ,
             repel = FALSE,     # Avoid text overlapping
		 label = "none",
             ) +   
		labs(title ="PCA", x = "PC1", y = "PC2")



	fviz_pca_ind(var,
             repel = FALSE,     # Avoid text overlapping
		 label = "none",
		 addEllipses=TRUE, ellipse.level=0.95,
		 habillage = rdo$Log
             ) +   
		labs(title ="PCA", x = "PC1", y = "PC2")



	fviz_pca_var(var,
             col.var = "contrib", # Color by contributions to the PC
             gradient.cols = colours,
             repel = FALSE ,    # Avoid text overlapping
		 label = "none"
             )


	fviz_pca_biplot(var, repel = FALSE,
                col.var = "#2E9FDF", # Variables color
                col.ind = "#696969",  # Individuals color
                label = "var")

# prepfig("heatmap",ctlogns)
col= colorRampPalette(brewer.pal(8, "Blues"))(25)
heatmap(pv, Colv = NA, Rowv = NA,margins=c(10,21), col= col)
# postfig()

}




corheatmap(pcaearth,"em","Earth Movers Eval" )
corheatmap(pcaent,"ent","Entropy Eval")
corheatmap(pcaee,"eval","Eval", dimlabels=c("S1-Fitness","Simplicity","Precision","Trace Profile"))




if (scree){
	#scree
	# prepfig("screeeigem",pcaearth)
	# fviz_eig(pcaearth)
	# fviz_eig(pcaent)
	prepfig("screeeval","eval")
	fviz_eig(pcaee)
	postfig()
}

