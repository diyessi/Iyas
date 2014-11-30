==============================================================
QA-Islamweb Arabic corpus for SemEval-2015 Task 3
"Answer Selection in Community Question Answering"
Version 2.0: September 25, 2014
==============================================================

This file contains the basic information regarding the QA Arabic corpus provided for the SemEval-2015 task "Answer Selection in Community Question Answering". The current version (2.0, September 25, 2014) corresponds to the release of the training data sets. Test sets will be provided in future versions. All changes and updates on these data sets are reported in Section 1 of this document.

[1] LIST OF VERSIONS

  v2.0 [2014/09/15]: initial distribution of the TRAINING data sets. 
      The following changes are observed from distribution 1.0:
    - Training and development sets have been provided
    - Several cosmetic changes apply to the XML labels 
    - The "ANNOTATION" field was renamed to "GOLD"
    - The set of labels for the "ANNOTATION" field have been slightly simplified
    - The Yes/No type of questions are dropped.
    - Please, disregard the formatting of the TRIAL dataset and stick to the current version 2.0

  v1.0 [2014/06/30]: initial distribution of the TRIAL data sets


[2] CONTENTS OF THE DISTRIBUTION 2.0

We are providing the following files:

* README.txt 
  this file

* datasets/QA-Arabic-train.xml
  training data set; 1,300 questions
  
* datasets/QA-Arabic-dev.xml
  development data set; 200 questions
  
* datasets/QA-Arabic-train-input.xml
  the training data set, but with the gold labels hidden
  
* datasets/QA-Arabic-dev-input.xml
  the development data set, but with the gold labels hidden -- to be used as input at development time

* datasets/QA-Arabic-train.gold
  the gold labels for training dataset (should be used in the CGOLD attribute in the Answer Field)

* datasets/QA-Arabic-dev.gold
  the gold labels for development dataset (should be used in the CGOLD attribute in the Answer Field)


Note: The training and the development sets are obtained by randomly splitting the questions 
into two sets of ~85% and ~15% of the total size.


This distribution is directly downloadable from the official SemEval-2015 Task 3 website http://alt.qcri.org/semeval2015/task3/index.php?id=data-and-tools

Licensing: 
- these datasets are free for general research use 
- you should use the following citation in your publications whenever using this resource:

@InProceedings{Marquez-EtAl:2015:SemEval,
  author    = {Lluis Marquez and James Glass and Walid Magdy and Alessandro Moschitti and Preslav Nakov and Bilal Randeree},
  title     = {SemEval-2015 Task 3: Answer Selection in Community Question Answering},
  booktitle = {Proceedings of the 9th International Workshop on Semantic Evaluation (SemEval 2015)},
  year      = {2015},
  publisher = {Association for Computational Linguistics},
}



[3] DATA FORMAT

The datasets are XML-formated and the text encoding is UTF-8.

A dataset file is a sequence of examples (Questions):

<root>
  <Question> ... <\Question>
  <Question> ... <\Question>
  ...
  <Question> ... <\Question>
</root>

Each Question tag has a list of attributes, as in the following example:

<Question QID = "20831" QCATEGORY = "فقه العبادات > الطهارة" QDATE = "2002-13-08">

- QID: internal question identifier
- QCATEGORY: the question category
- QDATE: date of posting

The structure of a Question is the following:

<Question ...>
  <QSubject> text </QSubject>
  <QBody> text </QBody>
    <Answer> ... </Answer>
    <Answer> ... </Answer>
    ...
    <Answer> ... </Answer>
</Question>

The text between the <QSubject> and the </QSubject> tags is the title of the question that is created by Islamweb.
The text between the <QBody> and teh </QBody> tags is the full question that is provided by the user.
What follows is a list of possible answers, which can be the original direct answer on Islamweb or related answers that are linked to that answer, or randomly selected answers that should have no relation to the topic of the question.

Each Answer tag has some attributes, as in the following example:

<Answer CID = "41673" CGOLD = "?">

 - CID: the answer ID
 - CGOLD: the classification of the answer (Hierarchical classification) 

The text between the <Answer> and the </Answer> tags is the answer text. It can contain tags such as the following:
	- NE: names entities in the text, usually person names
	- Quran: Quran verse
	- Hadeeth: A saying by the Islamic prophet


[4] MORE INFORMATION ON THE QA-Islamweb CORPUS

The Arabic Fatwa corpus consists of a set of questions and answers related to Islam regarding what is allowed and what is not allowed to do in Islam. A question is submitted by a user on a dedicated Islamic website about some issue in life/religion and a scholar answers the question by giving details and references to his/her answer. All answers are by professional people and are very descriptive. The user's question can be general, for example "How to pray?", or it can be very personal, like someone who has a specific problem in his life and wants to find how Islam guides people to deal with it. In another example, a woman explains that her husband takes her salary and gives her only a small part of it, which makes her take some money from her children to be able to live, and she asks whether her husband is allowed to do so or not, and whether taking money from her children is correct or not. The answer of the scholar is usually descriptive, where it contains and introduction to the topic of the question, then the general rules of religion about the topic, and then an answer to the specific question and guidance to how to deal with the problem. Many times links to related questions are provided for the user to read more about similar situations or related questions.

We constructed our test set by selecting some of the questions from the corpus and providing a set of possible answers. The set of provided answers is as follows:
-	"Direct" answer of the question, this will be the perfect answer to the question and will be scored the best
-	"Related" answers to the question, which will be generated from the links in the original answer or by simple search in the collection. These answers will be labeled as "potentially useful answers".
-	"Irrelevant", which is a randomly selected answer for some other questions.


[5] CREDITS

Task Organizers:

    Lluis Marquez
        Arabic Language Technologies (ALT)
        Qatar Computing Research Institute (QCRI), Qatar
    James Glass (CSAIL, MIT)
    Walid Magdy (ALT-QCRI, Qatar)
    Alessandro Moschitti (ALT-QCRI, Qatar)    
    Preslav Nakov (ALT-QCRI, Qatar)
    Bilal Randeree (Qatar Living, Qatar)

Task website: http://alt.qcri.org/semeval2015/task3/

Contact: semeval-cqa@googlegroups.com

Acknowledgements: This research is part of the Interactive sYstems for Answer Search (Iyas) project, conducted by the Arabic Language Technologies (ALT) group at the atar Computing Research Institute (QCRI) within the Qatar Foundation. 
