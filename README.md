#Construction and Analysis of an Emotion Proposition Store

This is the respository for my Bachelor's thesis dealing with the construction and analysis of an Emotion Proposition Store. This project includes the following contributions:
- Designing and evaluating patterns that are frequent and clearly associated with an emotion. These patterns can be used as-is to extract tuples of emotion holders and causes from the web as well as from special domain corpora.
- Acquiring more than 1,700,000 propositions from the Annotated Gigaword news corpus using these patterns, filtering, and generalizing them by employing co-reference resolution and named-entity recognition (NER). These propositions contain information about the emotion, the emotion holder, and the cause of said emotion.
- Storing these propositions in an emotion proposition store, which we make available to the research community.
- Analysing and evaluating them to gain further understanding about emotions in news text as well as the capabilities of the resource.Distributional analysis allows us to determine ambiguous concepts as well as single-word and compound expressions that are highly associated with an emotion.

##Structure of this repository

This repository is organized as follows:

- [NRC-Emotion-Lexicon-v0.92](NRC-Emotion-Lexicon-v0.92/): The [NRC Word-Emotion Association Lexicon](http://saifmohammad.com/WebPages/NRC-Emotion-Lexicon.htm) by Saif Mohammad.
- [R](R/): R code to generate summaries for agreement with the NRC Emotion Lexicon.
- [anno_gigaword](anno_gigaword/): The forked [Annotated Gigaword Java API](https://github.com/mgormley/agiga) by Courtney Napoles, Matthew Gormley, and Benjamin Van Durme.
- [annotation](annotation/): The annotated files of the two annotation tasks. [patterns_annotated](annotation/patterns_annotated/) contains the pattern annotation, while [bigrams_annotated](annotation/bigrams_annotated/) contains the annotation of the bigrams.
- [dependencies](dependencies/): The [JCommon](dependencies/jcommon-1.0.23/) and [JFreeChart](dependencies/jfreechart-1.0.19) libraries used for generating charts.
- [mallet](mallet/): The pseudo-documents and topic models generated using [MALLET](http://mallet.cs.umass.edu/topics.php).
- [emotion_word_sources](emotion_word_sources/): Related work that was used as a source for the patterns.
- [out](out/): The directory of the results. 
  - [Emotion proposition store](out/emotion_proposition_store/): The extracted propositions in _shelves_ of 100,000 lines. They have the following format: `ID \t emotion \t pattern \t emotion holder \t NP cause \t S cause subject \t S cause predicate \t S cause object \t S cause prepositional objects \t cause bag-of-words`.
  - [Patterns](out/patterns): The pattern templates and the regular expressions.
  - [Scores](out/scores/): The lists of unigrams and bigrams ranked by point-wise mutual information (PMI) or chi-square for Plutchik's eight emotions, sorted by source (emotion holder, NP cause, S cause subject + predicate, S cause predicate + object). These can be used as an emotion lexicon.
  - [Sentences](out/sentences/): The extracted propositions along with the sentences that they were extracted from in chunks of 100,000 lines.
  - [Stats](out/stats/): Statistics about the patterns and the extracted propositions.
- [src](src/): The source directory.
  - [AgigaReader](src/AgigagReader.java): Class to extract propositions from the Annotated Gigaword corpus.
  - [Analyzer](src/Analyzer.java): Class to analyze extractions.
  - [AnnotationComparer](src/AnnotationComparer.java): Class to compare pattern and bigram annotations.
  - [AnnotationTaskGenerator](src/AnnotationTaskGenerator.java): Class to create the bigram annotation task.
  - [EmotionPatternExtractor](src/EmotionPatternExtractor.java): Class to convert pattern templates into regular expressions.
  - [Enums](src/Enums.java): Class containing various enumerations.
  - [Extensions](src/Extensions.java): Class containing various extension methods.
  - [Extraction](src/Extraction.java): Class storing information about an extraction.
  - [GitFileSplitter](src/GitFileSplitter.java): Class to split files for upload via GitHub.
  - [MALLETProcessor](src/MALLETProcessor.java): Class to process MALLET topic distributions.
  - [RandomWriter](src/RandomWriter.java): Class to create the pattern annotation task.
  - [ResultsCleaner](src/ResultsCleaner.java): Class to remove duplicates and erroneous patterns from results.
  - [ResultsReader](src/ResultsReader.java): Class to read extractions and write score files.
  - [ResultsStatsWriter](src/ResultsStatsWriter.java): Class to write statistics about extracted propositions.
  - [Stats](src/Stats.java): Class to store and write emotion and pattern statistics.
  - [Utils](src/Utils.java): Utility class containing IO, token- and tree-processing methods.
  - [Visualizer](src/Visualizer.java): Class to generate charts from association scores.
  - [WordTypesExtractor](src/WordTypesExtractor.java): Class to extract word types from [Tsvetkov et al.](emotion_word_sources/tsvetkov_et_al./)
- [thesis](thesis/): The directory for the thesis LaTeX project.

