import sys
import math

A_LABELS = ["Not_English", "Good", "Potential", "Dialogue", "Bad"]

B_LABELS = ["Yes", "No", "Unsure"]

def main():
	
	'''
		Subtask A
	'''
	
	dir_prefix = "a/dev/"
	
	dev_cids = [line.split("|EV| #")[1].split("\t")[1].strip() for line in open(dir_prefix + "Bad.svm", "r")]
	
	preds = []
	for label in A_LABELS:
		preds.append([float(line.strip()) for line in open(dir_prefix + label + ".pred", "r")])
		
	predictions = []
		
	for prediction_index, preds in enumerate(zip(*preds)):
		label_index = preds.index(max(preds))
		predictions.append(A_LABELS[label_index])
			
	dialogue_ids = set([line.strip() for line in open("../data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-devel.xml.dialogue.txt", "r")])
	
	with open("subtask_a.pred", "w") as out:
		for test_id, prediction in zip(dev_cids, predictions):
			if test_id in dialogue_ids:
				prediction = "Dialogue"
			out.write(str(test_id) + "\t" + str(prediction) + "\n")
		
	'''
		Subtask B
	'''
	
	dir_prefix = "b/dev/"
	
	dev_qids = [line.split("|EV| #")[1].split("\t")[0].strip() for line in open(dir_prefix + "Yes.svm", "r")]
	
	preds = []
	for label in B_LABELS:
		preds.append([float(line.strip()) for line in open(dir_prefix + label + ".pred", "r")])
		
	question_id_list = []
	question_counter = {}
		
	for prediction_index, predictions in enumerate(zip(*preds)):
		label_index = predictions.index(max(predictions))
		question_id = dev_qids[prediction_index]
		
		if question_id not in question_counter:
			question_id_list.append(question_id)
			question_counter[question_id] = {"Yes": 0, "No": 0, "Unsure": 0}
			
		question_counter[question_id][B_LABELS[label_index]] += 1
		
	with open("subtask_b.pred", "w") as out:
		for question_id in question_id_list:
			
			output_label = "Unsure"
			
			counter = question_counter[question_id]
			if counter["Yes"] > counter["No"] and counter["Yes"] > counter["Unsure"]:
				output_label = "Yes"
			elif counter["No"] > counter["Yes"] and counter["No"] > counter["Unsure"]:
				output_label = "No"
			
			out.write(question_id + "\t" + output_label + "\n")
	
if __name__ == "__main__":
	main()
