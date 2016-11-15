package org.dla.threads;

/**
 *
 * @author Dennis Andress
 */

/**
 * Produces a 8 didgit string of letters and numbers which is
 * a base 36 representation ofSystem.currentTimeMillis(). Duplicates
 * are avoided by keeping the last used timestamp in a static member, and making
 * sure it is not used twice.
 *
 *
 * @author dennis.andress
 */
public class SequencedName {

	private long tick = 0l;
	private String prefix = null;
	private static long prev = 0;


	/** The idea for SequenceName comes from FoxPro, where there was a function called SYS2015().
	 * SYS2015() was really handy for unique file names, in the world of MS-DOS where 8 characters were the limit!
	 * <p/>
	 * A separate class was needed to avoid duplicate results
	 */
	class sys2015 {

		private final String[] sequence = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
			"A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
			"K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

		String call() {
			StringBuilder buf = new StringBuilder();
			long hBasis = tick;
			long hMod = 0;
			while (hBasis >= 36) {
				hMod = hBasis % 36;
				hBasis = hBasis / 36;
				buf.append(sequence[(int) hMod]);
			}
			hMod = hBasis % 35;
			buf.append(sequence[(int) hMod]);//.append(buf.toString());
			buf.reverse();
			if (prefix != null)
				buf.insert(0, prefix);
			return buf.toString();
		}
	}

	/**
	 * Constructor which prepends a string to the front
	 * of the output. Provides a way to personalized the output
	 *
	 * @param prefix A string of anything.
	 */
	public SequencedName(final String prefix) {
		this.prefix = prefix;
	}

	/** Constructor to use for when you don't want to prefix something
	 * on the front of the result.
	 * Default`Xtor
	 *
	 */
	public SequencedName() {
	}

	/** Generates and returns a SequencedName. Compares the current output of
	 * {@code System.currentTimeMillis()} to the previously used value and loops until they
	 * are different.
	 *
	 * @return
	 */
	public String generate() {
		tick = System.currentTimeMillis();
		while (tick == prev) {
			try {
				Thread.sleep(1);  // I hate sleep(), but I guess it's better with it than without it...
				tick = System.currentTimeMillis();
			} catch (InterruptedException ex) {
			}
		}
		sys2015 s = new sys2015();
		prev = tick;
		return s.call();
	}
	public static void main(String[] args) {
		SequencedName sn = new SequencedName("_");
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
		System.out.println("=------ >  " + sn.generate());
	}

}

