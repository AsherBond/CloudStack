/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.utils.crypt;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class RSAHelper {
    
	static {
		BouncyCastleProvider provider = new BouncyCastleProvider();
		if (Security.getProvider(provider.getName()) == null)
			Security.addProvider(provider);
	}
	
	private static RSAPublicKey readKey(String key) throws Exception {
		byte[] encKey = Base64.decodeBase64(key.split(" ")[1]);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encKey));
		
		byte[] header = readElement(dis);
		String pubKeyFormat = new String(header);
		if (!pubKeyFormat.equals("ssh-rsa")) 
			throw new RuntimeException("Unsupported format");
			
		byte[] publicExponent = readElement(dis);
		byte[] modulus = readElement(dis);
		
		KeySpec spec = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(publicExponent));
		KeyFactory keyFactory = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
		RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic(spec);
	
		return pubKey;
	}

	private static byte[] readElement(DataInput dis) throws IOException {
        int len = dis.readInt();
        byte[] buf = new byte[len];
        dis.readFully(buf);
        return buf;
	}

	public static String encryptWithSSHPublicKey(String sshPublicKey, String content) {
		String returnString = null;
		try {
	        RSAPublicKey publicKey = readKey(sshPublicKey);
	        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", BouncyCastleProvider.PROVIDER_NAME);
	        cipher.init(Cipher.ENCRYPT_MODE, publicKey , new SecureRandom());
	        byte[] encrypted = cipher.doFinal(content.getBytes());
	        returnString = Base64.encodeBase64String(encrypted);
		} catch (Exception e) {}
		
        return returnString;
	}
}
