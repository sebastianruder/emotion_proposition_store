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

1. Compile list of emotion-triggering expression patterns for Plutchik's eight basic emotions.
  1. Derive patterns from
    * introspection
    * semantic classes in VerbNet
    * emotion verb classes from Mathieu and Fellbaum
    * thesauri
    * sentiment lexica (highly associated verbs)
  1. Get an overview of pattern frequencies, precision and recall (e.g. with CQP) and select the most frequent, least ambiguous ones.
  1. Match emotion-triggering expression patterns on subset of corpus locally using GW API; start with smaller corpus portions, later on larger portion on single server.
  1. Cross-check emotion class belongingness with a second annotator.
    * Try after selecting the initial expressions on the basis of identifying the expression with full examples first (e.g. by leaving out the expression and having someone insert one of the possible class labels),
    * and towards the end with the final choice of expressions and with further steps completed (characterizing of experiencer and proposition).
  1. Provide
    * overview of process and applied criteria,
    * sketch candidates,
    * list of chosen expression w/ statistics.

2. Extract core propositions, i.e. NPs when applicable; else focusing primarily on dependencies (verb arguments). Potentially refer to software project group for extracting verb arguments. Extracting experiencer and propositions will require a stepwise approach:
  1. Start with dependency-like structure, i.e. main predicate plus dependents; keep index to surface form.
  2. Capture argument structure of proposition (head and dependencies).
  3. Filter to obtain "core" propositions: avoid further proposition embedding structures; light verbs; quantificational nouns, etc. -> collect examples and devise filter conditions.
  4. Foresee alternative representation choices: optionally include (simple) modifiers.

3. Deploy program on ella remote server cluster using Hadoop/MapReduce with a stable extraction procedure on smaller samples; experiment with larger corpus portions.

4. Store emotion-triggering expressions in proposition store as triples (similar to TextRunner, KnowItAll). Experiment with generalization steps:
  1. Substitute pronouns with nominal antecedent (or eliminate/filter if too unreliable).
  2. Substitute NEs with NE class if available (optional, depending on annotations).
  3. Common nouns: Apply most frequent sense from WordNet and thereby access its governing supersense hypernym (optional).
  4. Determine the final representation format.

5. Devise frequency analyses and association statistics (e.g. PMI) to select emotion indicating expressions (single vs. compound expressions).

6. Evaluate against existing emotion-labeled resources, e.g. Borth et al. (2013), Mohammad and Turney (2011). Work out possible evaluation strategies. Investigate the available lexicons for evaluation, check to what extent this needs to be taken into account in step 1 and plan on how to do the following evaluations:
   1. Evaluation re. coverage (distinct and overlapping expressions)
   2. Evaluation re. pos/neg emotion (with mapping of classes to pos/neg) for overlapping expressions
   3. Evaluation of our acquision approach with regard to existing lexicons; answering the question: How many of the existing lexicon's expressions can we obtain on a given corpus size? This requires choosing a representative nb of test expressions to compare.

7. Evaluate against event-aligned GW corpus by Michael Roth to identify events with diverging sentiments.
Here one needs to identify overlapping emotion words/expressions from the set of acquired propositions and the aligned
events. Cannot be easily estimated at early stages. Possibly overly complex. Other option: Classification experiments; 
binary (pos/neg), all 8 classes. Requires test data set. Possible with Amazon Mechanical Turk.

1. (optional:) Derive new emotion-triggering patterns from homogeneous expressions using bootstrapping.
1. (optional:) Apply approach to other domains
1. (optional, MA only:) Link acquired expressions to an emotion-annotated ontology
1. (optional, MA only:) Generalize to near-synonyms of emotion-bearing expressions in the distributional space
