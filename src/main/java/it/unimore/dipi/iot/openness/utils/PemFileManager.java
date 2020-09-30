package it.unimore.dipi.iot.openness.utils;

import java.io.*;
import java.security.*;

import it.unimore.dipi.iot.openness.exception.PemFileManagerException;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
 
public class PemFileManager {

	public static final String PEM_TYPE_CERTIFICATE = "CERTIFICATE";

	private PemObject pemObject;

	public PemFileManager(Key key, String description) {
		this.pemObject = new PemObject(description, key.getEncoded());
	}
	
	public void write(String filename) throws FileNotFoundException, IOException {
		PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(filename)));
		try {
			pemWriter.writeObject(this.pemObject);
		} finally {
			pemWriter.close();
		}
	}

	public static void pemExport(String path, String pemType, byte[] bytes) throws PemFileManagerException {

		File file = new File(path);
		if (file.exists()) {
			if (!file.delete()) {
				throw new PemFileManagerException("deleted the file unsuccessfully");
			}
		} else {
			File dir = new File(file.getParent());
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					throw new PemFileManagerException("made dir failed in method newCA");
				}
			}
		}

		try {
			if (!file.createNewFile()) {
				throw new PemFileManagerException("created new file unsuccessfully");
			}
			PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(file)));
			PemObject pemObject = new PemObject(pemType, bytes);
			pemWriter.writeObject(pemObject);
			pemWriter.close();
		} catch (Exception e) {
			throw new PemFileManagerException("An error occurred on Util.pemExport :" + e.getMessage());
		}
	}


	/**
	 *
	 * @param privateKeyFile
	 * @param password
	 * @return
	 * @throws IOException
	 */
	public static KeyPair readKeyPair(String privateKeyFile, String password) throws IOException {

		// don't forget to add the provider
		Security.addProvider(new BouncyCastleProvider());

		// reads your key file
		PEMParser pemParser = new PEMParser(new FileReader(privateKeyFile));
		Object object = pemParser.readObject();
		JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

		KeyPair kp;

		if (object instanceof PEMEncryptedKeyPair) {
			// Encrypted key - we will use provided password
			PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) object;
			// uses the password to decrypt the key
			PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
			kp = converter.getKeyPair(ckp.decryptKeyPair(decProv));
		} else {
			// Unencrypted key - no password needed
			PEMKeyPair ukp = (PEMKeyPair) object;
			kp = converter.getKeyPair(ukp);
		}

		return kp;
	}

	public static PrivateKey loadPrivateKey(String keyPath) throws IOException, GeneralSecurityException {

		try (Reader reader = new FileReader(keyPath);

			 PEMParser parser = new PEMParser(reader)) {
			Object readObject;
			while ((readObject = parser.readObject()) != null) {
				if (readObject instanceof PEMKeyPair) {
					PEMKeyPair keyPair = (PEMKeyPair) readObject;
					return new JcaPEMKeyConverter().getPrivateKey(keyPair.getPrivateKeyInfo());
				} else if (readObject instanceof PrivateKeyInfo) {
					return new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) readObject);
				}
			}
		}

		throw new GeneralSecurityException("Cannot generate private key from file: " + keyPath);

	}

	public static PublicKey loadPublicKey(String keyPath) throws IOException, GeneralSecurityException {

		try{

			Reader reader = new FileReader(keyPath);
			PEMParser parser = new PEMParser(reader);

			Object readObject;

			while ((readObject = parser.readObject()) != null) {
				if (readObject instanceof SubjectPublicKeyInfo) {
					return new JcaPEMKeyConverter().getPublicKey((SubjectPublicKeyInfo) readObject);
				}
			}

			return null;
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

}