package cc.centralink.devicemanager.util;

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Stack;

public abstract class XMLParserHandler extends DefaultHandler {
	
	protected static final String TAG = "ParserHandler";
	
	private Stack<String> in_node;
	public Stack<String> getInNode() {
		
		return in_node;
		
	}
	
	private boolean debugMode = true;
	
	public abstract Object getParsedData();
	
	@Override
	public void startDocument() throws SAXException {
		
		
		in_node = new Stack<String>();		
	}
	
	@Override
	public void endDocument() throws SAXException {
		
	}
	
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if (isDebugMode()) {
			Log.v(TAG, "startElement: qName=" + qName); 
			
			for (int i = 0; i < atts.getLength(); i++) {
				Log.v(TAG, "\t\t atts[" + i + "]getQName = " + atts.getQName(i));
				Log.v(TAG, "\t\t atts[" + i + "]getValue = " + atts.getValue(i));
				
			}
		}
		
		in_node.push(qName);
	}
	
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (isDebugMode()) {
			Log.v(TAG, "endElement: qName = " + qName);
			
		}
		
		in_node.pop();
	}
	
	
	@Override
	public void characters(char ch[], int start , int len) {
		String fetchStr = new String(ch).substring(start, start + len);
		
		if (isDebugMode()) {
			Log.v(TAG, "\t characters: ch = " + fetchStr);
			
			characters(fetchStr);
		}
	}
	
	public void characters(String fetchStr) {
		
	}
	
	public String printNodePos() {
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0 ; i < in_node.size(); i++) {
			sb.append(in_node.get(i));
		}
		
		sb.append("\n");
		
		return sb.toString();
	}
	
	public static String findAttr(Attributes atts, String findStr) {
		int i;
		for (i = 0 ; i < atts.getLength(); i++) {
			if (atts.getQName(i).compareToIgnoreCase(findStr) == 0) {
				break;
			}
		}
		
		return atts.getValue(i);
	}
	
	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}
	
	public boolean isDebugMode() {
		return debugMode;
	}

}
