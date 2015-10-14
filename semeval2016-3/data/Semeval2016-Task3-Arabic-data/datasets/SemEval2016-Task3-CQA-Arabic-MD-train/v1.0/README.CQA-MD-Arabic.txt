==============================================================
CQA-MD Arabic corpus for SemEval-2016 Task 3
"Community Question Answering" - "Medical Domain"
Version 1.0: September 30, 2015
==============================================================

This file contains the basic information regarding the CQA-MD Arabic corpus provided for the SemEval-2016 task "Community Question Answering". The current version (1.0, September 30, 2015) corresponds to the release of the Arabic full training data set. The test and development sets will be provided in future versions.

[1] LIST OF VERSIONS

  v1.0 [2015/09/30]: distribution of the ARABIC TRAINING data:
                     1,031 original questions, 30,411 potentially related question-answer pairs

[2] CONTENTS OF THE DISTRIBUTION

We are providing the following files:

* README.txt 
  this file

* SemEval2016-Task3-CQA-MD-train.xml
  traning data set; 1,031 original questions, and 30,411 potentially related question-answer pairs


This distribution is directly downloadable from the official SemEval-2016 Task 3 website http://alt.qcri.org/semeval2016/task3/index.php?id=data-and-tools

Licensing: 
- these datasets are free for general research use 
- you should use the following citation in your publications whenever using this resource:

@InProceedings{nakov-EtAl:2015:SemEval,
  author    = {Nakov, Preslav  and  M\`{a}rquez, Llu\'{i}s  and  Magdy, Walid  and  Moschitti, Alessandro  and  Glass, Jim  and  Randeree, Bilal},
  title     = {{SemEval}-2015 Task 3: Answer Selection in Community Question Answering},
  booktitle = {Proceedings of the 9th International Workshop on Semantic Evaluation},
  series    = {SemEval '2015},
  month     = {June},
  year      = {2015},
  address   = {Denver, Colorado},
  publisher = {Association for Computational Linguistics},
  pages     = {269--281},
  url       = {http://www.aclweb.org/anthology/S15-2047}
}



[3] DATA FORMAT

The datasets are XML-formatted and the text encoding is UTF-8.

A dataset file is a sequence of examples (questions):

<root>
  <Question> ... <\Question>
  <Question> ... <\Question>
  ...
  <Question> ... <\Question>
</root>

Each <Question> has an ID, e.g., <Question QID="200634">

The internal structure of a <Question> is the following:

<Question ...>
  <Qtext> .... </Qtext>
  <QApair> .... </QApair>
  <QApair> .... </QApair>
  <QApair> .... </QApair>
</Question>

<Qtext> is the text of the question.
<QApair> is a question-answer pair retrieved using a search engine; the task is to judge the relevance of this pair with respect to the question. 

There are about 30 instances of <QApair> per <Question>.


*** QApair ***

<QApair> contains the following attributes:

- ID (QAID): a unique ID of the question-answer pair.

- Relevance (QArel): relevance of the question-answer pair with respect to the <Question>, which is to be predicted at test time:
	
  - "D" (Direct): The question-answer pair contains a direct answer to the original question such that if the user is searching for an answer to the original question <Question>, the proposed question-answer pair would be satisfactory and there will be no need to search any further.
	
  - "R" (Related): The question-answer pair contains an answer to the <Question> that covers some of the aspects raised in the original question, but this is not sufficient to answer it directly. With this question-answer pair, it would be expected that the user will continue the search to find a direct answer or more information.

	- "I" (Irrelevant): The question-answer pair contains an answer that does not relate to the original question <Question>.
	
- Confidence (QAconf): This is the confidence value for the Relevance annotation, based on inter-annotator agreement and other factors. This value is available for the TRAINING dataset only, and it is not available for the DEV and the TEST datasets.


[4] ABOUT THE CQA-MD CORPUS

We generated the CQA-MD Arabic corpus using data from three Arabic medical websites. First, we extracted 1,531 medical questions from http://www.webteb.com/. We then used a number of indexing and retrieval methods to generate a list of potentially relevant question-answer pairs by querying the content of two other websites: http://www.altibbi.com/ and http://consult.islamweb.net/.

For each original question, we retrieved the 30 top-ranked question-answer pairs from our combined index of the other two websites. We then used crowd-sourcing to get annotations about the relevance of each question-answer pair with respect to the corresponding original question. We controlled for quality using hidden tests. We asked for three judgments per example, and we used a combination of majority voting and annotator confidence to select the final label. The average inter-annotator agreement was 81%.

Finally, we divided the data into training, development and testing datasets, based on confidence, where the examples in the test dataset have the highest annotation confidence.

Here are some statistics about the datasets:

TRAIN:
- ORIGINAL QUESTIONS:
    - TOTAL:               1,031
- RETRIEVED QUESTION-ANSWER PAIRS:
    - TOTAL:              30,411
    - Direct:                917
    - Relevant:           17,412
    - Irrelevant:         12,082

DEV:
- ORIGINAL QUESTIONS:
    - TOTAL:                 250
- RETRIEVED QUESTION-ANSWER PAIRS:
    - TOTAL:               7,387
    - Direct:                 86
    - Relevant:            4,612
    - Irrelevant:          2,689

	
[5] CREDITS

Task Organizers:

    Lluís Màrquez
        Arabic Language Technologies (ALT)
        Qatar Computing Research Institute (QCRI), Qatar
    James Glass (CSAIL, MIT)
    Walid Magdy (ALT-QCRI, Qatar)
	Hamdy Mubarak (ALT-QCRI, Qatar)
    Alessandro Moschitti (ALT-QCRI, Qatar)    
    Preslav Nakov (ALT-QCRI, Qatar)
    Bilal Randeree (Qatar Living, Qatar)

Task website: http://alt.qcri.org/semeval2016/task3/

Contact: semeval-cqa@googlegroups.com

Acknowledgements: This research is developed by the Arabic Language Technologies (ALT) group at Qatar Computing Research Institute (QCRI), HBKU, within the Qatar Foundation in collaboration with MIT.
It is part of the Interactive sYstems for Answer Search (Iyas) project.
