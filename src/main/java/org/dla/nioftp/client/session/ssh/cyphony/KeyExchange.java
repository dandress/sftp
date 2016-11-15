package org.dla.nioftp.client.session.ssh.cyphony;

import org.dla.nioftp.client.session.ssh.packet.PacketException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client generates a random number
 * <pre>  x (1 <; x < q) and computes e = g^x mod p.</pre> Client sends e to Server.
 *
 *
 * @author Dennis Andress
 *
 *  Jan 30, 2013 12:07:52 PM
 */
public class KeyExchange {

	private static final Logger logger = LoggerFactory.getLogger(KeyExchange.class);
	static final byte[] _g = {2};
	static final byte[] _p = {
		(byte) 0x00,
		(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
		(byte) 0xC9, (byte) 0x0F, (byte) 0xDA, (byte) 0xA2, (byte) 0x21, (byte) 0x68, (byte) 0xC2, (byte) 0x34,
		(byte) 0xC4, (byte) 0xC6, (byte) 0x62, (byte) 0x8B, (byte) 0x80, (byte) 0xDC, (byte) 0x1C, (byte) 0xD1,
		(byte) 0x29, (byte) 0x02, (byte) 0x4E, (byte) 0x08, (byte) 0x8A, (byte) 0x67, (byte) 0xCC, (byte) 0x74,
		(byte) 0x02, (byte) 0x0B, (byte) 0xBE, (byte) 0xA6, (byte) 0x3B, (byte) 0x13, (byte) 0x9B, (byte) 0x22,
		(byte) 0x51, (byte) 0x4A, (byte) 0x08, (byte) 0x79, (byte) 0x8E, (byte) 0x34, (byte) 0x04, (byte) 0xDD,
		(byte) 0xEF, (byte) 0x95, (byte) 0x19, (byte) 0xB3, (byte) 0xCD, (byte) 0x3A, (byte) 0x43, (byte) 0x1B,
		(byte) 0x30, (byte) 0x2B, (byte) 0x0A, (byte) 0x6D, (byte) 0xF2, (byte) 0x5F, (byte) 0x14, (byte) 0x37,
		(byte) 0x4F, (byte) 0xE1, (byte) 0x35, (byte) 0x6D, (byte) 0x6D, (byte) 0x51, (byte) 0xC2, (byte) 0x45,
		(byte) 0xE4, (byte) 0x85, (byte) 0xB5, (byte) 0x76, (byte) 0x62, (byte) 0x5E, (byte) 0x7E, (byte) 0xC6,
		(byte) 0xF4, (byte) 0x4C, (byte) 0x42, (byte) 0xE9, (byte) 0xA6, (byte) 0x37, (byte) 0xED, (byte) 0x6B,
		(byte) 0x0B, (byte) 0xFF, (byte) 0x5C, (byte) 0xB6, (byte) 0xF4, (byte) 0x06, (byte) 0xB7, (byte) 0xED,
		(byte) 0xEE, (byte) 0x38, (byte) 0x6B, (byte) 0xFB, (byte) 0x5A, (byte) 0x89, (byte) 0x9F, (byte) 0xA5,
		(byte) 0xAE, (byte) 0x9F, (byte) 0x24, (byte) 0x11, (byte) 0x7C, (byte) 0x4B, (byte) 0x1F, (byte) 0xE6,
		(byte) 0x49, (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xEC, (byte) 0xE6, (byte) 0x53, (byte) 0x81,
		(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	};
	private MessageDigest messageDigest;
	private Signature signature;
	private KeyFactory keyFactory;
	private KeyAgreement keyAgreement;
	private KeyPairGenerator keyPairGen;
	private BigInteger e;  // my public key
	private byte[] e_array;
	/**
	 * Server's version string (CR and NL excluded)
	 */
	private byte[] V_S;
	/**
	 * Client's version string (CR and NL excluded)
	 */
	private byte[] V_C;
	/**
	 * Payload (Freight) of the server's SSH_MSG_KEXINIT
	 */
	private byte[] I_S;
	/**
	 * Payload (Freight) of the client's SSH_MSG_KEXINIT
	 */
	private byte[] I_C;
	private byte[] sig_of_H = null;
	private BigInteger K;
	private byte[] k_array = null;
	private byte[] H = null;
	/**
	 * K_S is server_key_blob, which includes .... string ssh-dss impint p of dsa impint q of dsa impint g of dsa impint
	 * pub_key of dsa
	 */
	private byte[] K_S;
	private BigInteger p;
	private BigInteger g;
	private BigInteger f = null;

	/*
	 The hash H is computed as the HASH hash of the concatenation of the
	 following:
	 * string    V_C, the client's version string (CR and NL excluded)
	 * string    V_S, the server's version string (CR and NL excluded)
	 * string    I_C, the payload of the client's SSH_MSG_KEXINIT
	 * string    I_S, the payload of the server's SSH_MSG_KEXINIT
	 * string    K_S, the host key
	 * mpint     e, exchange value sent by the client
	 * mpint     f, exchange value sent by the server
	 * mpint     K, the shared secret
	 * This value is called the exchange hash, and it is used to authenticate the key exchange.
	 */
	/**
	 *
	 * @param serverVersionId
	 * @param ourVersionId
	 * @param serverFreight
	 * @param ourFreight
	 */
	public KeyExchange(final String serverVersionId, final String ourVersionId, final byte[] serverFreight, final byte[] ourFreight) throws PacketException {
		this.V_S = serverVersionId.getBytes();
		this.V_C = ourVersionId.getBytes();

		this.I_S = serverFreight;
		this.I_C = ourFreight;
		p = new BigInteger(_p);
		g = new BigInteger(_g);

		try {
			keyAgreement = KeyAgreement.getInstance("DiffieHellman");
			keyPairGen = KeyPairGenerator.getInstance("DiffieHellman");
		} catch (NoSuchAlgorithmException ex) {
			throw new PacketException(ex);
		}
	}

	public void clear() {
		messageDigest = null;
		signature = null;
		keyFactory = null;
		keyAgreement = null;
		keyPairGen = null;
		K = null;
		e = null;
		p = null;
		g = null;
		f = null;
	}

	public byte[] getE() throws PacketException {
		try {
			DHParameterSpec dps = new DHParameterSpec(p, g);
			keyPairGen.initialize(dps);
			KeyPair kp = keyPairGen.generateKeyPair();
			keyAgreement.init(kp.getPrivate());
			byte[] publicKey = kp.getPublic().getEncoded();
			e = ((DHPublicKey) kp.getPublic()).getY();
			e_array = e.toByteArray();
			return e_array;
		} catch (InvalidKeyException ex) {
			throw new PacketException(ex.getMessage(), ex);
		} catch (InvalidAlgorithmParameterException ex) {
			throw new PacketException(ex.getMessage(), ex);
		}
	}

	private void makeK() throws PacketException {
		try {
			KeyFactory myKeyFac = KeyFactory.getInstance("DH");
			DHPublicKeySpec keySpec = new DHPublicKeySpec(f, p, g);
			PublicKey yourPubKey = myKeyFac.generatePublic(keySpec);
			keyAgreement.doPhase(yourPubKey, true);
			byte[] sharedSecret = keyAgreement.generateSecret();
			K = new BigInteger(sharedSecret);
			k_array = sharedSecret;
			logger.trace("Shared secret first byte (hex): {}. K first byte (hex): {}. K sign (-1, 0 or 1): {}",
					Integer.toHexString(sharedSecret[0] & 0xff), Integer.toHexString(k_array[0] & 0xff), K.signum());
		} catch (InvalidKeyException ex) {
			throw new PacketException(ex.getMessage(), ex);
		} catch (InvalidKeySpecException ex) {
			throw new PacketException(ex.getMessage(), ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new PacketException(ex.getMessage(), ex);
		}
	}

	public void setKS(final byte[] k_s) {
		this.K_S = k_s;
//		dump(k_s);
	}

	public void setF(byte[] _f) {
		f = new BigInteger(_f);
//		dump(_f);
	}

	public int getBlockSize() {
		return 20;
	}

	public boolean verifyKeys() throws PacketException {
		boolean rslt = false;
		try {
			messageDigest = MessageDigest.getInstance("SHA-1");
			assert f != null;
			makeK();
			makeH();
			processK_S();

			ByteBuffer sig = ByteBuffer.wrap(sig_of_H);
			int i = sig.getInt(); // seems to be the offset of the key from 0
			byte[] q = new byte[i];
			sig.get(q, 0, i);		// returns 'ssh-rsa'
			int len = sig.getInt();
			byte[] foo = new byte[len];
			sig.get(foo, 0, foo.length);
			rslt = signature.verify(foo);
			logger.debug("KEX Hash verifaction: {}", rslt);
		} catch (SignatureException ex) {
			throw new PacketException(ex.getMessage(), ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new PacketException(ex.getMessage(), ex);
		}
		return rslt;
	}

	private void makeH() {
		final byte[] f_array = f.toByteArray();
		Buffer buf = new Buffer(1024 * 10 * 2);
		buf.putString(V_C);
		buf.putString(V_S);
		buf.putString(I_C);
		buf.putString(I_S);
		buf.putString(K_S);
		buf.putMPInt(e_array);
		buf.putMPInt(f_array);
		buf.putMPInt(k_array);
		byte[] foo = new byte[buf.getLength()];
		buf.getByte(foo, 0, foo.length);
		messageDigest.update(foo, 0, foo.length);
		H = messageDigest.digest();
	}

	private void processK_S() throws PacketException {
		try {
			ByteBuffer buf = ByteBuffer.wrap(K_S);
			int len = buf.getInt();
			byte[] s = new byte[len];
			buf.get(s, 0, len);
			final String alg = byteToString(s, 0, s.length);
			logger.info("KeyExchange using {}", alg);
			if (alg.equals("ssh-rsa")) {
				signature = java.security.Signature.getInstance("SHA1withRSA");
				keyFactory = KeyFactory.getInstance("RSA");

				len = buf.getInt();
				byte[] ee = new byte[len];  // p
				buf.get(ee, 0, len);

				len = buf.getInt();
				byte[] n = new byte[len]; // q
				buf.get(n, 0, len);

				RSAPublicKeySpec rsaPubKeySpec = new RSAPublicKeySpec(new BigInteger(n), new BigInteger(ee));
				PublicKey pubKey = keyFactory.generatePublic(rsaPubKeySpec);
				signature.initVerify(pubKey);
				signature.update(H);
//TODO finish ssh-dss
			} else {
			}
		} catch (NoSuchAlgorithmException ex) {
			throw new PacketException(ex.getMessage(), ex);
		} catch (InvalidKeyException ex) {
			throw new PacketException(ex.getMessage(), ex);
		} catch (SignatureException ex) {
			throw new PacketException(ex.getMessage(), ex);
		} catch (InvalidKeySpecException ex) {
			throw new PacketException(ex.getMessage(), ex);
		}

	}

	protected String byteToString(final byte[] bytes, int begin, int length) throws PacketException {
		try {
			return new String(bytes, begin, length, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			throw new PacketException(ex);
		}
	}

	public void setSig_of_H(byte[] sig_of_H) {
		this.sig_of_H = sig_of_H;
	}

	private void dump(byte[] foo) {
		for (int i = 0; i < foo.length; i++) {
			if ((foo[i] & 0xf0) == 0)
				System.err.print("0");
			System.err.print(Integer.toHexString(foo[i] & 0xff));
			if (i % 16 == 15) {
				System.err.println("");
				continue;
			}
			if (i % 2 == 1)
				System.err.print(" ");
		}
	}

	public byte[] getH() {
		return H;
	}

	byte[] getK() {
		return k_array;
	}

}
