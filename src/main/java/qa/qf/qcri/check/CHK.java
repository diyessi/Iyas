package qa.qf.qcri.check;

import java.io.File;

/**
 * A class that contains methods to check. 
 * Borrowed from Lump 2.
 * 
 * @author albarron
 *
 */
public class CHK {
	
	/**
	 * throw CheckFailedError if false
	 * @param condition
	 */
	public final static void CHECK(final boolean condition){
		if (!condition) {
			throw new CheckFailedError();
		}
	}
	
	/**
	 * throw CheckFailedError if false, displaying the required message
	 * @param condition
	 * @param message
	 */
	public final static void CHECK(final boolean condition, 
									final String message){
		if (!condition) {
			throw new CheckFailedError(message);
		}		
	}
	
	/**
	 * Check that the given object is not null; throws a CheckFailedError
	 * if it is 
	 * @param object
	 */
	public final static void CHECK_NOT_NULL(final Object object){
		if (object == null) {
			throw new CheckFailedError(new NullPointerException());
		}		
	}
	
  /**
   * Throw error if the file cannot be read. 
   *
   * @param file file to be read
   * @param message additional information to display (can be empty)
   */
  public final static void CAN_READ(final File file, 
      final String message){
    if (! file.canRead()) {
      throw new CheckFailedError(
          String.format("Check if file %s exists and has the proper reading permissions.\n%s",
          message)
          );
    }
  }

}
