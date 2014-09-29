#Sentiment analysis
##Repository for my bachelor thesis dealing with sentiment analysis

###Corpus
- Annotated Gigaword V.5 is used as made available for me by the [Institute for Computational Linguistics of the Ruprecht-Karls-Universität Heidelberg](http://www.cl.uni-heidelberg.de/)
- [The Annotated Gigaword Java API](https://github.com/mgormley/agiga) by Courtney Napoles, Matthew Gormley, Benjamin Van Durme is used

#### API use
The Annotated Gigaword API V.1.5 was forked and copied to anno_gigaword/agiga.
When the sentiment_analysis repo is cloned, the anno_gigaword/agiga submodule can be populated using ```git submodule update --init```.
```mvn compile assembly:single``` is subsequently used to generate the necessary .jar file.

In anno_gigaword the following command can be used to display the part-of-speech tags of afp_eng_199405.xml.gz:
```
java -cp agiga/target/agiga-1.5-SNAPSHOT-jar-with-dependencies.jar:lib/* edu.jhu.agiga.AgigaPrinter pos afp_eng_199405.xml.gz
```

Employ StreamingSentenceReader to read in files.
