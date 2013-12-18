import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.util.Properties;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
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


	/**
	 * 
	 */
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

		// We need to receive 5 arguments from the user
		if(args.length < 5) {
			System.out.println("Usage: <File To Encrypt Location> <Encrypted File Location> <Keystore Location> <Keystore Alias> <KeyStore Password>");
			return;
		}

		// Inits Encryptor (Creates the key generator, cipher, IV...)
		Encryptor encryptor = new Encryptor();

		// Validate and store the given file to encrypt
		File fileToEncrypt = new File(args[0]);
		if(!fileToEncrypt.exists()) {
			System.err.println("File to encrypt does not exist. Please validate its location and try again!");
			return;
		}
		
		// Validate and store the given file name to encrypt 
		File encryptedFile = new File(args[1]);
		if(encryptedFile.exists()) {
			System.err.println("Deleting the file currently in location of encrypted file(" + args[1] + ")");
			try {
				encryptedFile.delete();
			}
			catch(Exception ex) {
				System.err.println("Could acess or delete the encrypted file");
				return;
			}
		}
		
		// Validate and store the given keyStroe
		File keyStoreFile = new File(args[2]);
		if(!keyStoreFile.exists()) {
			System.err.println("Keystore file specified does not exist. Please validate its location and try again");
			return;
		}
		
		String keyStoreAlias = args[3];
		String keyStorePassword = args[4];
		
		try 
		{
			// Load and Initialize Key Store according to input parameters
			keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());
		} catch (Exception ex) {
			System.err.println("Could not load the key store. Try checking the password. Error details: " + ex.getMessage());
			return;
		}
			
		try {
			// Encrypet the private key
			encryptor.keyEncryption(keyStoreAlias, keyStorePassword);
		}
		catch (Exception ex)
		{
			System.err.println("While trying to encrypt the key(assymetric encryption), encountered an error: " + ex.getMessage());
			return;
		}
		

		try {
			encryptor.generateDigitalSignatureForFile(fileToEncrypt, keyStoreAlias, keyStorePassword);
		}
		catch (Exception e) {
			System.out.println("Error Signing File: " + e.getMessage());
			return;
		}
			
		try{
			encryptor.Encrypt(fileToEncrypt, encryptedFile);
		}
		catch (IOException ex) {
			System.err.println("Error Encrypting file. Details: " + ex.getMessage());
		} 

		//now we write a configuration file as described in instructions for the decryptor to use
		try {
			encryptor.WriteConfigFile();
		}
		catch (Exception ex) {
			System.out.println("Could not write the configuration file! Error details: " + ex.getMessage());
		}

		
		// Everything went well :)
		System.out.println("Successfully encrypted the file: " + fileToEncrypt);

	}
	
	/**
	 * method writes all arguments needed for decryptor to a config file
	 * @throws IOException 
	 */
	private void WriteConfigFile() throws IOException {
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
	}
	
	/**
	 * Method encrypts the private key using the public one
	 * @param keyStoreConfig - info about the keystore
	 * @return true if method succeeds. false otherwsie
	 * @throws KeyStoreException 
	 * @throws UnrecoverableEntryException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws NoSuchPaddingException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	private void keyEncryption(String keyStoreAlias, String KeyStorePassword) throws NoSuchAlgorithmException,
																					UnrecoverableEntryException,
																					KeyStoreException, 
																					InvalidKeyException, 
																					NoSuchPaddingException, 
																					IllegalBlockSizeException, 
																					BadPaddingException {

			// Extract the private key from the keyStore
			PrivateKeyEntry entry = (PrivateKeyEntry) keystore.getEntry(keyStoreAlias, 
					new KeyStore.PasswordProtection(KeyStorePassword.toCharArray()));
			
			// Extract the public key from the keyStore
			PublicKey publicKey = entry.getCertificate().getPublicKey();

			// Inits a RSA Cipher and encrypts the privateKey with the given publicKey
			// Using Asymmetric. 
			Cipher keyEncryptionCipher = Cipher.getInstance(KEY_ENCRYPTION_ALGORITHM); 
			keyEncryptionCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			encryptedSecretKey = keyEncryptionCipher.doFinal(secretKey.getEncoded());
	}

	/***
	 * Encrypt the file using the cipher we created
	 * @param fileToEncrypt - input file
	 * @param encryptedFile - output file
	 * @return true if succeeded. False otherwise
	 * @throws IOException - IF we encountered an error with closing the streams
	 */
	private void Encrypt(File fileToEncrypt, File encryptedFile) throws IOException 
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

		} catch (IOException ex) {
			throw ex;
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			
			if (outputStream != null) {
				outputStream.close();
			}
		}
	}

	/**
	 * method creates the signature for the encrypted file
	 * 
	 * @param fileToEncrypt - file we encrypt
	 * @param keyStoreConfig - keystore info
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws UnrecoverableEntryException 
	 * @throws SignatureException 
	 * @throws NoSuchProviderException 
	 * @throws InvalidKeyException 
	 */
	private void generateDigitalSignatureForFile(File fileToEncrypt, String keyStoreAlias, String keyStorePassword ) throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, InvalidKeyException, NoSuchProviderException, SignatureException {

			//Read file's Bytes
			byte[] fileToEncryptBytes = FileToBytes(fileToEncrypt);

			if(fileToEncrypt == null) {
				throw new IllegalAccessError("The given file To Encrypt was empty");
			}

			//create a digest
			MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
			messageDigest.update(fileToEncryptBytes);


			// Get entry from keystore and create the private key for signature
			PrivateKeyEntry keyEntry = (PrivateKeyEntry) keystore.getEntry(keyStoreAlias, 
					new KeyStore.PasswordProtection(keyStorePassword.toCharArray()));
			PrivateKey privateKey = keyEntry.getPrivateKey();

			createSignature(privateKey, messageDigest.digest());
	}
	
	/**
	 * method signs the file
	 * 
	 * @param privateKey - private key we will use for signature
	 * @param fileDigest - file digest to encrypt
	 * @throws NoSuchAlgorithmException - could not find algorithm (will not get here)
	 * @throws NoSuchProviderException - could not find provider (will not get here)
	 * @throws InvalidKeyException - bad key (will not get here)
	 * @throws SignatureException
	 */
	private static void createSignature(PrivateKey privateKey, byte[] fileDigest) 
			throws NoSuchAlgorithmException, NoSuchProviderException,InvalidKeyException, SignatureException {

		Signature signatureGenerator = Signature.getInstance(SIGNATURE_ALGORITHM);
		signatureGenerator.initSign(privateKey);
		signatureGenerator.update(fileDigest);

		// sign the digest
		resultSignature = signatureGenerator.sign();
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
