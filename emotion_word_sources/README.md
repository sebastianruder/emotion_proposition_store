# README emotion word sources

In this directory I collect different sources for emotion words.

* [Tsvetkov et al.](http://www.cs.cmu.edu/~ytsvetko/papers/adj-lrec14.pdf)
    - `germanet_feeling.labeled`: the subsequent data manually labeled
    - `germanet_feeling.txt`: words with adjective type FEELING translated from GermaNet by the authors
    - `word_types.feeling`: words with adjective type FEELING extracted from the subsequent data
    - `word_types.predicted`: adjective types predicted by the classifier

* [FrameNet](https://framenet.icsi.berkeley.edu/fndrupal/)
	- `experiencer_obj.labeled`: lexical units belonging to the [Experiencer_obj](https://framenet2.icsi.berkeley.edu/fnReports/data/frame/Experiencer_obj.xml)
	frame labeled with emotion type. Def.: "Some phenomenon (the Stimulus) provokes a particular emotion in an Experiencer."
	Experiencer and stimulus (cause) are core frame elements.
	- `desiring.labeled`: lexical units pertaining to the [Desiring](https://framenet2.icsi.berkeley.edu/fnReports/data/frame/Desiring.xml)
	 frame. All are labeled with anticipation. Def.: "An Experiencer desires that an Event occur. [...]"
	- `emotion_active.labeled`: lexical units of the [Emotion_active](https://framenet2.icsi.berkeley.edu/fnReports/data/frame/Emotion_active.xml)
	frame labeled with emotion type. Def.: "This frame has similarities to Experiencer_obj, but here the verbs are
	more 'active' in meaning. [...]"
	- `emotion_directed.labeled`: lexical units of the [Emotion_directed](https://framenet2.icsi.berkeley.edu/fnReports/data/frame/Emotion_directed.xml)
	frame labeled with emotion type. The adjectives have been mostly converted into their respective verb forms to
	facilitate identification of the stimulus. Def.: "The adjectives and nouns in this frame describe an Experiencer who is
	feeling or experiencing a particular emotional response to a Stimulus or about a Topic. [...]"
	- `emotions_of_mental_activity.labeled`: lexical units belonging to the [Emotions_of_mental_activity](https://framenet2.icsi.berkeley.edu/fnReports/data/frame/Emotions_of_mental_activity.xml)
	frame labeled with the emotion type joy. Def.: "An Experiencer can be described as having an emotion as induced by
	a Stimulus." The frame inherits from [Emotions_by_stimulus](https://framenet2.icsi.berkeley.edu/fnReports/data/frame/Emotions_by_stimulus.xml)
	which inherits from [Emotions](https://framenet2.icsi.berkeley.edu/fnReports/data/frame/Emotions.xml).
