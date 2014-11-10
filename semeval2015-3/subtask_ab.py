import numpy as np
import pandas as pd
import sys

from sklearn.multiclass import OneVsRestClassifier
from sklearn import svm
from sklearn import linear_model as lm
from sklearn import ensemble
from sklearn import tree
from sklearn import preprocessing
from sklearn.utils import assert_all_finite

SCALE = False

def main():
	
	if len(sys.argv) != 3:
		print "Usage:", sys.argv[0], "<train file> <devel file>"
		print "You will find the script output in the current directory."
		sys.exit()
	else:
		subtask_a(sys.argv[1], sys.argv[2])	
		subtask_b(sys.argv[1], sys.argv[2])	

def subtask_a(train, test):
	train_ids, train_gold, train_data = read_data_subtask_a(train)
	test_ids, test_gold, test_data = read_data_subtask_a(test)
		
	label_to_id, id_to_label = get_label_indexes(train_gold, test_gold)
	
	train_labels = [label_to_id[label] for label in train_gold]
	test_labels = [label_to_id[label] for label in test_gold]
	
	lb = preprocessing.LabelBinarizer()
	lb.fit(train_labels)
	
	train_labels = lb.transform(train_labels)
	test_labels = lb.transform(test_labels)
	
	if SCALE:
		train_data = preprocessing.scale(train_data)
		test_data = preprocessing.scale(test_data)
	
	clf = OneVsRestClassifier(svm.LinearSVC(), n_jobs=2)
	
	clf.fit(train_data, train_labels)
	
	predictions = clf.predict(test_data)
	
	with open("subtask_a.pred", "w") as out:
		for test_id, prediction in zip(test_ids, lb.inverse_transform(predictions)):
			out.write(str(test_id) + "\t" + str(id_to_label[prediction]) + "\n")
			
def subtask_b(train, test):
	train_ids, train_gold, train_data = read_data_subtask_b(train)
	test_ids, test_gold, test_data = read_data_subtask_b(test)
		
	label_to_id, id_to_label = get_label_indexes(train_gold, test_gold)
	
	train_labels = [label_to_id[label] for label in train_gold]
	test_labels = [label_to_id[label] for label in test_gold]
	
	lb = preprocessing.LabelBinarizer()
	lb.fit(train_labels)
	
	train_labels = lb.transform(train_labels)
	test_labels = lb.transform(test_labels)
	
	if SCALE:
		train_data = preprocessing.scale(train_data)
		test_data = preprocessing.scale(test_data)
	
	clf = OneVsRestClassifier(svm.LinearSVC(), n_jobs=2)
	
	clf.fit(train_data, train_labels)
	
	predictions = clf.predict(test_data)
	
	question_id_list = []
	question_counter = {}
	
	for test_id in test_ids:
		question_id = test_id.split("_")[0]
		
		if question_id not in question_counter:
			question_id_list.append(question_id)
			question_counter[question_id] = {"Yes": 0, "No": 0, "Unsure": 0}
	
	for test_id, prediction in zip(test_ids, lb.inverse_transform(predictions)):
		question_id = test_id.split("_")[0]
		question_counter[question_id][id_to_label[prediction]] += 1
	
	with open("subtask_b.pred", "w") as out:
		for question_id in question_id_list:
			
			output_label = "Unsure"
			
			counter = question_counter[question_id]
			if counter["Yes"] > counter["No"] and counter["Yes"] > counter["Unsure"]:
				output_label = "Yes"
			elif counter["No"] > counter["Yes"] and counter["No"] > counter["Unsure"]:
				output_label = "No"
			
			out.write(question_id + "\t" + output_label + "\n")

def get_label_indexes(train_labels, test_labels):
	labels = set(train_labels) | set(test_labels)
	labels = list(labels)
	
	label_to_id = {label : label_id for label, label_id in zip(labels, range(0, len(labels)))}
	id_to_label = {label_to_id[label] : label for label in label_to_id}
	
	return label_to_id, id_to_label

def read_data_subtask_a(data_file):
	df = pd.read_csv(data_file)
	ids = df["qid"].values
	cgold = df["cgold"].values
	cgold_yn = df["cgold_yn"].values
	data = df[range(3, 16)]
	data = data.astype("float64").replace([np.inf, -np.inf, np.nan], 0.0)
	
	return ids, cgold, data
	
def read_data_subtask_b(data_file):
	df = pd.read_csv(data_file)
	
	df = df[df["cgold_yn"] != "Not Applicable"]
	
	ids = df["qid"].values
	cgold = df["cgold"].values
	cgold_yn = df["cgold_yn"].values
	data = df[range(3, 16)]
	data = data.astype("float64").replace([np.inf, -np.inf, np.nan], 0.0)
	
	return ids, cgold_yn, data

if __name__ == "__main__":
	main()
	
# Tried classifiers
#clf = OneVsRestClassifier(svm.SVC())
#clf = OneVsRestClassifier(lm.LogisticRegression())
#clf = OneVsRestClassifier(svm.LinearSVC(C=3), n_jobs=2)
#clf = OneVsRestClassifier(svm.SVC(kernel='linear'), n_jobs=2)
#clf = OneVsRestClassifier(svm.LinearSVC(C=1), n_jobs=2)
#clf = OneVsRestClassifier(lm.SGDClassifier())
#clf = OneVsRestClassifier(ensemble.AdaBoostClassifier(), n_jobs=2)
#clf = svm.LinearSVC(C=1, multi_class="crammer_singer", dual=False)


