package cc.centralink.devicemanager.util;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


public class XMLParser {

	protected XMLParserHandler xmlParserHandler;
	
	public XMLParser(XMLParserHandler parser) {
		xmlParserHandler = parser;
	}
	
	public Object[] getData(InputStream inputStream) throws SAXException, IOException, ParserConfigurationException {
		Object[] data;
		SAXParserFactory spf = SAXParserFactory.newInstance();
		
		SAXParser sp = spf.newSAXParser();
		
		XMLReader xr = sp.getXMLReader();
		
		if (xmlParserHandler == null) {
			throw new NullPointerException("xmlParserHandler is null");
		} else {
			xr.setContentHandler((ContentHandler) xmlParserHandler);
			
			
			Log.e("Parse data", "pre initialize InputSource");
			
			
			
			try {
				xr.parse(new InputSource(inputStream));
			 } catch (IOException e) {
			   
				 System.err.println("I/O exception reading XML document");
			 
			 } catch (SAXException e) {
			   
				 System.err.println("XML exception reading document.");
			 }
			
			
			
			Log.e("Parse data", "initialize InputSource done");
			
			data = (Object[])xmlParserHandler.getParsedData();
			
			Log.e("Parse data", data[0].toString());
		}
		
		inputStream.close();
		
		return data;
		
	}
	
	public Object[] getData(String urlPath) throws SAXException, IOException, ParserConfigurationException {
		
		StringBuilder builder = new StringBuilder();
		HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 5000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        int timeoutSocket = 10000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        HttpClient client = new DefaultHttpClient(httpParameters);
        
        
        HttpGet httpGet = new HttpGet(urlPath);
        try {
        	HttpResponse response = client.execute(httpGet);
        	StatusLine statusLine = response.getStatusLine();
        	int statusCode = statusLine.getStatusCode();
        	if (statusCode == 200) {
        		HttpEntity entity = response.getEntity();
        		InputStream content = entity.getContent();
        		BufferedReader reader = new BufferedReader(new InputStreamReader(content));
        		String line;
        		
        		while ((line = reader.readLine()) != null) {
        			builder.append(line);
        			//Log.e("download data", line);
        		}
      		
        		Object[] data = getData(content);
        		
        		return data;
        		
        	} else {
        		
        		Log.e("XMLParser", "Failed to download file");
        	}
        } catch (SocketTimeoutException e) {
        	
        	
        	
    	} catch (ConnectTimeoutException e) {
    		
        	
        } catch (ClientProtocolException e) {
        	
            
        } catch (IOException e) {
        	
        }
        
        return null;
		
	}
	
	
	
}
