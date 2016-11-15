package org.dla.nioftp.client.session.ssh.packet;

/**
 * Receives and sends a list of algorithms. Note that {@code unloadFreight} calls algos.matchToReceivedAlgos which
 * builds a list of matches between what the server sent and the static list in AlgorithmStrings.
 *
 * @author Dennis Andress
 *
 *  Jan 29, 2013 7:42:39 AM
 */
public class KexInitFreighter extends BaseFreighter implements Freight {

	byte cookie[];
	private AlgorithmStrings algos;

	public KexInitFreighter(Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
		fill(16);
		buffer.skip(16);
		putString(algos.kex_algorithms);
		putString(algos.server_host_key_algorithms);
		putString(algos.encryption_algorithms_client_to_server);
		putString(algos.encryption_algorithms_server_to_client);
		putString(algos.mac_algorithms_client_to_server);
		putString(algos.mac_algorithms_server_to_client);
		putString(algos.compression_algorithms_client_to_server);
		putString(algos.compression_algorithms_server_to_client);
		putString(algos.languages_client_to_server);
		putString(algos.languages_server_to_client);
		buffer.putByte((byte) 0);	// 1 byte of 0 for the boolean 'packet follows'
		buffer.putInt(0);  // 4 bytes of 0 as 'reserved'
	}

	@Override
	public void unloadFreight() throws PacketException {
		cookie = new byte[16];
		buffer.getByte(cookie);
		algos.sv_kex_algorithms = byteToString(buffer.getString());
		algos.sv_server_host_key_algorithms = byteToString(buffer.getString());
		algos.sv_encryption_algorithms_client_to_server = byteToString(buffer.getString());
		algos.sv_encryption_algorithms_server_to_client = byteToString(buffer.getString());
		algos.sv_mac_algorithms_client_to_server = byteToString(buffer.getString());
		algos.sv_mac_algorithms_server_to_client = byteToString(buffer.getString());
		algos.sv_compression_algorithms_client_to_server = byteToString(buffer.getString());
		algos.sv_compression_algorithms_server_to_client = byteToString(buffer.getString());
		algos.sv_languages_client_to_server = byteToString(buffer.getString());
		algos.sv_languages_server_to_client = byteToString(buffer.getString());

		algos.matchToReceivedAlgos();
	}

	private void print(String g, String[] array) {
		System.out.println("");
		System.out.println(g);
		for (String t : array) {
			System.out.println(t);
		}
	}

	public void setAlgorithmStrings(AlgorithmStrings algos) {
		this.algos = algos;
	}
}
