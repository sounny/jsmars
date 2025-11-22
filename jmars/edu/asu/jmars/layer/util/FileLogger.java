package edu.asu.jmars.layer.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.DualOutputStream;
import edu.asu.jmars.util.Util;

/**
 * <p>
 * Redirects stdout, stderr, and DebugLog's output stream to a
 * {@link DualOutputStream} that sends all messages to each original stream to
 * the original stream and to a file-based log, where the filename is provided
 * by <code>jmars.config:log.filename</code>.
 * 
 * <p>
 * The log file has a maximum size of <code>jmars.config:log.maxsize</code>.
 * When the log file grows beyond the maximum size, the first size/<code>jmars.config:log.dropfraction</code>
 * bytes are removed from the head of the file.
 * 
 * <p>
 * The {@link #restoreOutputs()} method will restore the original output
 * streams.
 */
public class FileLogger {
	private static File logFile = new File(Main.getJMarsPath() + Config.get("log.filename", "debuglog-jmars5.txt"));
	private PrintStream stdout;
	private PrintStream stderr;
	private PrintStream dbglog;
	/**
	 * Constructs a new logger; if anything goes wrong, the original streams
	 * should be left as they were.
	 */
	public FileLogger() throws FileNotFoundException {
		stdout = System.out;
		stderr = System.err;
		dbglog = DebugLog.getOutputStream();
		LogOutputStream logStream = new LogOutputStream(logFile, this);
		try {
			System.setOut(new PrintStream(new DualOutputStream(stdout, logStream)));
			System.setErr(new PrintStream(new DualOutputStream(stderr, logStream)));
			DebugLog.setOutputStream(new PrintStream(new DualOutputStream(dbglog, logStream)));
		} catch (RuntimeException e) {
			restoreOutputs();
			throw e;
		}
	}
	/** Restore the original stdout/stderr/debuglog output streams */
	public void restoreOutputs() {
		System.setOut(stdout);
		System.setErr(stderr);
		DebugLog.setOutputStream(dbglog);
	}
	public String getContent() {
		String content;
		try {
			content = Util.readResponse(new FileInputStream(logFile));
		} catch (FileNotFoundException e1) {
			content = "Unable to open logfile " + logFile.getAbsolutePath();
		}
		return content;
	}
}

/**
 * <p>
 * Logs to the given file, trimming off a certain lead fraction of the file when
 * it reaches the maximum allowed size.
 * 
 * <p>
 * NOTE! This class requires that the underlying character stream consist of one
 * byte elements.  Character sets such at UTF-8 are variable and the way bytes
 * are discarded from the front of the file could discard part of a multi-byte
 * character... so don't use them!
 * 
 * <p>
 * Care should be taken with the maxsize parameter, since this class brings
 * several copies of the entire log into memory when trimming.
 * 
 * <p>
 * This class's write methods have a very uniform runtime cost when compared
 * with e.g. {@link BufferedOutputStream}, which would stall for a long period
 * when the log needed to be trimmed. This is accomplished by quickly moving
 * writes into an in-memory buffer that a separate thread saves to disk on a low
 * priority thread, sometime later.
 */
final class LogOutputStream extends OutputStream {
	private final Object lock = new Object();
	private static int MAX_SIZE = Config.get("log.maxsize", 64*1024);
	private static int DROP_FRACTION = Config.get("log.dropfraction", 4);
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	private final File file;
	private final Thread writerThread;
	private final FileLogger logger;
	
	/**
	 * Creates a File Logger around the given output File.
	 * @param log The file to log to
	 * @throws FileNotFoundException If the given file does not exist and cannot be created
	 */
	public LogOutputStream(File log, FileLogger logger) throws FileNotFoundException {
		this.file = log;
		this.logger = logger;
		writerThread = new Thread(new WriterActivity());
		writerThread.setPriority(Thread.MIN_PRIORITY);
		writerThread.setDaemon(true);
		writerThread.setName("LogOutputStream.writerThread");
		writerThread.start();
	}
	public void close() throws IOException {
		synchronized(lock) {
			writerThread.interrupt();
		}
	}
	public void flush() throws IOException {
		synchronized(lock) {
			lock.notifyAll();
			try {
				lock.wait();
			} catch (InterruptedException e) {
				writerThread.interrupt();
			}
		}
	}
	public void write(byte[] b, int off, int len) throws IOException {
		synchronized(lock) {
			try {
				buffer.write(b, off, len);
				lock.notifyAll();
			} catch (RuntimeException e) {
				writerThread.interrupt();
				throw e;
			}
		}
	}
	public void write(byte[] b) throws IOException {
		synchronized(lock) {
			try {
				buffer.write(b);
				lock.notifyAll();
			} catch (RuntimeException e) {
				writerThread.interrupt();
				throw e;
			}
		}
	}
	public void write(int b) throws IOException {
		synchronized(lock) {
			try {
				buffer.write(b);
				lock.notifyAll();
			} catch (RuntimeException e) {
				writerThread.interrupt();
				throw e;
			}
		}
	}
	
	class WriterActivity implements Runnable {
		private OutputStream out;
		private int size;
		public WriterActivity() throws FileNotFoundException {
			out = new BufferedOutputStream(new FileOutputStream(file, true));
			size = (int)file.length();
		}
		public void run() {
			try {
				// loop forever, unless an exception occurs
				while (true) {
					byte[] toWrite;
					synchronized(lock) {
						if (buffer.size() == 0) {
							lock.wait();
						}
						toWrite = buffer.toByteArray();
						buffer.reset();
					}
					if (size + toWrite.length > MAX_SIZE) {
						// if we're over the limit just because of toWrite, shorten it to MAX_SIZE
						if (toWrite.length > MAX_SIZE) {
							byte[] out = new byte[MAX_SIZE];
							System.arraycopy(toWrite, toWrite.length-MAX_SIZE, out, 0, MAX_SIZE);
							toWrite = out;
						}
						// Trim enough chars off the start of the log to make
						// room, trimming at least size/DROP_FRACTION chars so
						// when the log is full, we don't have to trim once
						// per write
						int skip = Math.max(
							(int)((double)size / DROP_FRACTION),
							size + toWrite.length - MAX_SIZE);
						out.close();
						String data = Util.readResponse(new FileInputStream(file));
						file.delete();
						out = new BufferedOutputStream(new FileOutputStream(file));
						
						byte[] bytes = Charset.forName("ISO-8859-1").encode(data.substring(skip)).array();
						size = bytes.length;
						out.write(bytes, 0, size);
					}
					if (toWrite.length > 0) {
						out.write(toWrite);
						out.flush();
						size += toWrite.length;
					}
					synchronized(lock) {
						lock.notifyAll();
					}
				}
			} catch (Exception e) {
				// expecting at least IOException and InterruptException
				
				// close logging thread and restore streams
				try {
					out.close();
				} catch (IOException e1) {
				}
				logger.restoreOutputs();
				
				System.err.println("Closed FileLogger due to exception:");
				e.printStackTrace();
			} finally {
				// make sure any waiting locks are freed
				synchronized(lock) {
					lock.notifyAll();
				}
			}
		}
	}
}

