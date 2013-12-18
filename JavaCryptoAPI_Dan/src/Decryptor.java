import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/***
 * The Decryptor class decrypts the file generated initially by the encryptor class
 * @author DanB
 *
 */
public class Decryptor {

	/* Message Digest Related */ 
	private static String DIGEST_ALGORITHM;
	private static String SIGNATURE_ALGORITHM;
	private static byte[] resultSignature;

	/* IV Related */
	private final static int IV_SIZE = 16;
	private static byte[] IV = new byte[IV_SIZE];

	/* Key Related */
	private static String KEY_ALGORITHM; 
	private static String KEY_ENCRYPTION_ALGORITHM;

	/* Encryption Related */
	private static byte[] encryptedSecretKey;
	private static String ENCRPYTION_ALGORTIHM;

	/*Key Store */
	private static KeyStore keystore;

	/**
	 * Method does the following:
	 * 1. receives command line arguments necessary
	 * 2. reads keystore and config file created in the encryptor class
	 * 3. decrypts the file
	 * 4. calculates the file digest, encrypts it (creates signature) and compares it with the original result
	 * @param args
	 */
	public static void main(String[] args) {

		//check we received the necessary arguments
		if(args.length < 6) {
			System.out.println("Usage: <File To Decrypt Location> <Decrypted File Location> <Keystore Location> <Keystore Alias> <KeyStore Password> <Config File>");
			return;
		}

		File fileToDecrypt = new File(args[0]);
		File decryptedFile = new File(args[1]);
		File keyStoreFile = new File(args[2]);
		String keyStoreAlias = args[3];
		String keyStorePassword = args[4];
		File configFile = new File(args[5]);

		try{
			Decryptor decryptor = new Decryptor();

			if(! Decryptor.validateArguments(args, fileToDecrypt, decryptedFile, keyStoreFile, configFile))
				return;

			KeyStoreConfig keyStoreConfig = new KeyStoreConfig(keyStoreFile, keyStoreAlias, keyStorePassword);

			if(!decryptor.init(keyStoreConfig, configFile))
				return;
			
			//decrypt the file and print it to the specified output and to console.
			PrintWriter out = new PrintWriter(decryptedFile);
			byte[] decryptResultBytes = decryptFile(fileToDecrypt, keyStoreConfig);
			String decryptResult = new String(decryptResultBytes);
			System.out.println(decryptResult);
			out.print(decryptResult);
			out.close();
			
			//now verify the result's signature
			verifyFileSignature(keyStoreConfig, decryptResultBytes);
		}
		catch (Exception e) {
			System.out.println("Encountered an error with decrypting the file. Error details: " + e.getMessage());
		}
	}
	
	/***
	 * Method validates all input arguments given in command line
	 * @param args - command line arguments
	 * @param fileToDecrypt - file the decryptor will need to decrypt
	 * @param decryptedFile - file we will need to save the results.
	 * @param keyStoreFile - all info about the keystore
	 * @param configFile - config file containing all info from encryptor
	 * @return
	 */
	private static boolean validateArguments(String[] args, File fileToDecrypt, File decryptedFile, File keyStoreFile, File configFile) {

		System.out.println("Validating Input Arguments");

		if(! fileToDecrypt.exists()) {
			System.out.println("File to decrypt does not exist. Please validate its location and try again!");
			return false;
		}

		if(decryptedFile.exists()) {
			System.out.println("Deleting the file currently in location of decrypted file(" + args[1] + ")");

			try{

				decryptedFile.delete();
			}
			catch(Exception e) {
				System.out.println("Could not delete the decrypted file");
				return false;
			}
		}

		if(! keyStoreFile.exists()) {
			System.out.println("Keystore file specified does not exist. Please validate its location and try again");
			return false;
		}

		if(! configFile.exists()) {
			System.out.println("Config file specified does not exist. Please validate its location and try again");
			return false;
		}

		return true;
	}
	
	/**
	 * method initializes the parameters the decryptor needs by reading keystore and configuration file into memory
	 * @param keyStoreConfig - keystore info
	 * @param configFile - config file the encryptor created
	 * @return
	 */
	private boolean init(KeyStoreConfig keyStoreConfig, File configFile) {

		return (loadKeyStore(keyStoreConfig) && LoadConfiguration(configFile));

	}
	
	/*** 
	 * Method verifies that the signature given in the config file matches the decrypted file signature
	 * @param keyStoreConfig - keystore info
	 * @param decryptResult - byte array containing the decrypted result
	 */
	private static void verifyFileSignature(KeyStoreConfig keyStoreConfig, byte[] decryptResult) {
		try {
			
			//retrieve entry from keystore config
			PrivateKeyEntry entry = (PrivateKeyEntry)keystore.getEntry(keyStoreConfig.keyStoreAlias, 
					new KeyStore.PasswordProtection(keyStoreConfig.keyStorePassword.toCharArray()));
			PublicKey publicKey = entry.getCertificate().getPublicKey();
			
			// create the message digest we will compare to the one in the config file
			MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
			messageDigest.update(decryptResult);
			byte[] digest = messageDigest.digest();
			
			//sign the digest
			Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
			signature.initVerify(publicKey);
			signature.update(digest);
			
			//last, compare it to the original
			System.out.println("Does the file's signature match the original? Answer: " + (signature.verify(resultSignature) ? "YES" : "False"));
			
		} 
		catch (Exception e) 
		{
			System.out.println("Could verify the file's intergrity. Error details: " + e.getMessage());
		}
	}
	
	/**
	 * Method decrypts the input file
	 * @param fileToDecrpyt - file we need to decrypt
	 * @param keyStoreConfig - keystore info
	 * @return If method succeeds decrypting the file it returns the decrypted result in a form of a byte array. Otherwise, returns null.
	 */
	private static byte[] decryptFile(File fileToDecrpyt, KeyStoreConfig keyStoreConfig ) {
		try{
			Cipher cipher = Cipher.getInstance(KEY_ENCRYPTION_ALGORITHM);


			PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry)keystore.getEntry(keyStoreConfig.keyStoreAlias, 
					new KeyStore.PasswordProtection(keyStoreConfig.keyStorePassword.toCharArray()));
			PrivateKey privateKeyEncrypted = privateKeyEntry.getPrivateKey();

			//decrpyt private key
			cipher.init(Cipher.DECRYPT_MODE, privateKeyEncrypted);
			SecretKey privateKey = new SecretKeySpec(cipher.doFinal(encryptedSecretKey), KEY_ALGORITHM);

			//now, we init the cipher to use file's encryption algorithm(using IV and key)
			cipher = Cipher.getInstance(ENCRPYTION_ALGORTIHM);
			cipher.init(Cipher.DECRYPT_MODE, privateKey, new IvParameterSpec(IV));

			//read all file's bytes and decrpy its
			RandomAccessFile f = new RandomAccessFile(fileToDecrpyt, "r");
			byte[] fileBytes = new byte[(int)f.length()];
			f.read(fileBytes);
			f.close();

			return cipher.doFinal(fileBytes);

		} catch (Exception e) {
			System.out.println("Could not decrypt the file. Error details: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Method loads the configuration file containing info from the encryptor results
	 * @param configFile - file containing all info created by encryptor
	 * @return true if method was successful. false otherwise
	 */
	private boolean LoadConfiguration(File configFile) {
		Properties config = new Properties();
		try {
			config.load(new FileReader(configFile));

			DIGEST_ALGORITHM = config.getProperty("DigestAlgorithm");
			ENCRPYTION_ALGORTIHM = config.getProperty("EncryptionAlgorithmForFile");
			KEY_ALGORITHM = config.getProperty("KeyAlgorithm");
			KEY_ENCRYPTION_ALGORITHM = config.getProperty("KeyEncryptionAlgorithm");
			SIGNATURE_ALGORITHM = config.getProperty("SignatureEncryptionAlgorithm");
			encryptedSecretKey = hexStringToByteArray(config.getProperty("EncryptedKey"));
			resultSignature = hexStringToByteArray(config.getProperty("Signature"));
			IV = hexStringToByteArray(config.getProperty("IV"));

			return true;
		}
		catch (Exception e) {
			System.out.println("Encountered an error while reading the specified configuation file. Error details: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * method reads to memory the key store specified in command line argument
	 * @param keyStoreConfig - all info specified about key store in command line arguments
	 * @return true if method was successful. false otherwise
	 */
	private boolean loadKeyStore(KeyStoreConfig keyStoreConfig) {
		try {

			keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream(keyStoreConfig.keyStoreFile), 
					keyStoreConfig.keyStorePassword.toCharArray());

		} catch (Exception e) {
			System.out.println("Could not load specified KeyStore. Trying fixing input argumets. Error Details: " + e.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * Converts a given hex string to a byte array
	 * @param s - hex string to convert
	 * @return a byte array from the hex string
	 */
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
}
