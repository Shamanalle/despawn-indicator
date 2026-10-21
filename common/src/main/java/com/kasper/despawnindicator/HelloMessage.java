package com.kasper.despawnindicator;

/**
 * Client to server, once after joining: the protocol the client speaks and its mod version. The server sends
 * despawn clocks only to players who said hello, so clients without the mod never get unknown packets.
 */
public final class HelloMessage {

    /** The protocol of this build. A server answers a newer client in its own protocol, which the client reads. */
    public static final int PROTOCOL = 1;
    private static final int MAX_VERSION_BYTES = 64;

    public final int protocol;
    public final String modVersion;

    public HelloMessage(int protocol, String modVersion) {
        this.protocol = protocol;
        this.modVersion = modVersion;
    }

    /** This client's hello. */
    public static HelloMessage ours() {
        return new HelloMessage(PROTOCOL, BuildInfo.version());
    }

    public byte[] encode() {
        return new Bytes.Writer().varInt(protocol).string(modVersion).toByteArray();
    }

    /** The hello in {@code data}, or null when it is not one. */
    public static HelloMessage decode(byte[] data) {
        try {
            Bytes.Reader in = new Bytes.Reader(data);
            int protocol = in.varInt();
            String version = in.hasMore() ? in.string(MAX_VERSION_BYTES) : "";
            return protocol > 0 ? new HelloMessage(protocol, version) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
