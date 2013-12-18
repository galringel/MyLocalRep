import java.io.File;

/***
 * Class containing all info about the keystore the two programs will need
 * @author DanB
 *
 */
public class KeyStoreConfig 
{		
		KeyStoreConfig(File keyStoreFile, String keyStoreAlias, String keyStorePassword) {
			this.keyStoreFile = keyStoreFile;
			this.keyStoreAlias = keyStoreAlias; 
			this.keyStorePassword = keyStorePassword;
		}

		public File keyStoreFile;
		public String keyStoreAlias;
		public String keyStorePassword;
}