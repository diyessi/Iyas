==============================================================
CQA-QL English corpus for SemEval-2016 Task 3
"Community Question Answering"
Version 2.0: October 4, 2015
==============================================================

This file contains the basic information regarding the CQA-QL English corpus provided for the SemEval-2016 task "Community Question Answering". The current version (2.0, October 4, 2015) corresponds to the release of the English training data set. The test sets will be provided in future versions. All changes and updates on these data sets are reported in Section 1 of this document.

[1] LIST OF VERSIONS

  v2.0 [2015/09/05]: added further training data, in a separate file (-part2):
                     100 more original questions, 1,000 related questions, 10,000 comments; unlike the previous distributions, this time there is judgments of relevance for each comment with respect to the related question (in addition to the relevance with respect to the original question)
  v1.1 [2015/09/05]: added more training data compared to the initial distribution:
                     103 original questions, 613 related questions, 5855 comments
  v1.0 [2015/09/05]: initial distribution of the English TRAINING data:
                     50 original questions, 500 related questions, 5000 comments

[2] CONTENTS OF THE DISTRIBUTION 2.0

We are providing the following files:

* README.txt 
  this file

* SemEval2016-Task3-CQA-QL-train-part1.xml
  traning data set as of version 1.1; 103 original questions, 613 related questions, 5,855 comments; no judgments of comment relevance with respect to the related question

* SemEval2016-Task3-CQA-QL-train-part2.xml
  new training data of 100 more original questions, 1,000 related questions, 10,000 comments; contains judgments of comment relevance with respect to the related question


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

A dataset file is a sequence of examples (original questions):

<root>
  <OrgQuestion> ... </OrgQuestion>
  <OrgQuestion> ... </OrgQuestion>
  ...
  <OrgQuestion> ... </OrgQuestion>
</root>

Each OrgQuestion has an ID, e.g., <OrgQuestion ORGQ_ID="Q1">

The structure of an OrgQuestion is the following:

<OrgQuestion ...>
  <OrgQSubject> text </OrgQSubject>
  <OrgQBody> text </OrgQBody>
  <Thread ...>
    <RelQuestion ...> ... </RelQuestion>
    <RelComment ...> ... </RelComment>
    <RelComment ...> ... </RelComment>
    ...
    <RelComment ...> ... </RelComment>
</OrgQuestion>

The text between the <OrgQSubject> and the </OrgQSubject> tags is the short version of the original question.
The text between tags <OrgQBody> and </OrgQBody> is the long version of the question.
What follows is a Thread, which is a sequence of potentially related questions. A Thread consists of a potentially relevant question RelQuestion, together with 10 comments RelComment for it.


*** Thread ***
A thread had a single attribute as in teh following example:

<Thread THREAD_SEQUENCE="Q1_R4">

- THREAD_SEQUENCE: internal related question identifier. It has the form Qxx_Ryy, where the Qxx is the ID of the original question, and Ryy is the rank of the thread in the list of results returned by a search engine for the original question Qxx.


*** RelQuestion ***

Each RelQuestion tag has a list of attributes, as in the following example:

<RelQuestion RELQ_ID="Q1_R4" RELQ_RANKING_ORDER="4" RELQ_CATEGORY="Advice and Help" RELQ_DATE="2013-05-02 19:43:00" RELQ_USERID="U1" RELQ_USERNAME="ankukuma" RELQ_RELEVANCE2ORGQ="PerfectMatch">

- RELQ_ID: the same as for the thread
- RELQ_RANKING_ORDER: is the rank of the related question thread in the list of results returned by a search engine for the original question
- RELQ_CATEGORY: the question category, according to the Qatar Living taxonomy  
- RELQ_DATE: date of posting
- RELQ_USERID: internal identifier for the user who posted the question; consistent across questions
- RELQ_USERNAME: the name of the user who posted the question; consistent across questions and comments; note that users can change their names over time, and this field shows the latest name the user used
- RELQ_RELEVANCE2ORGQ: relevance of the thread of this RelQuestion with respect to the OrgQuestion. This label could be 
  - PerfectMatch: matches the question (almost) perfectly
  - Relevant: covers some aspects of the question
  - Irrelevant: covers no aspects of the question

The structure of a RelQuestion is the following:

<RelQuestion ...>
  <RelQSubject> text </RelQSubject>
  <RelQBody> text </RelQBody>
</RelQuestion>

The text between the <RelQSubject> and the </RelQSubject> tags is the short version of the related question.
The text between tags <RelQBody> and </RelQBody> is the long version of the related question.


*** RelComment ***

Each RelComment tag has a list of attributes, as in the following example:

<RelComment RELC_ID="Q104_R22_C1" RELC_DATE="2012-01-09 11:39:52" RELC_USERID="U2011" RELC_USERNAME="drsam" RELC_RELEVANCE2ORGQ="Bad" RELC_RELEVANCE2RELQ="Good">

 - RELC_ID: Internal identifier of the comment
 - RELC_USERID: Internal identifier of the user posting the comment
 - RELC_USERNAME: the name of the user who posted the comment; consistent across questions and comments; note that users can change their names over time, and this field shows the latest name the user used
 - RELC_RELEVANCE2ORGQ: human assessment about whether the comment is "Good", "Bad", or "PotentiallyUseful" with respect to the *original* question, OrgQuestion
     - Good: at least one subquestion is directly answered by a portion of the comment
     - PotentiallyUseful: no subquestion is directly answered, but the comment gives potentially useful information about one or more subquestions
     - Bad: no subquestion is answered and no useful information is provided (e.g., the answer is another question, a thanks, dialog with another user, a joke, irony, attack of other users, or is not in English, etc.).
- RELC_RELEVANCE2RELQ: human assessment about whether the comment is "Good", "Bad", or "PotentiallyUseful" with respect to the *related* question, RelQuestion (there is an annotation for this attribute in part2 only; in part1, its value is always "N/A")
 
Comments are structured as follows:

<RelComment ...>
  <RelCText> text </RelCText>
</RelComment>

The text between the <RelCText> and the </RelCText> tags is the text of the comment.


[4] MORE INFORMATION ON THE CQA-QL CORPUS

The source of the CQA-QL corpus is the Qatar Living Forum data (http://www.qatarliving.com). A sample of questions and comments threads was automatically selected and posteriorly manually filtered and annotated with the categories defined in the task.

The manual annotation was a joint effort between the CSAIL-MIT and ALT-QCRI groups (see organizers below). 

After a first internal labeling of a small dataset by several independent annotators, we defined the annotation procedure and we prepared detailed annotation guidelines. 

CrowdFlower was used to collect the human annotations for the large corpus. Hamdy Mubarak (QCRI) implemented the CrowdFlower-based annotation. In all HITs, we collected the annotation of several annotators for each decision (there were at least three human annotators) and resolved discrepancies using the mechanisms of CrowdFlower. Unlike SemEval-2015 Task 3, this time we did not eliminate any comments, and thus there is a guarantee that for each question thread, we have the first 10 comments without any comment being skipped.

Some statistics about the datasets (training & development):

TRAIN-part1:
- ORIGINAL QUESTIONS:
    - TOTAL:               103
- RELATED QUESTIONS:
    - TOTAL:               613
    - PerfectMatch:         90
    - Relevant:            206
    - Irrelevant:          317
- RELATED COMMENTS:
    - TOTAL:             5,855
    - Good:              1,419
    - Bad:               3,436
    - PotentiallyUseful: 1,000

TRAIN-part2:
- ORIGINAL QUESTIONS:
    - TOTAL:                   100
- RELATED QUESTIONS:
    - TOTAL:                 1,000
    - PerfectMatch:            109
    - Relevant:                309
    - Irrelevant:              582
- RELATED COMMENTS:
    - TOTAL:                10,000
    - wrt ORIGINAL QUESTION:
        - Good:              1,214              
        - Bad:               7,987
        - PotentiallyUseful:   799
    - wrt RELATED QUESTION:
        - Good:              3,913
        - Bad:               4,467
        - PotentiallyUseful: 1,620


[5] CREDITS

Task Organizers:

    Lluís Màrquez
        Arabic Language Technologies (ALT)
        Qatar Computing Research Institute (QCRI), Qatar
    James Glass (CSAIL, MIT)
    Walid Magdy (ALT-QCRI, Qatar)
    Alessandro Moschitti (ALT-QCRI, Qatar)    
    Preslav Nakov (ALT-QCRI, Qatar)
    Bilal Randeree (Qatar Living, Qatar)

Task website: http://alt.qcri.org/semeval2016/task3/

Contact: semeval-cqa@googlegroups.com

Acknowledgements: This research is developed by the Arabic Language Technologies (ALT) group at Qatar Computing Research Institute (QCRI), HBKU, within the Qatar Foundation in collaboration with MIT. It is part of the Interactive sYstems for Answer Search (Iyas) project.
