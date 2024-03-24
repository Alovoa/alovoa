package com.nonononoki.alovoa.component;

import java.nio.charset.StandardCharsets;
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
import jakarta.persistence.AttributeConverter;

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

	private IvParameterSpec getIvSpec() {
		if (ivSpec == null) {
			ivSpec = new IvParameterSpec(salt.getBytes(StandardCharsets.UTF_8));
		}
		return ivSpec;
	}

	private SecretKeySpec getKeySpec() {
		if (keySpec == null) {
			keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
		}
		return keySpec;
	}

	private Cipher getEnCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException {
		if (enCipher == null) {
			enCipher = Cipher.getInstance(TRANSFORMATION);
			enCipher.init(Cipher.ENCRYPT_MODE, getKeySpec(), getIvSpec());
		}
		return enCipher;
	}

	private Cipher getDeCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException {
		if (deCipher == null) {
			deCipher = Cipher.getInstance(TRANSFORMATION);
			deCipher.init(Cipher.DECRYPT_MODE, getKeySpec(), getIvSpec());
		}
		return deCipher;
	}

	public String encode(String attribute)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException {
		byte[] ba = getEnCipher().doFinal(attribute.getBytes(StandardCharsets.UTF_8));
		return Base64.getUrlEncoder().encodeToString(ba);
	}

	public String decode(String dbData)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException {
		try {
			return new String(getDeCipher().doFinal(Base64.getUrlDecoder().decode(dbData)));
		} catch (BadPaddingException e) {
			throw new BadPaddingException();
		}
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		try {
			return encode(attribute);
		} catch (Exception e) {
			return attribute;
		}
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		try {
			return decode(dbData);
		} catch (Exception e) {
			return dbData;
		}
	}
}