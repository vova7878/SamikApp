package com.v7878.samik;

import java.util.StringJoiner;

public class Utils {
    public static String hex(byte[] data) {
        var sb = new StringJoiner(" ");
        for (byte b : data) sb.add(String.format("%02X", b & 0xFF));
        return sb.toString();
    }
}
