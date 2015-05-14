package qa.qcri.qf.emnlp2015;

public class LocalConfig {
	
	public static String PROJECT_FOLDER = "/home/noname/rs/work/ikernpersonality";
	
	public static String TRAIN_DIR = PROJECT_FOLDER + "/data/train_small";
	public static String TRAIN_SMALL = TRAIN_DIR + "/user_status.csv";
	public static String TRAIN_SMALL_TARGETS = TRAIN_DIR  + "/big5.csv";
	
	public static String DEV_DIR = PROJECT_FOLDER + "/data/dev_small";
	public static String DEV_SMALL = DEV_DIR + "/user_status.csv";
	public static String DEV_SMALL_TARGETS = DEV_DIR  + "/big5.csv";
	
	public static String RANDOM_DATASET = PROJECT_FOLDER + "/target/user_status.random.csv";
}
