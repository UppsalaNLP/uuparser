package org.maltparser.parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.maltparser.core.exception.MaltChainedException;

public class Diagnostics {
//	protected final boolean diagnostics;
	protected final BufferedWriter diaWriter;
	
	public Diagnostics(String fileName) throws MaltChainedException {
		try {
			if (fileName.equals("stdout")) {
				diaWriter = new BufferedWriter(new OutputStreamWriter(System.out));
			} else if (fileName.equals("stderr")) {
				diaWriter = new BufferedWriter(new OutputStreamWriter(System.err));
			} else {
				diaWriter = new BufferedWriter(new FileWriter(fileName));
			}
		} catch (IOException e) {
			throw new MaltChainedException("Could not open the diagnostic file. ", e);
		}
//		this.diagnostics = (Boolean)manager.getOptionValue("singlemalt", "diagnostics");
//		openDiaWriter(manager.getOptionValue("singlemalt", "diafile").toString());
	}
	
//	public boolean isDiagnostics() {
//		return diagnostics;
//	}

	public BufferedWriter getDiaWriter() {
		return diaWriter;
	}
	
	public void writeToDiaFile(String message) throws MaltChainedException {
		try {
			getDiaWriter().write(message);
		} catch (IOException e) {
			throw new MaltChainedException("Could not write to the diagnostic file. ", e);
		}
	}
	
	public void closeDiaWriter() throws MaltChainedException {
		if (diaWriter != null) {
			try {
				diaWriter.flush();
				diaWriter.close();
			} catch (IOException e) {
				throw new MaltChainedException("Could not close the diagnostic file. ", e);
			}
		}
	}
	
//	public void openDiaWriter(String fileName) throws MaltChainedException {
//		if (diagnostics) {
//			try {
//				if (fileName.equals("stdout")) {
//					diaWriter = new BufferedWriter(new OutputStreamWriter(System.out));
//				} else if (fileName.equals("stderr")) {
//					diaWriter = new BufferedWriter(new OutputStreamWriter(System.err));
//				} else {
//					diaWriter = new BufferedWriter(new FileWriter(fileName));
//				}
//			} catch (IOException e) {
//				throw new MaltChainedException("Could not open the diagnostic file. ", e);
//			}
//		}
//	}
}
