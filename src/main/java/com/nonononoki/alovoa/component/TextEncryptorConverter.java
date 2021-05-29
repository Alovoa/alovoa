package com.nonononoki.alovoa.component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TextEncryptorConverter implements AttributeConverter<String, String> {

	@Value("${app.text.key}")
	private String key;

	@Value("${app.text.salt}")
	private String salt;

	private static final String TRANSFORMATION = "AES/CBC/PKCS5PADDING";

	private static IvParameterSpec ivSpec;
	private static SecretKeySpec keySpec;
	private static Cipher enCipher;
	private static Cipher deCipher;
	
	SecureRandom random = new SecureRandom();

	private IvParameterSpec getIvSpec() throws Exception {
		if (ivSpec == null) {
			ivSpec = new IvParameterSpec(salt.getBytes(StandardCharsets.UTF_8));
		}
		return ivSpec;
	}

	private SecretKeySpec getKeySpec() throws UnsupportedEncodingException {
		if (keySpec == null) {
			keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
		}
		return keySpec;
	}

	private Cipher getEnCipher() throws InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException, Exception {
		if (enCipher == null) {
			enCipher = Cipher.getInstance(TRANSFORMATION);
			enCipher.init(Cipher.ENCRYPT_MODE, getKeySpec(), getIvSpec());
		}
		return enCipher;
	}

	private Cipher getDeCipher() throws InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException, Exception {
		if (deCipher == null) {
			deCipher = Cipher.getInstance(TRANSFORMATION);
			deCipher.init(Cipher.DECRYPT_MODE, getKeySpec(), getIvSpec());
		}
		return deCipher;
	}

	public String encode(String attribute) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, Exception {
		byte[] ba = getEnCipher().doFinal(attribute.getBytes(StandardCharsets.UTF_8));
		return Base64.getUrlEncoder().encodeToString(ba);
	}

	public String decode(String dbData) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, Exception {
		byte[] ba = getDeCipher().doFinal(Base64.getUrlDecoder().decode(dbData));
		return new String(ba);
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