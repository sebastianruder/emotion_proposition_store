# Working plan Bachelor's thesis on sentiment analysis

## Configuration
Machine: Ubuntu 14.04
Programming language: Java (IntelliJ IDE)

## Synopsis

* Aim: implement a keyword-based approach for acquiring a proposition store of emotion-tagged state-of-affairs, focusing on linguistic constructs that clearly identify the emotion holder (subject) and the target. Acquired propositions will be filtered, parsed and stored in a proposition store or distributional space, parameterized according to the basic emotion classes. Analysis of the proposition store or semantic space of the acquired propositions will yield the contributing single vs. compound emotion-triggering expressions.
* Evaluation: against existing emotion-labeled resources, e.g. Borth et al. 13, Mohammad and Turney 2011.
* Corpus: e.g. GigaWord Corpus (partly tagged for domains)
* Optional (MA only): linking acquired expressions to an emotion-annotated ontology
* Optional (MA only): generalization to of near-synonyms of emotion-bearing expressions in the distributional space

## Steps

1. Compile list of emotion-triggering expression patterns for Plutchik's eight basic emotions. Derive patterns from
  * introspection
  * semantic classes in VerbNet
  * emotion verb classes from Mathieu and Fellbaum
  * thesauri
  * sentiment lexica (highly associated verbs)
1. Get an overview of pattern frequencies, precision and recall (e.g. with CQP) and possibly select the most frequent, least ambiguous ones.
1. Optionally cross-check emotion class belongingness with a second annotator.
1. Match emotion-triggering expression patterns on subset of corpus locally using GW API.
1. Extract core propositions, i.e. NPs when applicable; else focusing primarily on dependencies (verb arguments). Potentially refer to software project group for extracting verb arguments.
1. Deploy program on ella remote server cluster using Hadoop/MapReduce.
1. Store emotion-triggering expressions in proposition store as triples (similar to TextRunner, KnowItAll). 
1. Analyze single and compound emotion-triggering expressions concerning homogeneity/heterogeneity, frequency, and association (using PMI).
1. Evaluate against existing emotion-labeled resources, e.g. Borth et al. (2013), Mohammad and Turney (2011).
1. Evaluate against event-aligned GW corpus by Michael Roth to identify events with diverging sentiments.
1. (optional:) Derive new emotion-triggering patterns from homogeneous expressions using bootstrapping.
1. (optional:) Apply approach to other domains
1. (optional, MA only:) Link acquired expressions to an emotion-annotated ontology
1. (optional, MA only:) Generalize to near-synonyms of emotion-bearing expressions in the distributional space
