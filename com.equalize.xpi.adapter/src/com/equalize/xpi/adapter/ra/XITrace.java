/*
 * (C) 2006 SAP XI 7.1 Adapter Framework Resource Adapter Skeleton
 */
package com.equalize.xpi.adapter.ra;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.sap.tc.logging.Category;
import com.sap.tc.logging.Location;
import com.sap.tc.logging.Severity;

/**
 * <p>Basic <code>XITrace</code> wrapper class for the new AdapterFramework, which
 * will make the usage of SAP J2EE standard logging and tracing more convenient and
 * consistent throughout the sample adapter.
 * (ra implementation specific)
 * @version: $Id: //tc/xpi.external/NW07_07_REL/src/_sample_rar_module/rar/src/com/sap/aii/af/sample/adapter/ra/XITrace.java#1 $
 **/

public class XITrace {
  private String className = null;
  protected Location location = null;
  protected static boolean tracing = true;

  // The SAP logging/tracing API severity levels are replicated here for
  // convenience, so that it's not needed to import the logging package, when
  // the trace wrapper is used.
  public static final int SEVERITY_ALL = Severity.ALL;
  public static final int SEVERITY_DEBUG = Severity.DEBUG;
  public static final int SEVERITY_ERROR = Severity.ERROR;
  public static final int SEVERITY_FATAL = Severity.FATAL;
  public static final int SEVERITY_GROUP = Severity.GROUP;
  public static final int SEVERITY_INFO = Severity.INFO;
  public static final int SEVERITY_MAX = Severity.MAX;
  public static final int SEVERITY_MIN = Severity.MIN;
  public static final int SEVERITY_NONE = Severity.NONE;
  public static final int SEVERITY_PATH = Severity.PATH;
  public static final int SEVERITY_WARNING = Severity.WARNING;

  /**
   * Constructor for trace utility class
   * @param  className: Name of the class that uses this trace utility, usually <code>this.getClass().getName()</code>
   */
  public XITrace(String className) {
     try {
      	this.className = className;
        location = Location.getLocation(className);
     } catch (Exception t) {
		//$JL-EXC$ No other handling required-.
        t.printStackTrace();
     }
  }

  public String toString() {
    return className;
  }

  /**
   * Writes a trace entry that the specified method was entered.
   *
   * @param  signature   Signature of the method
   */
  public void entering(String signature) {
    if (location != null) {
      location.entering(signature);
    }
  }

  /**
   *  Writes a trace entry that the specified method was entered.
   *
   * @param  signature   Signature of the method
   * @param  args  Arguments as object references
   */
  public void entering(String signature, Object[] args) {

    if (location != null) {
      location.entering(signature, args);
    }
  }


  /**
   * Writes a trace entry that the specified method is about to be exited.
   *
   * @param  signature   Signature of the method
   */
  public void exiting(String signature) {

    if (location != null) {
      location.exiting(signature);
    }
  }


  /**
   * Writes a trace entry that the specified method is about to be exited.
   *
   * @param  signature   Signature of the method
   * @param  res   Result as object references
   */
  public void exiting(String signature, Object res) {

    if (location != null) {
      location.exiting(signature, res);
    }
  }

  /**
   * Writes a trace entry that the specified throwable is about to be thrown.
   *
   * @param  signature   Signature of the method
   * @param  t     Throwable
   */
  public void throwing(String signature, Throwable t) {
    if (location != null) {
      location.throwing(signature, t);
    }
  }

  /**
   * Writes a trace entry that the specified throwable was caught.
   *
   * @param  signature   Signature of the method
   * @param  t     Throwable
   */
  public void catching(String signature, Throwable t) {
    if (location != null) {
      if(beLogged(XITrace.SEVERITY_WARNING)){
          // only build String from StackTrace if it is needed...   
	      ByteArrayOutputStream oStream = new ByteArrayOutputStream(1024);
	      PrintStream pStream = new PrintStream(oStream);
	      t.printStackTrace(pStream);
	      pStream.close();
	      String stackTrace = oStream.toString();
	      location.warningT(signature, "Catching {0}", new Object[] {stackTrace});
      }
    }
  }

  /**
   * Writes a debug trace and log message.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      respective arguments
   */
  public void debugT(String signature, Category category, String msg) {
    if (location != null) {
      if (category != null) {
        location.debugT(category, signature, msg);
      } else {
        location.debugT(signature, msg);
      }
    }
  }

	/**
	 * Writes a debug trace message.
	 *
	 * @param signature   Signature of the method
	 * @param msg   Debug message text
	 */
	public void debugT(String signature, String msg) {
		if (location != null) {
			location.debugT(signature, msg);
		}
	}

  /**
   * Writes a debug trace and log message.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      arguments
   * @param  args  Can be single argument but also array of arguments. For
   *      collections, their method toArray is used.
   */
  public void debugT(String signature, Category category, String msg,
    Object[] args) {
    if (location != null) {
      if (category != null) {
        location.debugT(category, signature, msg, args);
      } else {
        location.debugT(signature, msg, args);
      }
    }
  }

	/**
	 * Writes a debug trace message.
	 *
	 * @param signature  Signature of the method
	 * @param msg  Can have placeholders of java.text.MessageFormat syntax in the
	 * form of <code>{ <number>}</code> to be substituted by the arguments.
	 * @param args  Can be single argument but also array of arguments. For
	 * 				collections, their method toArray is used.
	 */
	public void debugT(String signature, String msg, Object[] args) {
		if (location != null) {
			location.debugT(signature, msg, args);
		}
	}

  /**
   * Writes an info trace and log message.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      respective arguments
   */
  public void infoT(String signature, Category category, String msg) {
    if (location != null) {
      if (category != null) {
        location.infoT(category, signature, msg);
      } else {
        location.infoT(signature, msg);
      }
    }
  }

	/**
	 * Writes an info trace message.
	 *
	 * @param signature    Signature of the method
	 * @param msg   Info message text
	 */
	public void infoT(String signature, String msg) {
		if (location != null) {
			location.infoT(signature, msg);
		}
	}

  /**
   * Writes an info trace and log message.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      respective arguments
   * @param  args  Can be single argument but also array of arguments. For
   *      collections, their method toArray is used.
   */
  public void infoT(String signature, Category category, String msg,
    Object[] args) {
    if (location != null) {
      if (category != null) {
        location.infoT(category, signature, msg, args);
      } else {
        location.infoT(signature, msg, args);
      }
    }
  }

  /**
   * Writes an info trace message.
   *
   * @param signature   Signature of the method
   * @param msg   Can have placeholders of java.text.MessageFormat syntax in the
   *        form of <code>{ <number>}</code> to be substituted by the arguments.
   * @param args   Can be single argument but also array of arguments. For
   *        collections, their method toArray is used.
   */
	public void infoT(String signature, String msg, Object[] args) {
		if (location != null) {
			location.infoT(signature, msg, args);
		}
	}

  /**
   * Writes an warning trace and log message.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      respective arguments
   */
  public void warningT(String signature, Category category, String msg) {
    if (location != null) {
      if (category != null) {
        location.warningT(category, signature, msg);
      } else {
        location.warningT(signature, msg);
      }
    }
  }

	/**
	 * Writes a warning trace message.
	 *
	 * @param signature  Signature of the method
	 * @param msg  Warning message text
	 */
	public void warningT(String signature, String msg) {
		if (location != null) {
			location.warningT(signature, msg);
		}
	}

  /**
   * Writes a warning trace and log message.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      respective arguments
   * @param  args  Can be single argument but also array of arguments. For
   *      collections, their method toArray is used.
   */
  public void warningT(String signature, Category category, String msg,
    Object[] args)	{
    if (location != null) {
      if (category != null) {
        location.warningT(category, signature, msg, args);
      } else {
        location.warningT(signature, msg, args);
      }
    }
  }

	/**
	 * Writes a warning trace message.
	 *
	 * @param signature  Signature of the method
	 * @param msg  Can have placeholders of java.text.MessageFormat syntax in the
	 *        form of <code>{ <number>}</code> to be substituted by the arguments.
	 * @param args  Can be single argument but also array of arguments. For
	 *      collections, their method toArray is used.
	 */
	public void warningT(String signature, String msg, Object[] args)	{
		if (location != null) {
			location.warningT(signature, msg, args);
		}
	}

  /**
   * Writes an error trace and log message.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      respective arguments
   */
  public void errorT(String signature, Category category, String msg) {
    if (location != null) {
      if (category != null) {
        location.errorT(category, signature, msg);
      } else {
        location.errorT(signature, msg);
      }
    }
  }

	/**
	 * Writes an error trace message.
	 *
	 * @param  signature   Signature of the method
	 * @param  category    Category of the trace
	 * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
	 *      in the form of <code>{ <number>}</code> to be substituted by the
	 *      respective arguments
	 */
	public void errorT(String signature, String msg) {
		if (location != null) {
			location.errorT(signature, msg);
		}
	}

  /**
   * Writes an error trace and log message.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      respective arguments
   * @param  args  Can be single argument but also array of arguments. For
   *      collections, their method toArray is used.
   */
  public void errorT(String signature, Category category, String msg,
    Object[] args) {
    if (location != null) {
      if (category != null) {
        location.errorT(category, signature, msg, args);
      } else {
        location.errorT(signature, msg, args);
      }
    }
  }

	/**
	 * Writes an error trace and log message.
	 *
	 * @param  signature   Signature of the method
	 * @param  category    Category of the trace
	 * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
	 *      in the form of <code>{ <number>}</code> to be substituted by the
	 *      respective arguments
	 * @param  args  Can be single argument but also array of arguments. For
	 *      collections, their method toArray is used.
	 */
	public void errorT(String signature, String msg, Object[] args) {
		if (location != null) {
			location.errorT(signature, msg, args);
		}
	}

  /**
   * Writes a fatal trace and log message.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      respective arguments
   */
  public void fatalT(String signature, Category category, String msg) {
    if (location != null) {
      if (category != null) {
        location.fatalT(category, signature, msg);
      } else {
        location.fatalT(signature, msg);
      }
    }
  }

	/**
	 * Writes a fatal trace message.
	 *
	 * @param  signature   Signature of the method
	 * @param  category    Category of the trace
	 * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
	 *      in the form of <code>{ <number>}</code> to be substituted by the
	 *      respective arguments
	 */
	public void fatalT(String signature, String msg) {
		if (location != null) {
			location.fatalT(signature, msg);
		}
	}

  /**
   * Writes a fatal trace and log message.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      respective arguments
   * @param  args  Can be single argument but also array of arguments. For
   *      collections, their method toArray is used.
   */
  public void fatalT(String signature, Category category, String msg,
    Object[] args) {
    if (location != null) {
      if (category != null) {
        location.fatalT(category, signature, msg, args);
      } else {
        location.fatalT(signature, msg, args);
      }
    }
  }

	/**
	 * Writes a fatal trace message.
	 *
	 * @param  signature   Signature of the method
	 * @param  category    Category of the trace
	 * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
	 *      in the form of <code>{ <number>}</code> to be substituted by the
	 *      respective arguments
	 * @param  args  Can be single argument but also array of arguments. For
	 *      collections, their method toArray is used.
	 */
	public void fatalT(String signature, String msg, Object[] args) {
		if (location != null) {
			location.fatalT(signature, msg, args);
		}
	}

  /**
   * Writes an error trace message if a given condition is false.
   *
   * @param  signature   Signature of the method
   * @param  category    Category of the trace
   * @param  assertion   A true or false condition to be evaluated
   * @param  msg   Can have placeholders of java.text.MessageFormat syntax,
   *      in the form of <code>{ <number>}</code> to be substituted by the
   *      respective arguments
   */
  public void assertion(String signature, Category category, boolean assertion,
    String msg) {
    if (location != null) {
      if (category != null) {
        location.assertion(category, signature, assertion, msg);
      } else {
        location.assertion(signature, assertion, msg);
      }
    }
  }


  /**
   * <p>This method can be used to avoid unneccessary and unperformant object
   * creation, in cases where debugging is switched off, or the serverity level
   * is lower than the current one. In such cases the parameter object array
   * will still be created and passed into the logging/tracing methods, without
   * beeing used/logged at all.
   * </p>
   * <p>This method is to be used, if huge byte arrays or streams are being
   * traced. For small objects which implement the toString method it shouldn't
   * be neccessary, to call this method. We suggest the following usage:
   * <br><br><pre><code>
   * TRACE.infoT(SIGNATURE, Categories.SYSTEM, "Message bytes: {0} ",
   * &nbsp;&nbsp;&nbsp;&nbsp;TRACE.beLogged(TRACE.SEVERITY_INFO) ?
   * &nbsp;&nbsp;&nbsp;&nbsp;new Object [] {new String(message.getBytes())} :
   * &nbsp;&nbsp;&nbsp;&nbsp;null);
   * </code></pre>
   *
   * @param   severity      One of the severity constants in the
   * <code>Trace</code> wrapper class.
   * @return if a logging/tracing call would currently be logged at the given
   * severity level.
   */
  public boolean beLogged(int severity) {
    if (location != null) {
      return location.beLogged(severity);
    } else {
      return false;
    }
  }
}
