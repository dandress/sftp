package org.dla.nioftp.client.session.ssh.cyphony;

import org.dla.nioftp.client.session.ssh.packet.AlgorithmStrings;
import org.dla.nioftp.client.session.ssh.packet.PacketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 *  Feb 4, 2013 1:42:09 PM
 */
public class PacketAlgorithms {

	private static final Logger logger = LoggerFactory.getLogger(PacketAlgorithms.class);
	private byte[] session_id;
	private ICipher s2ccipher;
	private ICipher c2scipher;
	private IMac s2cmac;
	private IMac c2smac;

	private byte[] H;
	private byte[] K;
	private byte[] IVc2s;
	private byte[] IVs2c;
	private byte[] keyC2s;
	private byte[] keyS2c;
	private byte[] MacC2s;
	private byte[] MacS2c;
	private AlgorithmStrings algos;
	private MessageDigest messageDigest;
	private Buffer buffer;
	private StringBuilder logOutput = new StringBuilder();

	public PacketAlgorithms(KeyExchange keyExchange, AlgorithmStrings algos) {
		this.algos = algos;

		H = keyExchange.getH();
		K = keyExchange.getK();
		session_id = new byte[H.length];
		System.arraycopy(H, 0, session_id, 0, H.length);
		buffer = new Buffer(50000);
	}

	public void init() throws PacketException {
		try {
			messageDigest = MessageDigest.getInstance("SHA-1");

			/**
			 * Key	Calculation
			 * client to server initial IV	h(K || H || "A" | | SID)
			 * server to client initial IV	h(K || H || "B" | | SID)
			 * client to server encryption	h(K || H || "C" | | SID)
			 * server to client encryption	h(K || H || "D" | | SID)
			 * client to server integrity	h(K || H || "E" | | SID)
			 * server to client integrity	h(K || H || "F" | | SID)
			 * <p/>
			 * Where:
			 * K = shared secret
			 * H = exchange hash
			 * SID = session ID
			 */
			buffer.reset();
			buffer.putMPInt(K);
			buffer.putByte(H);
			buffer.putByte((byte) 0x41);  // set this byte to 'A'
			buffer.putByte(session_id);
			messageDigest.update(buffer.buffer, 0, buffer.index);
			IVc2s = messageDigest.digest();

			int j = buffer.index - session_id.length - 1;		// this is the byte set to 'A' above
			buffer.buffer[j]++;		// increment 'A' to 'B'
			messageDigest.update(buffer.buffer, 0, buffer.index);
			IVs2c = messageDigest.digest();
			buffer.buffer[j]++;		// 'C'
			messageDigest.update(buffer.buffer, 0, buffer.index);
			keyC2s = messageDigest.digest();
			buffer.buffer[j]++;		// 'D'
			messageDigest.update(buffer.buffer, 0, buffer.index);
			keyS2c = messageDigest.digest();
			buffer.buffer[j]++;		// 'E'
			messageDigest.update(buffer.buffer, 0, buffer.index);
			MacC2s = messageDigest.digest();
			buffer.buffer[j]++;		// 'F'
			messageDigest.update(buffer.buffer, 0, buffer.index);
			MacS2c = messageDigest.digest();

			c2scipher = makeCipher(false, algos.getMatchedC2SCyphers(), keyC2s, IVc2s);
			s2ccipher = makeCipher(true, algos.getMatchedS2CCyphers(), keyS2c, IVs2c);
			c2smac = makeMac(false, algos.getMatchedC2SMacs(), MacC2s);
			s2cmac = makeMac(true, algos.getMatchedS2CMacs(), MacS2c);
			logger.info(logOutput.toString().trim());
		} catch (NoSuchAlgorithmException ex) {
			throw new PacketException(ex.getMessage(), ex);
		}
	}

	public void clear() {
		s2ccipher = null;
		c2scipher = null;
		s2cmac = null;
		c2smac = null;
		messageDigest = null;
		buffer = null;
	}

	private ICipher makeCipher(boolean decrypt, final String initString, byte[] key, byte[] iv) throws PacketException {
		String[] cyParams = splitInitString(initString);
		ICipher cipher = null;
		if (decrypt)
			logOutput.append("s2ccipher: ").append(cyParams[0]).append(" ");
		else
			logOutput.append("c2scipher: ").append(cyParams[0]).append(" ");
		final AlgoParams p = transposeAlgoNames(cyParams[0], cyParams[1]);
		cipher = new BaseCipher(decrypt, key, iv, H, K, messageDigest, p.blockSize, p.ivSize, p.cbc);
		cipher.init(p.algorithm, cyParams[1], p.padding);
		return cipher;
	}

	private IMac makeMac(boolean serverToClient, final String initString, byte[] key) throws PacketException {
		IMac mac = null;
		String[] cyParams = splitInitString(initString);
		// all Mac's begin with 'hmac,' so it's the second part that we need here...
		final MacParams m = transposeMacNames(cyParams);
		if (serverToClient)
			logOutput.append("s2cMac: ").append(m.algorithm).append(" ");
		else
			logOutput.append("c2sMac: ").append(m.algorithm).append(" ");
		mac = new BaseMac(m.blockSize, K, H, key, messageDigest, m.algorithm, m.name);
		return mac;
	}

	private MacParams transposeMacNames(final String[] cyParams) {
		final String macAlgo = cyParams[1];
		String suffix = null;
		if (cyParams.length == 3)		// some init strings have two dashes (-). This to catch the third piece
			suffix = cyParams[2];

		MacParams params = new MacParams();
		if (macAlgo.equalsIgnoreCase("sha1")) {
			if (suffix == null) {
				params.name = "hmac-sha1";
				params.blockSize = 20;
				params.algorithm = "HmacSHA1";
				return params;
			} else if (suffix.equals("96")) {
				params.name = "hmac-sha1-96";
				params.blockSize = 12;
				params.algorithm = "HmacSHA1";
				return params;
			}
		} else if (macAlgo.equalsIgnoreCase("sha2")) {
			if (suffix.equals("256")) {
				params.name = "hmac-sha2-256";
				params.blockSize = 32;
				params.algorithm = "HmacSHA2556";
				return params;
			} else if (suffix.equals("512")) {
				params.name = "hmac-sha1-512";
				params.blockSize = 64;
				params.algorithm = "HmacSHA512";
				return params;
			}
		} else if (macAlgo.equalsIgnoreCase("md5")) {
			params.name = "hmac-md5";
			params.blockSize = 16;
			params.algorithm = "HmacMD5";
			return params;
		} else if (macAlgo.equalsIgnoreCase("")) {
		}
		return null;
	}

	/**
	 * Thanks to the design of JCA, the different encryption algorithms can
	 * be created in using the same code. Only the the String name of the algorithm
	 * needs to change. This translates the algo name exchanged between the server
	 * and client into a name used by JCA. It also sets a few values that are
	 * specific to this usage i.e. blockSize, ivSize, cbc.
	 *
	 * @param sshAlgo
	 * @param transformation The part to the right of the daseh (-) in the init string.
	 *                          Used to set isCbc.
	 * @return
	 */
	private AlgoParams transposeAlgoNames(final String sshAlgo, final String transformation) {
		AlgoParams params = new AlgoParams();
		final boolean isCbc = transformation.equalsIgnoreCase("cbc") ? true : false;
		if (sshAlgo.equalsIgnoreCase("3des")) {
			params.algorithm = "DESede";
			params.blockSize = 24;
			params.ivSize = 8;
			params.cbc = isCbc;
//			params.padding = <using default>;
			return params;
		} else if (sshAlgo.equalsIgnoreCase("aes128")) {
			params.algorithm = "AES";
			params.blockSize = 16;
			params.ivSize = 16;
			params.cbc = isCbc;
//			params.padding = <using default>;
			return params;
		} else if (sshAlgo.equalsIgnoreCase("aes192")) {
			params.algorithm = "AES";
			params.blockSize = 24;
			params.ivSize = 16;
			params.cbc = isCbc;
//			params.padding = <using default>;
			return params;
		} else if (sshAlgo.equalsIgnoreCase("aes256")) {
			params.algorithm = "AES";
			params.blockSize = 32;
			params.ivSize = 16;
			params.cbc = isCbc;
//			params.padding = <using default>;
			return params;
		} else if (sshAlgo.equalsIgnoreCase("arcfour256")) {
			params.algorithm = "RC4";
			params.blockSize = 16;
			params.ivSize = 8;
			params.cbc = isCbc;
//			params.padding = <using default>;
			return params;
		} else if (sshAlgo.equalsIgnoreCase("blowfish")) {
			params.algorithm = "BLOWFISH";
			params.blockSize = 16;
			params.ivSize = 8;
			params.cbc = isCbc;
//			params.padding = <using default>;
			return params;
		} else if (sshAlgo.equalsIgnoreCase("")) {
		}
		return null;
	}

	/** Splits the coma (,) separated string into one array.
	 * Then takes the first element of that array (as I believe the spec calls for)
	 * and splits it into another array. Ensures that this second array is at least two elements long
	 * and then returns it.
	 *
	 * @param initString
	 * @return
	 */
	private String[] splitInitString(String initString) {
		final String[] initArray = initString.split(",");
		String[] algo = initArray[0].split("-");
		if (algo.length == 1) { // There was no "-", so there is only one element in the array
			final String s = algo[0];
			algo = new String[2];
			algo[0] = s;
			algo[1] = "";
		}
		return algo;
	}

	public byte[] getSession_id() {
		return session_id;
	}

	public ICipher getS2ccipher() {
		return s2ccipher;
	}

	public ICipher getC2scipher() {
		return c2scipher;
	}

	public IMac getS2cmac() {
		return s2cmac;
	}

	public IMac getC2smac() {
		return c2smac;
	}

	class AlgoParams {
		int blockSize;
		int ivSize;
		boolean cbc;
		String algorithm;
		String padding = "NoPadding";
	}

	class MacParams {
		int blockSize;
		String algorithm;
		String name;
	}
}
