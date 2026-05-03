package com.nonononoki.alovoa.runner;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
	void decrypt() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		String decodedString = textEncryptorConverter.decode(encryptedString);
		assertNotNull(decodedString);
    }
	
	@Test
	void encrypt() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		String encodedString = textEncryptorConverter.encode(unencryptedString);
		assertNotNull(encodedString);
    }

	@Test
	void randomTest() {
		String rand = RandomStringUtils.random(10, 0, 0, true, true, null, new SecureRandom());
		System.out.println(rand);
	}
}
