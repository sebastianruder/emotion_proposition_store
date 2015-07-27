write_to_file <- function(x , out_file, append = TRUE, sep = "\n") {
  out <- capture.output(x)
  cat(out, file = out_file, sep = sep, append = append)
}

setwd("/home/sebastian/git/sentiment_analysis/out/scores")

for (dir in list.dirs(recursive = FALSE)) {
  metric <- basename(dir)
  files <- list.files(dir)
  txt_files <- files[grepl("\\.txt", files)]
  unigram_files <- txt_files[grepl("unigram", txt_files)]
  overlap_files <- files[grepl("\\.overlap", files)]
  
  # put out_file in stats directory
  out_file <- file.path(dir, "stats", paste("nrc", metric, "overlap.stats", sep = "_"))
  unlink(out_file) # clean out_file
  file.create(out_file)
  
  for (unigram_file in unigram_files) {
    unigram_frame <- read.csv(file.path(dir, unigram_file), header = FALSE, sep = "\t")
    names(unigram_frame) <- c("Unigram", "Association score", "NRC emotion overlap",
                            "NRC sentiment overlap")
    unigram.100 <- unigram_frame[1:100,]
    unigram.1000 <- unigram_frame[1:1000,]
    
    # generate latex table using xtable
    # library(xtable) 
    # print(xtable(s), file = "myfile.tex") 
    
    # print the summaries; we're only interested in summaries for the overlap values
    write(paste("\n", unigram_file, sep = ""), out_file, append = TRUE)
    write("Top 100", out_file, append = TRUE)
    write_to_file(summary(unigram.100[3:4]), out_file)
    write("\nTop 1000", out_file, append = TRUE)
    write_to_file(summary(unigram.1000[3:4]), out_file)
    write("\nAll", out_file, append = TRUE)
    write_to_file(summary(unigram_frame[3:4]), out_file)
  }
  
  # put out_file in stats directory
  out_file <- file.path(dir, "stats", paste(metric, "overlap.stats", sep = "_"))
  unlink(out_file)
  file.create(out_file)
  
  for (overlap_file in overlap_files) {
    overlap_frame <- read.csv(file.path(dir, overlap_file), header = TRUE)
    write(paste("\n", overlap_file, sep = ""), out_file, append = TRUE)
    write_to_file(summary(overlap_frame), out_file)
  }
}
