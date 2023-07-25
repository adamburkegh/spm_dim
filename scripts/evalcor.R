library(readr)
library(dplyr)
library(tidyr)
library(data.table)
library(factoextra)
library(corrplot)
library(RColorBrewer)

hpath = "c:/Users/burkeat/bpm/"
# hpath = "c:/Users/Adam/bpm/"

workingPath = paste(hpath,"bpm-dimensions-lab/var/",sep="")
resultsPath = paste(hpath,"bpm-dimensions-lab/results/",sep="")

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
    graphics.off()
  }
}

corheatmap <- function(pcain,mname,source,
                       dimlabels=c("Adhesion","Precision","Simplicity","Trace Profile") )
{
  pcv <- get_pca_var(pcain)
  # pci <- get_pca_ind(pcain)
  scale <- 'none'
  # scale <- 'column'
  
  pv <- pcv$contrib[,1:nfactors]
  colnames(pv) <- dimlabels
  
  prepfig("hmevalcont", paste(mname,nfactors,sep=""))
  col= colorRampPalette(brewer.pal(8, "Blues"))(25)
  heatmap(pv, Colv = NA, Rowv = NA,margins=c(10,21), col= col,
          cexRow = 1.0, cexCol = 1.5 , 
          scale = scale)
  postfig()
  
  pv <- pcv$cos2[,1:nfactors]
  colnames(pv) <- dimlabels
  
  prepfig("hmevalcos2",paste(mname,nfactors,sep=""))
  col= colorRampPalette(brewer.pal(8, "Blues"))(25)
  heatmap(pv, Colv = NA, Rowv = NA,margins=c(10,21), col= col,
          cexRow = 1.0, cexCol = 1.5,
          scale = scale)
  # main=paste(source,"cos^2"))
  postfig()
}

corheatmaprowlabel <- function(pcain,mname,source,
                               dimlabels, rowlabels )
{
  pcv <- get_pca_var(pcain)
  
  pv <- pcv$contrib[,1:nfactors]
  colnames(pv) <- dimlabels
  rlbackup <- rownames(pv)
  rownames(pv) <- rowlabels
  
  prepfig("hmevalcont", mname)
  col= colorRampPalette(brewer.pal(8, "Blues"))(25)
  #heatmap(pv, Colv = NA, Rowv = NA,margins=c(10,21), col= col,
  #        cexRow = 1.0, cexCol = 1.5 )
  heatmap(pv, Colv = NA, Rowv = NA,margins=c(10,21), col= col,
          cexRow = 1.0, 
          labCol="",
          add.expr = text(x = seq_along(colnames(pv)),
                          y=-1.5, srt=45, labels=colnames(pv), 
                          xpd=TRUE, cex=2.0) )
  legend(x="bottomright", legend=c("min","mid","max"),
         fill=colorRampPalette(brewer.pal(8, "Blues"))(3),
         inset=c(0.17,0.55), bty="n" )
  rect(xleft = 0.75, xright = 0.905, ybottom = 0.56, ytop = 0.76) 
  text(x = 0.755, y = 0.70, adj = c(0,0),
       "Scaled contributions\nper component", cex=1.0 ) 
  
  # main=paste(source,"contributions") )
  postfig()
  
  pv <- pcv$cos2[,1:nfactors]
  colnames(pv) <- dimlabels
  
  prepfig("hmevalcos2",mname)
  col= colorRampPalette(brewer.pal(8, "Blues"))(25)
  heatmap(pv, Colv = NA, Rowv = NA,margins=c(10,21), col= col,
          cexRow = 1.0, cexCol = 1.5 )
  # main=paste(source,"cos^2"))
  postfig()
}

pfpca <- function(title,pcain){
  pcavar <- get_pca_var(pcain)
  pf(title)
  pf(paste("PCA contrib",title))
  pfc(pcavar$contrib[,1:nfactors])
  pf(paste("PCA cos2",title))
  pfc(pcavar$cos2[,1:nfactors])
}


pcaTopFeatures <- function(pcam,thresh){
  xd <- as.data.frame(pcam)
  res <- list()
  res$d1 <- xd[order(row.names(xd)), ] %>% filter (abs(Dim.1) > thresh) %>% select(Dim.1)
  res$d2 <- xd[order(row.names(xd)), ] %>% filter (abs(Dim.2) > thresh) %>% select(Dim.2)
  res$d3 <- xd[order(row.names(xd)), ] %>% filter (abs(Dim.3) > thresh) %>% select(Dim.3)
  res$d4 <- xd[order(row.names(xd)), ] %>% filter (abs(Dim.4) > thresh) %>% select(Dim.4)
  res
}



ctlog <- 'BPIC2013 closed & incidents, BPIC2018 control & reference, Road Traffic Fines and Sepsis'

ctlogns <- 'all'

#nfactors <- 6
#nfactors <- 5
#nfactors <- 4
nfactors <- 3

scree <- TRUE

matchSampleSizes <- TRUE

scalepca <- TRUE

dev.off()
dev.new()

resfile <-  paste(workingPath,"eval3",nfactors,ctlogns,".txt",sep="") 

write_lines(paste(ctlogns,"n=",nfactors),resfile)

pf <- function(msg){
  write_lines(msg,resfile,append=TRUE)
  write_lines(" ",resfile,append=TRUE)
}


pfc <- function(msg){
  write_lines(capture.output(msg),resfile,append=TRUE)
  write_lines(" ",resfile,append=TRUE)
}

rundata = read.csv( paste(workingPath,"eval3.psv", sep=""),
                    sep ="|", strip.white=TRUE)

rundata$ENTROPY_PRECISION_TRACEPROJECT <- ifelse(rundata$ENTROPY_PRECISION_TRACEPROJECT == "-0.0", 0, 
                                                 rundata$ENTROPY_PRECISION_TRACEPROJECT)
rundata$ENTROPY_FITNESS_TRACEPROJECT <- ifelse(rundata$ENTROPY_FITNESS_TRACEPROJECT == "-0.0", 0, 
                                               rundata$ENTROPY_FITNESS_TRACEPROJECT)

rundata <- rename(rundata,ACTIVITY_RATIO_GOWER=EVENT_RATIO_GOWER,
                  EARTH_MOVERS=EARTH_MOVERS_LIGHT_COVERAGE)

gt <- rundata %>% 
  select(Artifact.Creator) %>% 
  separate(Artifact.Creator,c("gtype","dtype"),sep="-",extra="merge") 

rundata$gtype <- gt$gtype


logstats <- read.csv( paste(resultsPath,"logstats.csv",sep=""))

rundata <- merge(rundata,logstats)


rundata$EARTH_MOVERS_TRACEWISE <- ifelse(rundata$EARTH_MOVERS_TRACEWISE < 0, 0, 
                                         rundata$EARTH_MOVERS_TRACEWISE)




#rd <- rundata %>% filter (Log == ctlog) %>% 
# rd <- rundata %>% filter (Log == 'BPIC2013 closed' || Log == 'Sepsis') %>% 
#		      filter ( !grepl('setm',Artifact.Creator) ) %>%
#		      filter ( !grepl('rando',Artifact.Creator) ) %>%

lrdo <- rundata %>% select (# Model.Run,
  # Log,
  Log.Trace.Count,
  Log.Event.Count,
  TRACE_OVERLAP_RATIO,
  TRACE_GENERALIZATION_DIFF_UNIQ,
  EARTH_MOVERS_TRACEWISE,
  STRUCTURAL_SIMPLICITY_STOCHASTIC,
  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
  ENTROPY_RECALL,
  ENTROPY_PRECISION_TRACEWISE,
  ENTROPY_PRECISION,
  TRACE_RATIO_GOWER_4,
  TRACE_RATIO_GOWER_3,				  
  TRACE_RATIO_GOWER_2,
  ACTIVITY_RATIO_GOWER,
  ENTROPY_FITNESS_TRACEWISE,
  ENTROPY_PRECISION_TRACEPROJECT,
  ENTROPY_FITNESS_TRACEPROJECT,
  TRACE_GENERALIZATION_FLOOR_5,				  
  EARTH_MOVERS,
  MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,
  ALPHA_PRECISION_UNRESTRICTED_ZERO,
  ENTROPIC_RELEVANCE_RESTRICTED_ZO,
  ENTROPIC_RELEVANCE_UNIFORM,
  ENTROPIC_RELEVANCE_ZERO_ORDER
)

rdo <- lrdo %>% select (-Log.Trace.Count,-Log.Event.Count)

rdearth <- rdo %>% filter ( !is.na(EARTH_MOVERS ) ) %>%
  select (-ENTROPY_PRECISION,-ENTROPY_RECALL)

rdent <- rdo %>% filter ( !is.na(ENTROPY_PRECISION) ) %>%
  select (-EARTH_MOVERS)

rdee <- rdo %>% filter (!is.na(ENTROPY_PRECISION) ) %>% 
  filter (!is.na(EARTH_MOVERS))

ldee <- lrdo %>% filter (!is.na(ENTROPY_PRECISION) ) %>% 
  filter (!is.na(EARTH_MOVERS))

logshortname <- rename(ldee,
                       LTC = Log.Trace.Count,
                       LEC = Log.Event.Count,
                       TOR = TRACE_OVERLAP_RATIO ,
                       EMT = EARTH_MOVERS_TRACEWISE,
                       EM = EARTH_MOVERS,
                       ARG = ACTIVITY_RATIO_GOWER,
                       TRG2 = TRACE_RATIO_GOWER_2,
                       TRG3 = TRACE_RATIO_GOWER_3,
                       TRG4 = TRACE_RATIO_GOWER_4,
                       HP = ENTROPY_PRECISION,
                       HF  = ENTROPY_RECALL,
                       HIPT = ENTROPY_PRECISION_TRACEWISE,
                       HIFT = ENTROPY_FITNESS_TRACEWISE,
                       SSENC = STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
                       SSEDC = STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                       SSS = STRUCTURAL_SIMPLICITY_STOCHASTIC,
                       TGF5 = TRACE_GENERALIZATION_FLOOR_5,
                       TGDU = TRACE_GENERALIZATION_DIFF_UNIQ,
                       HJPT = ENTROPY_PRECISION_TRACEPROJECT,
                       HJFT = ENTROPY_FITNESS_TRACEPROJECT,
                       CSS = MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,
                       APU0 = ALPHA_PRECISION_UNRESTRICTED_ZERO,
                       HRU = ENTROPIC_RELEVANCE_UNIFORM,
                       HRZ = ENTROPIC_RELEVANCE_ZERO_ORDER,
                       HRR = ENTROPIC_RELEVANCE_RESTRICTED_ZO
)

emshortname <- rename(rdearth, 
                      TOR = TRACE_OVERLAP_RATIO ,
                      EMT = EARTH_MOVERS_TRACEWISE,
                      EM = EARTH_MOVERS,
                      ARG = ACTIVITY_RATIO_GOWER,
                      TRG2 = TRACE_RATIO_GOWER_2,
                      TRG3 = TRACE_RATIO_GOWER_3,
                      TRG4 = TRACE_RATIO_GOWER_4,
                      HIPT = ENTROPY_PRECISION_TRACEWISE,
                      HIFT = ENTROPY_FITNESS_TRACEWISE,
                      SSENC = STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
                      SSEDC = STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                      SSS = STRUCTURAL_SIMPLICITY_STOCHASTIC,
                      TGF5 = TRACE_GENERALIZATION_FLOOR_5,
                      TGDU = TRACE_GENERALIZATION_DIFF_UNIQ,
                      HJPT = ENTROPY_PRECISION_TRACEPROJECT,
                      HJFT = ENTROPY_FITNESS_TRACEPROJECT,
                      CSS = MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,
                      APU0 = ALPHA_PRECISION_UNRESTRICTED_ZERO,
                      HRU = ENTROPIC_RELEVANCE_UNIFORM,
                      HRZ = ENTROPIC_RELEVANCE_ZERO_ORDER,
                      HRR = ENTROPIC_RELEVANCE_RESTRICTED_ZO
)

emcor <- cor(emshortname)

pf("Earth movers")
pf(paste("Sample size n =",nrow(rdearth)))
pfc(emcor)

prepfig("evalcorem",ctlogns, width=30, height=10)
corrplot(emcor[c("EM","EMT"),], method="number")
postfig()


rdent <- rdo %>% filter ( !is.na(ENTROPY_PRECISION) ) %>%
  select (-EARTH_MOVERS)

entshortname <- rename(rdent, 
                       TOR = TRACE_OVERLAP_RATIO ,
                       EMT = EARTH_MOVERS_TRACEWISE,
                       ARG = ACTIVITY_RATIO_GOWER,
                       TRG2 = TRACE_RATIO_GOWER_2,
                       TRG3 = TRACE_RATIO_GOWER_3,
                       TRG4 = TRACE_RATIO_GOWER_4,
                       HIPT = ENTROPY_PRECISION_TRACEWISE,
                       HP = ENTROPY_PRECISION,
                       HIFT = ENTROPY_FITNESS_TRACEWISE,
                       HF  = ENTROPY_RECALL,
                       SSENC = STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
                       SSEDC = STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                       SSS = STRUCTURAL_SIMPLICITY_STOCHASTIC,
                       TGF5 = TRACE_GENERALIZATION_FLOOR_5,
                       TGDU = TRACE_GENERALIZATION_DIFF_UNIQ,
                       HJPT = ENTROPY_PRECISION_TRACEPROJECT,
                       HJFT = ENTROPY_FITNESS_TRACEPROJECT,
                       CSS = MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,
                       APU0 = ALPHA_PRECISION_UNRESTRICTED_ZERO,
                       HRU = ENTROPIC_RELEVANCE_UNIFORM,
                       HRZ = ENTROPIC_RELEVANCE_ZERO_ORDER,
                       HRR = ENTROPIC_RELEVANCE_RESTRICTED_ZO
)


# Try without complexity
# rdee <- rdee %>% select(-MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY)

eeshortname <- rename(rdee, 
                      TOR = TRACE_OVERLAP_RATIO ,
                      EMT = EARTH_MOVERS_TRACEWISE,
                      EM = EARTH_MOVERS,
                      ARG = ACTIVITY_RATIO_GOWER,
                      TRG2 = TRACE_RATIO_GOWER_2,
                      TRG3 = TRACE_RATIO_GOWER_3,
                      TRG4 = TRACE_RATIO_GOWER_4,
                      HIPT = ENTROPY_PRECISION_TRACEWISE,
                      HP = ENTROPY_PRECISION,
                      HIFT = ENTROPY_FITNESS_TRACEWISE,
                      HF  = ENTROPY_RECALL,
                      SSENC = STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
                      SSEDC = STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                      SSS = STRUCTURAL_SIMPLICITY_STOCHASTIC,
                      TGF5 = TRACE_GENERALIZATION_FLOOR_5,
                      TGDU = TRACE_GENERALIZATION_DIFF_UNIQ,
                      HJPT = ENTROPY_PRECISION_TRACEPROJECT,
                      HJFT = ENTROPY_FITNESS_TRACEPROJECT,
                      CSS = MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,
                      APU0 = ALPHA_PRECISION_UNRESTRICTED_ZERO,
                      HRU = ENTROPIC_RELEVANCE_UNIFORM,
                      HRZ = ENTROPIC_RELEVANCE_ZERO_ORDER,
                      HRR = ENTROPIC_RELEVANCE_RESTRICTED_ZO
)

entcor <- cor(entshortname)

pf("Entropy")
pf(paste("Sample size n =",nrow(rdent)))
pfc(entcor)

prepfig("evalcorent",ctlogns, width=30, height=10)
corrplot(entcor[c("HP","HIPT","HJPT","HF","HIFT","HJFT","HRU","HRZ","HRR"),], method="number")
postfig()


pcaearth <- prcomp(rdearth, scale = scalepca)

pcaent <- prcomp(rdent, scale = scalepca)

pcaee <- prcomp(rdee, scale=scalepca)

eecor <- cor(eeshortname)

logcor <- cor(logshortname)



pfpca("Entropy PCA",pcaent)

pfpca("Earth movers PCA",pcaearth)
pfc(emcor)

pf("All eval PCA")
pf(paste("Sample size n =",nrow(rdee)))
pfpca("Eval all",pcaee)


prepfig("evalcor",ctlogns, width=30, height=30)
corrplot(eecor, method="number")
postfig()

prepfig("evalcorlog",ctlogns, width=30, height=30)
corrplot(logcor, method="number")
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
  # col= colorRampPalette(brewer.pal(8, "Blues"))(25)
  # heatmap(var, Colv = NA, Rowv = NA,margins=c(10,21), col= col)
  # postfig()
  
}


if (nfactors == 3){
  dlabels <- c("Adhesion\n(PCA1)","Simplicity\n(PCA2)","Relevance\n(PCA3)")
}
if (nfactors == 4){
  dlabels <- c("Adhesion","Entropy","Simplicity","Subtraceability")
}
if (nfactors == 5){
  dlabels <- c("Adhesion","Simplicity","Trace Profile","Precision","Thingy")
}
if (nfactors == 6){
  dlabels <- c("Adhesion","Simplicity","Relevance","ST1","ST2","Thingy")
}

rlabels <- c(21)
rlabels[1] <- c("Trace Overlap Ratio (TOR)")
rlabels[2] <- c("Trace Probability Mass Overlap (TMO)")
rlabels[3] <- c("Generalization by Trace Uniqueness (TGDU)")
rlabels[4] <- c("Earth Movers With Play-out Trace (EMT)")
rlabels[5] <- c("Structural Simplicity incl. Stochastic (SSS)")
rlabels[6] <- c("Structural Simplicity by Entity Count (SSENC)")
rlabels[7] <- c("Structural Simplicity by Edge Count  (SSEDC)")
rlabels[8] <- c("Entropy Recall (H_F)")
rlabels[9] <- c("Play-out Entropy Intersection Precision (HIPT)")
rlabels[10] <- c("Entropy Precision (H_P)")
rlabels[11] <- c("Trace Ratio Gower 4 (TRG4)")
rlabels[12] <- c("Trace Ratio Gower 3 (TRG3)")
rlabels[13] <- c("Trace Ratio Gower 2 (TRG2)")
rlabels[14] <- c("Activity Ratio Gower (ARG)")
rlabels[15] <- c("Play-out Entropy Intersection Fitness (HIFT)")
rlabels[16] <- c("Play-out Entropy Project Precision (HJPT)")
rlabels[17] <- c("Play-out Entropy Project Fitness (HJFT)")
rlabels[18] <- c("Generalization by Trace Floor 10 (TGF10)")
rlabels[19] <- c("Generalization by Trace Floor 5 (TGF5)")
rlabels[20] <- c("Generalization by Trace Floor 1 (TGF1)")
rlabels[21] <- c("Earth Movers Truncated (tEMSC0.8)")

corheatmap(pcaearth,"em3","Earth Movers Eval" , dimlabels=dlabels)
corheatmap(pcaent,"ent3","Entropy Eval",  dimlabels=dlabels)
corheatmap(pcaee,"eval3","Eval", dimlabels=dlabels)
#corheatmaprowlabel(pcaee,"eval","Eval", dimlabels=dlabels, rowlabels=rlabels)




if (scree){
  #scree
  prepfig("screeeigem","pcaearth")
  print(fviz_eig(pcaearth))
  postfig()
  prepfig("screeeigent","pcaent")
  print(fviz_eig(pcaent))
  postfig()
  prepfig("screeeval","eval")
  print(fviz_eig(pcaee) + 
          geom_hline(yintercept = 10))
  postfig()
}

write.csv(rdee, paste(workingPath,"eval_c3.csv",sep=""), row.names=FALSE)
