import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


/**
 * This class generates a file and encrypts it using AES(CBC, Random IV) using a key generated with RSA.
 * Moreover, the class signs the file and appends the signature to the file.
 * @author Gal Ringel 300922424
 *
 */
public class Encryptor {

	/* Message Digest Related */ 
	private final static String DIGEST_ALGORITHM = "SHA1";
	private static String SIGNATURE_ALGORITHM = "MD5withRSA";
	private static byte[] resultSignature;

	/* IV Related */
	private final static String RANDOM_ALGORITHM = "SHA1PRNG";
	private final static int IV_SIZE = 16;
	private static byte[] IV = new byte[IV_SIZE];

	/* Key Related */
	private KeyGenerator generator;
	private final static String KEY_ALGORITHM = "AES"; //Symmetric
	private final static int KEY_SIZE = 128; // in bits 
	private static SecretKey secretKey;
	private final static String KEY_ENCRYPTION_ALGORITHM = "RSA"; // Asymmetric

	/* Encryption Related */
	private static byte[] encryptedSecretKey;
	private final static String ENCRPYTION_ALGORTIHM = "AES/CBC/PKCS5Padding";
	private static Cipher encryptionCipher;

	/*Key Store */
	private static KeyStore keystore;


	public Encryptor() {

		// Initializes key generator with details regarding key algorithm and size
		try {

			this.generator = KeyGenerator.getInstance(KEY_ALGORITHM);
			this.generator.init(KEY_SIZE);

		} catch (Exception ex) {
			System.out.println("Could not generate key for encyption due to: " + ex.getMessage());
			System.exit(1);
		}

		// Create the encryption key for the algorithm specified in KEY_ALGORITHM
		secretKey = this.generator.generateKey();

		// Creates a random IV
		try {
			SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
			random.nextBytes(IV);
		}
		catch (Exception ex) {
			System.out.println("Encountered a problem while creating the random IV due to: " + ex.getMessage());

			//we need to terminate 
			System.exit(2);
		}
		
		 //method initializes the encryption cipher according to the encryption algorithm
		try {

			encryptionCipher = Cipher.getInstance(ENCRPYTION_ALGORTIHM);
			encryptionCipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV));

		} catch (Exception ex) {
			System.out.println("Could not create cypher for encryption. Error details: " + ex.getMessage());
			System.exit(3);
		}
	}

	/**
	 * The main method receives as an arguments (in this order) 
	 * 1. the file to encrypt location
	 * 2. the location where we will save the encrypted file
	 * 3. the keystore name and location
	 * 4. KS Alias
	 * 5. KS Password
	 * 
	 * Then the method generates the encrypted file(as described above) and saves it to disk.
	 * @param args encrypted file location, keystore location
	 */
	public static void main(String[] args) {

		//check we received the necessary arguments
		if(args.length < 5) {
			System.out.println("Usage: <File To Encrypt Location> <Encrypted File Location> <Keystore Location> <Keystore Alias> <KeyStore Password>");
			return;
		}

		Encryptor encryptor = new Encryptor();

		File fileToEncrypt = new File(args[0]);
		File encryptedFile = new File(args[1]);
		File keyStoreFile = new File(args[2]);
		String keyStoreAlias = args[3];
		String keyStorePassword = args[4];

		if(!Encryptor.validateArguments(args, fileToEncrypt, encryptedFile, keyStoreFile)) {
			return;
		}
			

		KeyStoreConfig keyStoreConfig = new KeyStoreConfig(keyStoreFile, keyStoreAlias, keyStorePassword);

		// Load and Initialize Key Store according to input parameters
		if(!Encryptor.loadKeyStore(keyStoreConfig)) {
			return;
		}
			

		try
		{
			if(!Encryptor.encryptKey(keyStoreConfig)) {
				return;
			}
				

			if(!encryptor.propegateSignatureForFile(fileToEncrypt, keyStoreConfig)) {
				return;
			}
				

			if(!Encryptor.EncryptFileContent(fileToEncrypt, encryptedFile)) {
				return;
			}

			//now we write a configuration file as described in instructions for the decryptor to use
			Encryptor.WriteConfigFile();

			System.out.println("Successfully encrypted the file: " + fileToEncrypt);

		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * method writes all arguments needed for decryptor to a config file
	 */
	private static void WriteConfigFile() {
		try {
			FileOutputStream fos = new FileOutputStream("config.cfg");

			Properties configFile = new Properties();

			configFile.setProperty("IV", convertbytesToHexaString(IV));
			configFile.setProperty("DigestAlgorithm", DIGEST_ALGORITHM);
			configFile.setProperty("EncryptionAlgorithmForFile", ENCRPYTION_ALGORTIHM);
			configFile.setProperty("KeyAlgorithm", KEY_ALGORITHM);
			configFile.setProperty("KeyEncryptionAlgorithm", KEY_ENCRYPTION_ALGORITHM);
			configFile.setProperty("SignatureEncryptionAlgorithm", SIGNATURE_ALGORITHM);
			configFile.setProperty("EncryptedKey", convertbytesToHexaString(encryptedSecretKey));
			configFile.setProperty("Signature", convertbytesToHexaString(resultSignature));

			configFile.store(fos, null);
		} catch (Exception e) {
			
			System.out.println("Could not write the configuration file! Error details: " + e.getMessage());
		}

	}
	
	/**
	 * Method encrypts the private key using the public one
	 * @param keyStoreConfig - info about the keystore
	 * @return true if method succeeds. false otherwsie
	 */
	private static boolean encryptKey(KeyStoreConfig keyStoreConfig) {

		try{

			// Get entry and extract public key
			PrivateKeyEntry entry = (PrivateKeyEntry) keystore.getEntry( keyStoreConfig.keyStoreAlias, 
					new KeyStore.PasswordProtection(keyStoreConfig.keyStorePassword.toCharArray()));
			PublicKey publicKey = entry.getCertificate().getPublicKey();

			//get an instance of the RSA cypher and encrypt the key using the public key in the keystore
			Cipher keyEncryptionCipher = Cipher.getInstance(KEY_ENCRYPTION_ALGORITHM); //Asymmetric
			keyEncryptionCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			encryptedSecretKey = keyEncryptionCipher.doFinal(secretKey.getEncoded());
		}
		catch(Exception e )
		{
			System.out.println ("While trying to encrypt the key(assymetric encryption), encountered an error: " + e.getMessage());
			return false;
		}

		return true;
	}

	/***
	 * Encrypt the file using the cipher we created
	 * @param fileToEncrypt - input file
	 * @param encryptedFile - output file
	 * @return true if succeeded. False otherwise
	 * @throws IOException - IF we encountered an error with closing the streams
	 */
	private static boolean EncryptFileContent(File fileToEncrypt, File encryptedFile) throws IOException 
	{
		//we initialize out of the try to close them in a finally block
		CipherOutputStream outputStream = null;
		FileInputStream inputStream = null;  

		try {

			//stream to use
			inputStream = new FileInputStream(fileToEncrypt);
			outputStream = new CipherOutputStream(new FileOutputStream(encryptedFile), encryptionCipher);

			byte[] buffer = new byte[512]; //512 block size is arbitrary
			int readBytes;

			//read all file until we reach EOF
			while((readBytes = inputStream.read(buffer)) >= 0)
			{
				outputStream.write(buffer, 0, readBytes);
			}

		} catch(Exception e) {

			System.out.println("Error Encrypting file. Details: " + e.getMessage());
			return false;

		} finally {

			if (inputStream != null) {
				inputStream.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		}

		return true;
	}

	/**
	 * Method creates an instance of the key store according to input parameters
	 * @param keyStoreConfig - input parameters relating to the key store
	 * @return true if the key store has been loaded correctly. False otherwise
	 */
	private static boolean loadKeyStore(KeyStoreConfig keyStoreConfig) 
	{
		try 
		{
			keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream(keyStoreConfig.keyStoreFile), keyStoreConfig.keyStorePassword.toCharArray());
			return true;

		} catch (Exception e) {
			System.out.println("Could not load the key store. Try checking the password. Error details: " + e.getMessage());
			return false;
		}
	}

	/***
	 * Retrieves a file's bytes
	 * @param file - input file to retrieve bytes for
	 * @return bytes of input file
	 */
	private static byte[] FileToBytes(File file) {

		try {

			RandomAccessFile rfa = new RandomAccessFile(file, "r");
			byte[] fileToEncryptBytes = new byte[(int)rfa.length()];
			rfa.read(fileToEncryptBytes);
			rfa.close();

			return fileToEncryptBytes;
		}
		catch (Exception e) {
			System.out.println("Error reading encrypted file. Error Details: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * method creates the signature for the encrypted file
	 * @param fileToEncrypt - file we encrypt
	 * @param keyStoreConfig - keystore info
	 * @return
	 */
	private boolean propegateSignatureForFile(File fileToEncrypt, KeyStoreConfig keyStoreConfig ) {

		try{

			//Read file's Bytes
			byte[] fileToEncryptBytes = FileToBytes(fileToEncrypt);

			if(fileToEncrypt == null) {
				return false;
			}

			//create a digest
			MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
			messageDigest.update(fileToEncryptBytes);


			// Get entry from keystore and create the private key for signature
			PrivateKeyEntry keyEntry = (PrivateKeyEntry) keystore.getEntry(keyStoreConfig.keyStoreAlias, 
					new KeyStore.PasswordProtection(keyStoreConfig.keyStorePassword.toCharArray()));
			PrivateKey privateKey = keyEntry.getPrivateKey();

			createSignature(privateKey, messageDigest.digest());

			return true;

		} catch (Exception e) {
			System.out.println("Error Signing File: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * method signs the file
	 * @param privateKey - private key we will use for signature
	 * @param fileDigest - file digest to encrypt
	 * @throws NoSuchAlgorithmException - could not find algorithm (will not get here)
	 * @throws NoSuchProviderException - could not find provider (will not get here)
	 * @throws InvalidKeyException - bad key (will not get here)
	 * @throws SignatureException
	 */
	private void createSignature(PrivateKey privateKey, byte[] fileDigest) 
			throws NoSuchAlgorithmException, NoSuchProviderException,InvalidKeyException, SignatureException {

		Signature signatureGenerator = Signature.getInstance(SIGNATURE_ALGORITHM);
		signatureGenerator.initSign(privateKey);
		signatureGenerator.update(fileDigest);

		//sign the digest
		resultSignature = signatureGenerator.sign();
	}
	
	/**
	 * method validates the encryptor receives all necessary arguments for encryption
	 * @param args - received arguments
	 * @param fileToEncrypt - file we need to encrypt
	 * @param encryptedFile - location to save the result of encryption
	 * @param keyStoreFile - key store location
	 * @return
	 */
	private static boolean validateArguments(String[] args, File fileToEncrypt, File encryptedFile, File keyStoreFile) {

		System.out.println("Validating Input Arguments");

		if(! fileToEncrypt.exists()) {
			System.out.println("File to encrypt does not exist. Please validate its location and try again!");
			return false;
		}

		if(encryptedFile.exists()) {
			System.out.println("Deleting the file currently in location of encrypted file(" + args[1] + ")");

			try{

				encryptedFile.delete();
			}
			catch(Exception e) {
				System.out.println("Could not delete the encrypted file");
				return false;
			}
		}

		if(! keyStoreFile.exists()) {
			System.out.println("Keystore file specified does not exist. Please validate its location and try again");
			return false;
		}

		return true;
	}
	
	/**
	 * method receives a byte array and converts it to a hex string
	 * @param byteArray byte array to convert
	 * @return hex string representation of byte array
	 */
	private static String convertbytesToHexaString(byte[] byteArray) {
		
	    final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	    
	    char[] hexChars = new char[byteArray.length * 2];
	    int v;
	    
	    for ( int j = 0; j < byteArray.length; j++ ) {
	        v = byteArray[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    
	    return new String(hexChars);
	}
}
