package socs.network.util;

import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFPacketType;

import java.util.regex.Pattern;

/**
 * Created by ericschaal on 2017-02-28.
 */
public class Utility {

    private static final Pattern PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    public static boolean validateIP(final String ip) {
        return PATTERN.matcher(ip).matches();
    }

    public static SOSPFPacketType getSOSPFPacketType(SOSPFPacket packet) {
        if (packet.sospfType == 0) return SOSPFPacketType.HELLO;
        else if (packet.sospfType == 1) return SOSPFPacketType.LSUPDATE;
        else return SOSPFPacketType.UNKNOWN;
    }

}
