#!/bin/bash

trainQuestionsNum=5

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# load the classpath stored in the classpaht.txt file.
# The classpath can be generated by using the (util.ClassPathPrint) class.

source $DIR/../classpath.txt
echo "DIR: $DIR"
#echo "$CLASSPATH"

function clean {
    echo -n "cleaning $1... "
    echo "done"
}

function write {
    echo -n "writing $1... "
    echo "done"
}
    
function train {
    echo "trainQuestionClassifierPath: $trainTrainQuestionClassifierPath"
    echo "trainCasesDir: $trainTrainCasesDir"
    echo "trainOutputDir: $trainTrainOutputDir"
    echo "lang: $trainLang"
    echo -n "training... "
    java qa.qcri.qf.tools.questionclassifier.QuestionClassifierTrain -trainQuestionClassifierPath "$trainTrainQuestionClassifierPath" -trainCasesDir "$trainTrainCasesDir" -trainOutputDir "$trainTrainOutputDir" -lang "$trainLang"
    echo "done"
}

function test {
    echo "testQuestionClassifierPath: $testTestQuestionClassifierPath"
	echo "testCasesDir: $testTestCasesDir"
	echo "testModelsDir: $testTestModelsDir";
	echo "lang: $testLang"
    echo -n "testing... "
    java qa.qcri.qf.tools.questionclassifier.QuestionClassifierTest -testQuestionClassifierPath "$testTestQuestionClassifierPath" -testCasesDir "$testTestCasesDir" -testModelsDir "$testTestModelsDir" -lang "$testLang"
    echo "done" 
}

function eval {
    echo -n "eval... "
    echo "done"
}

# clean files in the specified dir (e.g. remove CASes, etc...)
function cv.clean {
    if [ -z  $1 ]; then
        echo "usage: bash question_classifier.sh -load /path/to/config_file -cv.clean [train|test|*]"
        exit 1
    elif [ $1 == "train" ]; then
        if [ -d $cvtrainCasesDir ]; then
            removeFiles $cvtrainCasesDir
        fi
    elif [ $1 == "test" ]; then
        if [ -d $cvtestCasesDir ]; then
            removeFiles $cvtestCasesDir
        fi
    elif [ $1 == "*" ]; then
        removeFiles $cvtrainCasesDir
        removeFiles $cvtestCasesDir
    else 
        echo "usage: bash question_classifier.sh -load /path/to/config_file -cv.clean [train|test|*]"
        exit 1
    fi
}

function clean {
    if [ -z $1 ]; then
        echo "usage: bash question_classifier.sh -load /path/to/config_file cv.clean [train|test|*]"
        exit 1
    elif [ $1 == "train" ]; then
        if [ -d "$trainTrainCasesDir" ]; then
            removeFiles "$trainTrainCasesDir"
        fi
    elif [ $1 == "test" ]; then
        if [ -d "$testTestCasesDir" ]; then
            removeFiles "$testTestCasesDir"
        fi
    elif [ $1 == "*"]; then
        removeFiles "$trainTrainCasesDir"
        removeFiles "$testTestCasesDir"
    else
        echo "usage: bash question_classifier.sh -load /path/to/config_file -clean [train|test|*]"
        exit 1
    fi    
}

function removeFiles {
    if [ -z "$1" ]; then
        echo "dir not specified"
        exit 1
    fi
    if [ ! -d "$1" ]; then
        echo "$1: not a valid dir"
        exit 1
    fi
    for f in "$1"/*; do
        echo "removing file: $f... "
        rm $f
    done
}



# load settings from a config file
function load {     
    if [ -z $1 ]; then
        echo "config file not specified"
        exit 1    
    elif [ ! -f $1 ]; then
        echo "file $1 not found"
        exit 1
    fi
    echo -n "loading settings from: $1... "
    source $1
    echo "done"
}

function cv.train {
    echo "trainQuestionClassifierCVPath: $cvtrainTrainQuestionClassifierCVPath"
    echo "lang: $cvtrainLang"
    echo "CasesDir: $cvtrainCasesDir"
    echo "trainOutputDirCV: $cvtrainTrainOutputDirCV"
    echo -n "cv.training... "
    java -cp ${CLASSPATH}:$DIR/../target/classes qa.qcri.qf.tools.questionclassifier.QuestionClassifierTrainCV -trainQuestionClassifierCVPath "$cvtrainTrainQuestionClassifierCVPath" -lang "$cvtrainLang" -CasesDir "$cvtrainCasesDir" -trainOutputDirCV "$cvtrainTrainOutputDirCV"    
    echo "done"
}

function learn {
    echo "src: $learnSrc"
    echo "dest: $learnDest"
    echo "params: $learnParams"
    echo -n "learning... "
    echo "done"
    bash scripts/svm_train.sh "$learnSrc" "$learnDest" "$learnParams"
}

function cv.learn {
    echo "src: $cvlearnSrc"
    echo "dest: $cvlearnDest"
    echo "params: $cvlearnParams"
    echo -n "cv.learning... "
    bash scripts/svm_train_cv.sh "$cvlearnSrc" "$cvlearnDest" "$cvlearnParams"
    echo "done"
}

function cv.test {
    echo "testQuestionClassifierCVPath: $cvtestTestQuestionClassifierCVPath"
    echo "casesDir: $cvtestCasesDir"
    echo "testModelsDirCV: $cvtestTestModelsDirCV"
    echo "lang: $cvtestLang"
    
    echo -n "cv.testing... "
    java qa.qcri.qf.tools.questionclassifier.QuestionClassifierTestCV -testQuestionClassifierCVPath "$cvtestTestQuestionClassifierCVPath" -casesDir "$cvtestCasesDir" -testModelsDirCV "$cvtestTestModelsDirCV" -lang "$cvtestLang"
    echo "done"
}

function cv.split {
    echo "inputFile: $cvsplitInputFile"
    echo "outputFile: $cvsplitOutputFile"
    echo "k: $cvsplitK"
    echo "shuffle: $cvsplitShuffle"
    echo "seed: $cvsplitSeed"
    echo "writeFile: $cvsplitWriteFile"
    echo -n "cv.splitting... "
    java -cp ${CLASSPATH}:$DIR/../target/classes qa.qcri.qf.crossvalidation.FileKFold --inputFile "$cvsplitInputFile" --outputFile "$cvsplitOutputFile" -k "$cvsplitK" --shuffle "$cvsplitShuffle" --seed "$cvsplitSeed" --writeFile "$cvsplitWriteFile"

    echo "done"

}

usage="usage: bash question_classifier.sh [-load config_file] [-train] [-learn] [-test] [-clean [train|test|*]] [-cv.split] [-cv.train] [-cv.learn] [-cv.test] [-cv.clean [train|test|*]]

where:
    -load      load the configuration file
    -train     write the train file
    -learn     train the question classifier
    -test      test the question classifier
    -clean     remove the serialized CASes
    -cv.split  split dataset into k consecutive folds
    -cv.train  write k different train files
    -cv.learn  train k different question classifiers
    -cv.test   test  k different question classifiers
    -cv.clean  remove the serialized CASes
"

while [ -n "$(echo $1 | grep '-')" ]; do 
    case $1 in
        -set ) echo '-process option set'
                if [ ! -z "$2" -a "$(echo $2 | grep -E '^[[:alpha:]]+=[[:alnum:]]+$')" ]; then
                    declare "$2"                  
                fi
                shift ;;
        -write ) echo '-process option -write'
                 if [ ! -z "$2" ]; then
                    if [ "$2" = "*" ]; then
                        write "train"
                        write "test"
                    else
                        write "$2"
                    fi
                 fi 
                 shift ;;
        -train ) echo '-process option -train' 
                 train ;;                 
        -test  ) echo '-process option -test'
                 test ;;                 
        -learn ) echo '-process option -learn'
                learn;;
        -clean ) echo '-process option -clean'
                clean "$2"
                shift;;
        -eval  ) echo '-process option -eval'
                 eval ;;
        -load ) echo '-process option -load'
                 load "$2"
                 shift;;
        -cv.train ) echo "-process option cv.train"
                 cv.train;;
        -cv.test ) echo "-process option cv.test"
                 cv.test;;
        -cv.split ) echo "-process option cv.split"
                 cv.split;;    
        -cv.learn ) echo "-process option cv.learn"             
                 cv.learn;;
        -cv.clean ) echo "-process option cv.clean"         
                 cv.clean "$2"
                 shift;;    
        -help ) echo "-process option help"
                echo "$usage";;
        * ) echo 'usage: bash question_classifier.sh [-load config_file] [-train] [-learn] [-test] [-clean] [-cv.split] [-cv.train] [cv.learn] [-cv.test] [-cv.clean]'   
            exit 1
    esac
    shift
done

