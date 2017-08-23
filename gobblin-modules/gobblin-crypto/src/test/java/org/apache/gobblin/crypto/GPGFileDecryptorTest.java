package org.apache.gobblin.crypto;

import com.google.common.base.Charsets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Test class for {@link GPGFileDecryptor}
 *
 * Test key and test passphrase are generated offline
 */
public class GPGFileDecryptorTest {

  private static final String fileDir = "src/test/resources/crypto/gpg/";
  private static final String privateKey = "private.key";
  private static final String passwdBasedFile = "PasswordBasedEncryptionFile.txt.gpg";
  private static final String keyBasedFile = "KeyBasedEncryptionFile.txt.gpg";
  private static final String passPhrase = "test";

  private static final String expectedPasswdFileContent = "This is a password based encryption file.\n";
  private static final String expectedKeyFileContent = "This is a key based encryption file.\n";

  @Test
  public void keyBasedDecryptionTest() throws IOException {
    try(InputStream is = GPGFileDecryptor.decryptFile(
        FileUtils.openInputStream(
            new File(fileDir, keyBasedFile)), FileUtils.openInputStream(new File(fileDir, privateKey)), passPhrase)) {
      Assert.assertEquals(IOUtils.toString(is, Charsets.UTF_8), expectedKeyFileContent);
    }
  }

  @Test
  public void passwordBasedDecryptionTest() throws IOException {
    try(InputStream is = GPGFileDecryptor.decryptFile(
        FileUtils.openInputStream(new File(fileDir, passwdBasedFile)), passPhrase)) {
      Assert.assertEquals(IOUtils.toString(is, Charsets.UTF_8), expectedPasswdFileContent);
    }
  }

}
