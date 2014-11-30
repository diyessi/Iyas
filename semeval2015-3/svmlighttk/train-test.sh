SVM_LEARN="../../tools/SVM-Light-1.5-rer/svm_learn -t 5 -F 1 -C V -j 5"
SVM_CLASSIFY="../../tools/SVM-Light-1.5-rer/svm_classify"

train_dataset="train"
test_dataset="dev"

a_labels=( "Not_English" "Good" "Potential" "Dialogue" "Bad" )
b_labels=( "Yes" "No" "Unsure" )

for a_label in "${a_labels[@]}"
do
	$SVM_LEARN a/${train_dataset}/${a_label}.svm a/${train_dataset}/${a_label}.model
	$SVM_CLASSIFY a/${test_dataset}/${a_label}.svm a/${train_dataset}/${a_label}.model a/${test_dataset}/${a_label}.pred
done

for b_label in "${b_labels[@]}"
do
	$SVM_LEARN b/${train_dataset}/${b_label}.svm b/${train_dataset}/${b_label}.model
	$SVM_CLASSIFY b/${test_dataset}/${b_label}.svm b/${train_dataset}/${b_label}.model b/${test_dataset}/${b_label}.pred
done

