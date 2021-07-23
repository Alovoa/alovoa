package com.nonononoki.alovoa.runner;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.component.TextEncryptorConverter;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
//simple class to decrypt and encrypt stuff
class TextEncryptorConverterTest {
	
	@Autowired
	private TextEncryptorConverter textEncryptorConverter;
	
	private final String encryptedString = "";
	private final String unencryptedString = "";
	
	@Test
	public void decrypt() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		String decodedString = textEncryptorConverter.decode(encryptedString);
		Assert.assertNotNull(decodedString);
		return;
	}
	
	@Test
	public void encrypt() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		String encodedString = textEncryptorConverter.encode(unencryptedString);
		Assert.assertNotNull(encodedString);
		return;
	}
}
