package org.maltparser.core.feature.value;

import org.maltparser.core.feature.function.Function;
/**
 *  
 *
 * @author Johan Hall
 * @since 1.0
**/
public class SingleFeatureValue extends FeatureValue {
	protected int indexCode;
	protected String symbol;
	protected double value;
	protected boolean known;
	
	public SingleFeatureValue(Function function) {
		super(function);
		setIndexCode(0);
		setSymbol(null);
		setKnown(true);
		setValue(0);
	}
	
	public void reset() {
		super.reset();
		setIndexCode(0);
		setSymbol(null);
		setKnown(true);
		setValue(0);
	}
	
	public int getIndexCode() {
		return indexCode;
	}

	public void setIndexCode(int code) {
		this.indexCode = code;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public boolean isKnown() {
		return known;
	}

	public void setKnown(boolean known) {
		this.known = known;
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		if (!symbol.equals(((SingleFeatureValue)obj).symbol))
			return false;
		if (indexCode != ((SingleFeatureValue)obj).indexCode)
			return false;
		return super.equals(obj);
	}
	
	public String toString() {
		return super.toString()+ "{" + symbol + " -> " + indexCode + ", known=" + known +"} ";
	}
}
