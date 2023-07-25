library(ica)

resetplots <- function(){
  dev.off()
  par("mar") 
  par(mar=c(1,1,1,1))
}


# hpath = "c:/Users/burkeat/bpm/"
hpath = "c:/Users/Adam/bpm/"

workingPath = paste(hpath,"bpm-dimensions-lab/var/",sep="")
resultsPath = paste(hpath,"bpm-dimensions-lab/results/",sep="")


#rdnl = read.csv( paste(workingPath,"expn_c3.csv",sep=""),
#                 strip.white=TRUE)
rdnl = read.csv( paste(workingPath,"expn_nm_c3.csv",sep=""),
                 strip.white=TRUE)


# ica3 = ica(rdnl,3)
ica3 <- icafast(rdnl,3)
ica4 <- icafast(rdnl,4)

# par(mfcol = c(1, 2))
plot(1:nrow(rdnl), ica3$S[,1], type = "l", xlab = "S'1", ylab = "")
plot(1:nrow(rdnl), ica3$S[,2], type = "l", xlab = "S'2", ylab = "")
plot(1:nrow(rdnl), ica3$S[,3], type = "l", xlab = "S'3", ylab = "")

pairs(ica3$S, col=rainbow(3) )
plot(ica3$S[,1], ica3$S[,1], col=rainbow(3)[rdnl[,1]], xlab="Comp 1", ylab="Comp 1")

pairs(ica4$S, col=rainbow(4))
