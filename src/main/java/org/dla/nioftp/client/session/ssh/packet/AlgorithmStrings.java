package org.dla.nioftp.client.session.ssh.packet;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p> Contains a static list of algorithm names which are sent to the server in a SSH_KEXDH_INIT packet. The server
 * then responds, in a SSH_KEXDH_INIT packet, with its list of algos, which is created by matching what we send vs. what
 * the server supports. At least that's what the RFC says. OpenSSH, the foundation for Linux sshd, does it the other way
 * around i.e. the server sends SSH_KEXDH_INIT before the clients </p> <p> There is a method in here to compare and
 * match the class static list of algos against what the server sends. The matches are then available through getters.
 * </p> <p> There are many more algorithms defined in RFC-4253 6.3 https://tools.ietf.org/html/rfc4253#section-6.3 Here,
 * I put just the 'Required" and "Recommended" names, with required being first in each list. My understanding of the
 * RFC is that the first algo in each list will be used. </p>
 *
 * @author Dennis Andress
 *
 *  Jan 25, 2013 11:44:26 AM
 */
public class AlgorithmStrings {
	private static final Logger logger = LoggerFactory.getLogger(AlgorithmStrings.class);

// key exchange
	final String kex_algorithms = "diffie-hellman-group1-sha1";
//	final String kex_algorithms = "diffie-hellman-group14-sha1,diffie-hellman-group1-sha1";
//	final String kex_algorithms = "diffie-hellman-group-exchange-sha256,diffie-hellman-group14-sha1,diffie-hellman-group1-sha1";
// Signature algorithms
//   The following public key and/or certificate formats are currently defined:
//   ssh-dss           REQUIRED     sign   Raw DSS Key
//   ssh-rsa           RECOMMENDED  sign   Raw RSA Key
	final String server_host_key_algorithms = "ssh-rsa,ssh-dss";
	final String encryption_algorithms_client_to_server = "3des-cbc,aes128-cbc,blowfish-cbc,aes192-cbc,aes256-cbc";
	final String encryption_algorithms_server_to_client = "3des-cbc,aes128-cbc,blowfish-cbc,aes192-cbc,aes256-cbc";
	final String mac_algorithms_client_to_server = "hmac-sha1,hmac-sha1-96,hmac-md5";
	final String mac_algorithms_server_to_client = "hmac-sha1,hmac-sha1-96,hmac-md5";
	final String compression_algorithms_client_to_server = "none";
	final String compression_algorithms_server_to_client = "none";
	final String languages_client_to_server = "";
	final String languages_server_to_client = "";
	String sv_kex_algorithms;
	String sv_server_host_key_algorithms;
	String sv_encryption_algorithms_client_to_server;
	String sv_encryption_algorithms_server_to_client;
	String sv_mac_algorithms_client_to_server;
	String sv_mac_algorithms_server_to_client;
	String sv_compression_algorithms_client_to_server;
	String sv_compression_algorithms_server_to_client;
	String sv_languages_client_to_server;
	String sv_languages_server_to_client;
	private String[] matchedKex;
	private String[] matchedKeyAlgos;
	private String[] matchedC2SCyphers;
	private String[] matchedS2CCyphers;
	private String[] matchedC2SMacs;
	private String[] matchedS2CMacs;
	private String[] matchedC2SComp;
	private String[] matchedS2CComp;
	private String[] matchedC2SLang;
	private String[] matchedS2CLang;

	public AlgorithmStrings() {
	}

	public void matchToReceivedAlgos() {
		matchedKex = matchArray(kex_algorithms.split(","), sv_kex_algorithms.split(","));
		matchedKeyAlgos = matchArray(server_host_key_algorithms.split(","), sv_server_host_key_algorithms.split(","));
		matchedC2SCyphers = matchArray(encryption_algorithms_client_to_server.split(","), sv_encryption_algorithms_client_to_server.split(","));
		matchedS2CCyphers = matchArray(encryption_algorithms_server_to_client.split(","), sv_encryption_algorithms_server_to_client.split(","));
		matchedC2SMacs = matchArray(mac_algorithms_client_to_server.split(","), sv_mac_algorithms_client_to_server.split(","));
		matchedS2CMacs = matchArray(mac_algorithms_server_to_client.split(","), sv_mac_algorithms_server_to_client.split(","));
		matchedC2SComp = matchArray(compression_algorithms_client_to_server.split(","), sv_compression_algorithms_client_to_server.split(","));
		matchedS2CComp = matchArray(compression_algorithms_server_to_client.split(","), sv_compression_algorithms_server_to_client.split(","));
		matchedC2SLang = matchArray(languages_client_to_server.split(","), sv_languages_client_to_server.split(","));
		matchedS2CLang = matchArray(languages_server_to_client.split(","), sv_languages_server_to_client.split(","));
		logger.trace("kex {}", a2s(matchedKex));
		logger.trace("c2s ciphers {}", a2s(matchedC2SCyphers));
		logger.trace("s2c ciphers {}", a2s(matchedS2CCyphers));
		logger.trace("c2s Macs {}", a2s(matchedC2SMacs));
		logger.trace("s2c Macs {}", a2s(matchedS2CMacs));
	}

	private String[] matchArray(final String[] client, final String[] server) {
		ArrayList<String> matches = new ArrayList<String>();
		for (String kx : client) {
			for (String svkex : server) {
				if (kx.equals(svkex))
					matches.add(kx);
			}
		}
		String[] t = new String[matches.size()];
		return matches.toArray(t);
	}

	public String getMatchedKex() {
		return a2s(matchedKex);
	}

	public String getMatchedKeyAlgos() {
		return a2s(matchedKeyAlgos);
	}

	public String getMatchedC2SCyphers() {
		return a2s(matchedC2SCyphers);
	}

	public String getMatchedS2CCyphers() {
		return a2s(matchedS2CCyphers);
	}

	public String getMatchedC2SMacs() {
		return a2s(matchedC2SMacs);
	}

	public String getMatchedS2CMacs() {
		return a2s(matchedS2CMacs);
	}

	public String getMatchedC2SComp() {
		return a2s(matchedC2SComp);
	}

	public String getMatchedS2CComp() {
		return a2s(matchedS2CComp);
	}

	public String getMatchedC2SLang() {
		return a2s(matchedC2SLang);
	}

	public String getMatchedS2CLang() {
		return a2s(matchedS2CLang);
	}

	private String a2s(final String[] array) {
		StringBuilder buf = new StringBuilder(array.length + 10);
		for (String s : array) {
			buf.append(s).append(",");
		}
		final int pos = buf.lastIndexOf(",");
		return buf.substring(0, pos);
	}
}
