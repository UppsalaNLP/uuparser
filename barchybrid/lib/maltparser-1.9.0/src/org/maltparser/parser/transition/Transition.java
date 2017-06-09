package org.maltparser.parser.transition;


/**
 * Transition contains one individual transition. For example, Nivre arc-eager algorithms have the unlabeled 
 * transition <code>SH</code>, <code>RE</code> and the labeled transition<code>RA</code>, <code>LA</code>. These
 * transition will be four individual transition.
 * 
 * @author Joakim Nivre
 * @author Johan Hall
*/
public class Transition implements Comparable<Transition> {
	/**
	 * Transition code
	 */
	private final int code;
	/**
	 * Transition symbol
	 */
	private final String symbol;
	/**
	 * <code>true</code> if the transition is labeled, otherwise <code>false</code>
	 */
	private final boolean labeled;
	private final int cachedHash;
	/**
	 * Creates a transition 
	 * 
	 * @param code	Transition code
	 * @param symbol	Transition name
	 * @param labeled	<code>true</code> if the transition is labeled, otherwise <code>false</code>
	 */
	public Transition(int code, String symbol, boolean labeled) {
		this.code = code;
		this.symbol = symbol;
		this.labeled = labeled;
		final int prime = 31;
		int result = prime + code;
		result = prime * result + (labeled ? 1231 : 1237);
		this.cachedHash = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
	}

	/**
	 * Returns the transition code
	 * 
	 * @return the transition code
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * Returns the transition symbol
	 * 
	 * @return	the transition symbol
	 */
	public String getSymbol() {
		return symbol;
	}
	
	/**
	 * Returns true if the transition is labeled, otherwise false
	 * 
	 * @return <code>true</code> if the transition is labeled, otherwise <code>false</code>
	 */
	public boolean isLabeled() {
		return labeled;
	}

	
	public int compareTo(Transition that) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    if (this.code < that.code) return BEFORE;
	    if (this.code > that.code) return AFTER;
	    return EQUAL;
	}
	
	@Override
	public int hashCode() {
		return cachedHash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Transition other = (Transition) obj;
		if (code != other.code)
			return false;
		if (labeled != other.labeled)
			return false;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return symbol + " [" + code +"] " + labeled;
	}
}
