package com.nonononoki.alovoa.component;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.model.AlovoaException;

@Component
public class TextEncryptorConverter implements AttributeConverter<String, String> {

	@Value("${app.text.key}")
	private String key;

//	@Value("${app.text.salt}")
//	private String salt;

	private final String TRANSFORMATION = "AES/CBC/PKCS5PADDING";

	private static IvParameterSpec ivSpec;
	private static SecretKeySpec keySpec;
	private static Cipher enCipher;
	private static Cipher deCipher;
	
	SecureRandom random = new SecureRandom();

	private IvParameterSpec getIvSpec() throws Exception {
		if (ivSpec == null) {
//			ivSpec = new IvParameterSpec(salt.getBytes("UTF-8"));
			byte[] bytesIV = new byte[16];
		    random.nextBytes(bytesIV);
			ivSpec = new IvParameterSpec(bytesIV);
		}
		return ivSpec;
	}

	private SecretKeySpec getKeySpec() throws Exception, UnsupportedEncodingException {
		if (keySpec == null) {
			keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
		}
		return keySpec;
	}

	private Cipher getEnCipher() throws Exception, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		if (enCipher == null) {
			enCipher = Cipher.getInstance(TRANSFORMATION);
			enCipher.init(Cipher.ENCRYPT_MODE, getKeySpec(), getIvSpec());
		}
		return enCipher;
	}

	private Cipher getDeCipher() throws Exception, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		if (deCipher == null) {
			deCipher = Cipher.getInstance(TRANSFORMATION);
			deCipher.init(Cipher.DECRYPT_MODE, getKeySpec(), getIvSpec());
		}
		return deCipher;
	}

	public String encode(String attribute) throws Exception, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		byte[] ba = getEnCipher().doFinal(attribute.getBytes());
		String e = Base64.getUrlEncoder().encodeToString(ba);
		return e;
	}

	public String decode(String dbData) throws Exception, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		byte[] ba = getDeCipher().doFinal(Base64.getUrlDecoder().decode(dbData));
		String s = new String(ba);
		return s;
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		try {
			return encode(attribute);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		try {
			return decode(dbData);
		} catch (Exception e) {
			return null;
		}
	}
}