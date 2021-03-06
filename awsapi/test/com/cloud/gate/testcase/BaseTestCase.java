package com.cloud.gate.testcase;


import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.log4j.xml.DOMConfigurator;

public class BaseTestCase extends TestCase {
	protected void setUp() {
		URL configUrl = System.class.getResource("/conf/log4j-cloud-bridge.xml");
		if(configUrl != null) {
			System.out.println("Configure log4j using log4j-cloud-bridge.xml");

			try {
				File file = new File(configUrl.toURI());
				
				System.out.println("Log4j configuration from : " + file.getAbsolutePath());
				DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
			} catch (URISyntaxException e) {
				System.out.println("Unable to convert log4j configuration Url to URI");
			}
		} else {
			System.out.println("Configure log4j with default properties");
		}
	}
	
	public void testDummy() {
	}
	
	public static int getRandomMilliseconds(int rangeLo, int rangeHi) {
		int i = new Random().nextInt();
		
		long pos = (long)i - (long)Integer.MIN_VALUE;
		long iRange = (long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE;
		return rangeLo + (int)((rangeHi - rangeLo)*pos/iRange);
	}
}
