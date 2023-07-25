library(tsne)
library(plotly)


hpath = "c:/Users/burkeat/bpm/"
# hpath = "c:/Users/Adam/bpm/"

workingPath = paste(hpath,"bpm-dimensions-lab/var/",sep="")
resultsPath = paste(hpath,"bpm-dimensions-lab/results/",sep="")


rdnl = read.csv( paste(workingPath,"expn_c3.csv",sep=""),
                 strip.white=TRUE)



# features <- subset(rundata, select = -c(Artifact.Creator)) 

set.seed(0)
tsne <- tsne(rdnl, initial_dims = 2)
tsne <- data.frame(tsne)
pdb <- cbind(tsne,rundata$Artifact.Creator)
options(warn = -1)
fig <-  plot_ly(data = pdb ,x =  ~X1, y = ~X2, type = 'scatter', mode = 'markers', split = ~iris$Species)

fig <- fig %>%
  layout(
    plot_bgcolor = "#e5ecf6"
  )

fig