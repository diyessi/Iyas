package qa.qcri.qf.cQAdemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import qa.qf.qcri.cqa.CQAcomment;
import qa.qf.qcri.cqa.CQAinstance;

public class OutputVisualization {

	private List<CQAinstance> threadList; 
	
	public OutputVisualization(List<CQAinstance> threads) {
		this.threadList = threads;
	}
	
	public void print() {
		printOnCommandLine();
	}
	
	/**
	 * 
	 */
	public void printOnCommandLine() {
		List<CQAcomment> clist;
		for (CQAinstance thread : threadList) {
			clist = thread.getComments();
			Collections.sort(clist);
			System.out.println("\n****\nThread: " 
					+ thread.getQuestion().getWholeText());
			for (CQAcomment c : clist) {
				System.out.println(c.getScore() + ": " + c.getWholeText());
			}
			System.out.println("****\n");
		}
	}
	
}
