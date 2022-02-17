package com.example.qranalyzer;


import java.io.UnsupportedEncodingException;
import java.util.Locale;

// 2021/09/14
public class QRDecoder {
    private static final int MODE_LENGTH = 4;
    private static final char[] ALPHANUMERIC_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:".toCharArray();

    private static int getLengthOfLength(String mode) {
        return getLengthOfLength(mode, 1);
    }

    private static int getLengthOfLength(String mode, int version) {
        switch (mode) {
            case "0001":
                if (1 <= version && version <= 9) {
                    return 10;
                }
                if (10 <= version && version <= 26) {
                    return 12;
                }
                if (27 <= version && version <= 40) {
                    return 14;
                }
                break;
            case "0010":
                if (1 <= version && version <= 9) {
                    return 9;
                }
                if (10 <= version && version <= 26) {
                    return 11;
                }
                if (27 <= version && version <= 40) {
                    return 13;
                }
                break;
            case "0100":
                if (1 <= version && version <= 9) {
                    return 8;
                }
                if (10 <= version && version <= 26) {
                    return 16;
                }
                if (27 <= version && version <= 40) {
                    return 16;
                }
                break;
            case "1000":
                if (1 <= version && version <= 9) {
                    return 8;
                }
                if (10 <= version && version <= 26) {
                    return 10;
                }
                if (27 <= version && version <= 40) {
                    return 12;
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        throw new IllegalArgumentException();
    }

    public static String decode(byte[] rawBytes) {
        String b = bin(rawBytes);
        int i = 0;
        StringBuilder content = new StringBuilder();
        while (i < b.length() - 8 * 2) {
            String mode = b.substring(i, i + MODE_LENGTH);
            i += MODE_LENGTH;
            if (mode.equals("0000")) {
                break;
            }
            int lengthOfLength = getLengthOfLength(mode);
            int len = Integer.parseInt(b.substring(i, i + lengthOfLength), 2);
            i += lengthOfLength;
            switch (mode) {
                case "0001":
                    for (int j = 0; j < len / 3; j++) {
                        int num = Integer.parseInt(b.substring(i, i + 10), 2);
                        i += 10;
                        content.append(String.format(Locale.US, "%03d", num));
                    }
                    if (len % 3 == 2) {
                        int num = Integer.parseInt(b.substring(i, i + 7), 2);
                        i += 7;
                        content.append(String.format(Locale.US, "%02d", num));
                    }
                    if (len % 3 == 1) {
                        int num = Integer.parseInt(b.substring(i, i + 4), 2);
                        i += 4;
                        content.append(String.format(Locale.US, "%01d", num));
                    }
                    break;
                case "0010":
                    for (int j = 0; j < len / 2; j++) {
                        int alphanum = Integer.parseInt(b.substring(i, i + 11), 2);
                        i += 11;
                        int upper = alphanum / 45;
                        int lower = alphanum % 45;
                        content.append(ALPHANUMERIC_CHARS[upper]);
                        content.append(ALPHANUMERIC_CHARS[lower]);
                    }
                    if (len % 2 == 1) {
                        int alphanum = Integer.parseInt(b.substring(i, i + 6), 2);
                        i += 6;
                        content.append(ALPHANUMERIC_CHARS[alphanum]);
                    }
                    break;
                case "0100":
                    byte[] buf = new byte[len];
                    for (int j = 0; j < len; j++) {
                        buf[j] = (byte) Integer.parseInt(b.substring(i, i + 8), 2);
                        i += 8;
                    }
                    try {
                        content.append(new String(buf, "Shift-JIS"));
                    } catch (UnsupportedEncodingException e) {
                        /* DO NOTHING */
                    }
                    break;
                case "1000":
                    for (int j = 0; j < len; j++) {
                        int kanjinum = Integer.parseInt(b.substring(i, i + 13), 2);
                        i += 13;
                        int upper = kanjinum / 0xC0;
                        int lower = kanjinum % 0xC0;
                        int ik = upper * 0x100 + lower;
                        if (upper <= 0x1E) {
                            ik += 0x8140;
                        } else if (upper <= 0x2A) {
                            ik += 0xC140;
                        } else {
                            // content.append("�");
                            throw new IllegalStateException();
                        }
                        byte[] bk = new byte[2];
                        bk[0] = (byte) (ik / 0x100);
                        bk[1] = (byte) (ik % 0x100);
                        try {
                            content.append(new String(bk, "Shift-JIS"));
                        } catch (UnsupportedEncodingException e) {
                            /* DO NOTHING */
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("");
            }
        }
        return content.toString();
    }

    private static String byte2bin(byte b) {
        int i = b & 0xFF;
        int[] r = new int[8];
        for (int j = 8 - 1; j >= 0; j--) {
            r[j] = i % 2;
            i >>= 1;
        }
        StringBuilder ret = new StringBuilder();
        for (int j = 0; j < 8; j++) {
            ret.append(String.format(Locale.US, "%d", r[j]));
        }
        return ret.toString();
    }

    private static String bin(byte[] rawBytes) {
        StringBuilder ret = new StringBuilder();
        for (byte b : rawBytes) {
            ret.append(byte2bin(b));
        }
        return ret.toString();
    }
}