import pandas as pd
import numpy as np
import scipy as sp
import re

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.multiclass import OneVsRestClassifier
from sklearn import svm
from sklearn import linear_model as lm
from sklearn import ensemble
from sklearn.metrics import f1_score
from sklearn.externals import joblib
from sklearn.preprocessing import MinMaxScaler, StandardScaler
from sklearn import cluster
from sklearn import lda

#ENGLISH_WORDS = set([line.strip() for line in open("english-words.95", "r")])

IS_A_WORD = re.compile("[a-zA-Z']+")

def compute_jaccard_index(set_1, set_2):
	n = len(set_1.intersection(set_2))
	return n / float(len(set_1) + len(set_2) - n)
    
def compute_sim_features(df_col_1, df_col_2):
	features = []
	for a, b in zip(df_col_1.values, df_col_2.values):
		tok_a = a.split(" ")
		tok_b = b.split(" ")
		sim = compute_jaccard_index(set(tok_a), set(tok_b))
		features.append([sim])
	return features
	
def add_features_from_csv(target_csv, current_data, start_index):
	f_df = pd.read_csv(target_csv)
	last_f_name = f_df.columns.values.tolist()[-1]
	last_f_index = int(last_f_name[1:]) + 1
	for f_index in range(start_index, last_f_index):
		f_name = "f" + str(f_index)
		f_values = [[value] for value in f_df[f_name].values.tolist()]
		current_data = sp.sparse.hstack((current_data, f_values))
	return current_data

def tokenize(text):
    return [tok.strip() for tok in text.split(" ") if len(tok.strip()) > 1]
    
def main():
	
	scaler = MinMaxScaler()

	RANDOM_STATE = 123
	
	print "Reading data..."

	df_train = pd.read_csv("CQA-QL-train.xml.full.csv")
	df_dev = pd.read_csv("CQA-QL-devel.xml.full.csv")
	
	for col_name in ["QBODY", "QSUBJECT", "CBODY"]:
		df_train[col_name] = df_train[col_name].astype('str')
		df_dev[col_name] = df_dev[col_name].astype('str')

	train_qid = df_train["QID"].values
	train_q_body = df_train["QBODY"]
	train_q_subject = df_train["QSUBJECT"]
	train_c_body = df_train["CBODY"]
	train_gold = df_train["CGOLD"].values
	
	#train_c_body = train_c_body.map(spell_check)

	dev_qid = df_dev["QID"].values
	dev_quser_id = df_dev["QUSERID"].values
	dev_cuser_id = df_dev["CUSERID"].values
	dev_cid = df_dev["CID"].values
	dev_q_body = df_dev["QBODY"]
	dev_q_subject = df_dev["QSUBJECT"]
	dev_c_body = df_dev["CBODY"]
	dev_gold = df_dev["CGOLD"].values
	
	text_train = train_q_body.append(train_q_subject)
	text_train = text_train.append(train_c_body)
	
	text_dev = dev_q_body.append(dev_q_subject)
	text_dev = text_dev.append(dev_c_body)

	clf = OneVsRestClassifier(lm.LogisticRegression(C=1, random_state=RANDOM_STATE), n_jobs=-1)
	clf = OneVsRestClassifier(svm.LinearSVC(C=1, random_state=RANDOM_STATE, class_weight="auto"), n_jobs=-1)
	#clf = OneVsRestClassifier(svm.SVC(C=1, probability=True), n_jobs=-1)
	
	stoplist = [word.decode("utf-8").strip() for word in open("../../../../resources/stoplist-en.txt", "r")]
	
	print "Vectorizing..."
	
	vectorizer = TfidfVectorizer(min_df=4, ngram_range=(1, 3), \
		stop_words=set(stoplist), tokenizer=tokenize)
		
	vectorizer_small = TfidfVectorizer(min_df=4, ngram_range=(1, 3), \
		stop_words=set(stoplist), tokenizer=tokenize, max_features=2000)
		
	#vectorizer.fit(text_train)

	vectorizer.fit(train_q_body)
	vec_train_q_body = vectorizer.transform(train_q_body)
	vec_dev_q_body = vectorizer.transform(dev_q_body)

	vectorizer.fit(train_c_body)
	vec_train_c_body = vectorizer.transform(train_c_body)
	vec_dev_c_body = vectorizer.transform(dev_c_body)
	
	vectorizer.fit(train_q_subject)
	vec_train_q_subj = vectorizer.transform(train_q_subject)
	vec_dev_q_subj = vectorizer.transform(dev_q_subject)
	
	print "Computing features..."

	train_data = sp.sparse.hstack((vec_train_q_body, vec_train_c_body, vec_train_q_subj))	
	dev_data = sp.sparse.hstack((vec_dev_q_body, vec_dev_c_body, vec_dev_q_subj))
	
	j1_train = compute_sim_features(df_train["QBODY"], df_train["CBODY"])
	j1_dev = compute_sim_features(df_dev["QBODY"], df_dev["CBODY"])
	
	j2_train = compute_sim_features(df_train["QSUBJECT"], df_train["CBODY"])
	j2_dev = compute_sim_features(df_dev["QSUBJECT"], df_dev["CBODY"])
	
	train_data = sp.sparse.hstack((train_data, j1_train, j2_train))
	dev_data = sp.sparse.hstack((dev_data, j1_dev, j2_dev))
	
	for word in ["yes", "sure", "no", "can", "neither", "okay", "sorry", "qr"]:		
		train_data = sp.sparse.hstack((train_data, contain_word_split(train_c_body, word)))
		dev_data = sp.sparse.hstack((dev_data, contain_word_split(dev_c_body, word)))
		
	for word in ["?", "@"]:
		train_data = sp.sparse.hstack((train_data, contain_word(train_c_body, word)))
		dev_data = sp.sparse.hstack((dev_data, contain_word(dev_c_body, word)))
		
	train_data = sp.sparse.hstack((train_data, short_comment(train_c_body)))
	dev_data = sp.sparse.hstack((dev_data, short_comment(dev_c_body)))
	
	#train_data = sp.sparse.hstack((train_data, num_of_english_words(train_c_body)))
	#dev_data = sp.sparse.hstack((dev_data, num_of_english_words(dev_c_body)))
	
	train_data = add_features_from_csv("CQA-QL-train.xml.csv", train_data, start_index=1)
	dev_data = add_features_from_csv("CQA-QL-devel.xml.csv", dev_data, start_index=1)
	
	'''
	train_data = add_features_from_csv("iman.train.csv", train_data, start_index=22)
	dev_data = add_features_from_csv("iman.dev.csv", dev_data, start_index=22)
	'''
	
	train_data, dev_data = compute_clustering(train_data, dev_data, n_clusters=5)
	
	#train_data = add_wei_features("wei.train.csv", train_data)
	#dev_data = add_wei_features("wei.devel.csv", dev_data)

	print "Training..."

	clf.fit(train_data, train_gold)
	
	print "Predicting..."
	
	predictions = clf.predict(dev_data)
	#scores = clf.decision_function(dev_data)
	
	predictions = ["Dialogue" if q_u_id == c_u_id else prediction \
		for prediction, q_u_id, c_u_id in zip(predictions, dev_quser_id, dev_cuser_id)]
		
	predictions = transform_to_coarse(predictions)
	dev_gold = transform_to_coarse(dev_gold)

	with open("subtask_a.pred", "w") as out:
		for dev_id, prediction in zip(dev_cid, predictions):
			out.write(str(dev_id) + "\t" + str(prediction) + "\n")

	f1 = f1_score(dev_gold, predictions, average="macro")
	print "F1-Macro:", f1
	
def compute_clustering(train_data, dev_data, n_clusters):
	# KMeans	
	print "Computing clusters..."
	
	kmeans = cluster.KMeans(n_clusters=n_clusters, init="k-means++")
	train_clusters = kmeans.fit_transform(train_data)
	dev_clusters = kmeans.transform(dev_data)
	
	train_data = sp.sparse.hstack((train_data, train_clusters))
	dev_data = sp.sparse.hstack((dev_data, dev_clusters))
	
	return train_data, dev_data
	
def transform_to_coarse(labels):
	coarse_labels = ["Bad" if label == "Dialogue" else label for label in labels]
	coarse_labels = ["Bad" if label == "Not English" else label for label in coarse_labels]
	coarse_labels = ["Bad" if label == "Other" else label for label in coarse_labels]
	return coarse_labels
	
def add_wei_features(target_csv, current_data):
	f_df = pd.read_csv(target_csv)
	f_names = f_df.columns.values.tolist()
	for f_name in f_names:
		f_values = [[value] for value in f_df[f_name].values.tolist()]
		current_data = sp.sparse.hstack((current_data, f_values))
	return current_data
	
def num_of_english_words(text):
	feature = []
	english_words = 0
	for field in text.values:
		total_number_of_words = 0
		words = field.split(" ")
		for word in words:
			if re.match(IS_A_WORD, word):
				total_number_of_words +=1
				if word.lower() in ENGLISH_WORDS:
					english_words += 1
		if total_number_of_words == 0:
			total_number_of_words += 1
		feature.append([english_words / float(total_number_of_words)])
	return feature	
	
def short_comment(text):
	feature = []
	for field in text.values:
		if len(field.split(" ")) < 2:
			feature.append([1])
		else:
			feature.append([0])
	return feature
	
def length(text):
	feature = []
	for field in text.values:
		feature.append(float(len(field.split(" "))))
	return feature	
	
def contain_word_split(text, word):
	feature = []
	for field in text.values:
		if word in field.lower().split(" "):
			feature.append([1])
		else:
			feature.append([0])
	return feature
	
def contain_word(text, word):
	feature = []
	for field in text.values:
		if word in field.lower():
			feature.append([1])
		else:
			feature.append([0])
	return feature
	
def number_of_chars(text, char):
	feature = []
	for field in text.values:
		n_c = field.count(char)
		if n_c > 20:
			feature.append([1])
		else:
			feature.append([0])
	return feature

if __name__ == "__main__":
	main()
