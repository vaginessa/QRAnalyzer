package tech.oliet.qranalyzer;


import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class QRDecoder {
    private final byte[] rawBytes;
    private final int version;
    private String hexContents = "";
    private boolean hasResidualData = false;
    private String residualData = "";
    private String hiddenData = "";
    private int end_index = -1;
    private static final int MODE_LENGTH = 4;
    private static final char[] ALPHANUMERIC_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:".toCharArray();

    public QRDecoder(byte[] rawBytes, int version) {
        this.rawBytes = rawBytes;
        this.version = version;
    }

    public QRDecoder(byte[] rawBytes, String errorCorrectionLevel) {
        this(rawBytes, calculateVersion(rawBytes.length * 8, errorCorrectionLevel));
    }

    private static int calculateVersion(int dataBitLength, String errorCorrectionLevel) throws IllegalArgumentException {
        int ecl;
        switch (errorCorrectionLevel) {
            case "L":
                ecl = 0;
                break;
            case "M":
                ecl = 1;
                break;
            case "Q":
                ecl = 2;
                break;
            case "H":
                ecl = 3;
                break;
            default:
                throw new IllegalArgumentException("Invalid Error Correction Level");
        }

        // https://www.qrcode.com/about/version.html
        final int[][] dataBitLengthMap = {
                {-1, -1, -1, -1},
                {152, 128, 104, 72},
                {272, 224, 176, 128},
                {440, 352, 272, 208},
                {640, 512, 384, 288},
                {864, 688, 496, 368},
                {1088, 864, 608, 480},
                {1248, 992, 704, 528},
                {1552, 1232, 880, 688},
                {1856, 1456, 1056, 800},
                {2192, 1728, 1232, 976},
                {2592, 2032, 1440, 1120},
                {2960, 2320, 1648, 1264},
                {3424, 2672, 1952, 1440},
                {3688, 2920, 2088, 1576},
                {4184, 3320, 2360, 1784},
                {4712, 3624, 2600, 2024},
                {5176, 4056, 2936, 2264},
                {5768, 4504, 3176, 2504},
                {6360, 5016, 3560, 2728},
                {6888, 5352, 3880, 3080},
                {7456, 5712, 4096, 3248},
                {8048, 6256, 4544, 3536},
                {8752, 6880, 4912, 3712},
                {9392, 7312, 5312, 4112},
                {10208, 8000, 5744, 4304},
                {10960, 8496, 6032, 4768},
                {11744, 9024, 6464, 5024},
                {12248, 9544, 6968, 5288},
                {13048, 10136, 7288, 5608},
                {13880, 10984, 7880, 5960},
                {14744, 11640, 8264, 6344},
                {15640, 12328, 8920, 6760},
                {16568, 13048, 9368, 7208},
                {17528, 13800, 9848, 7688},
                {18448, 14496, 10288, 7888},
                {19472, 15312, 10832, 8432},
                {20528, 15936, 11408, 8768},
                {21616, 16816, 12016, 9136},
                {22496, 17728, 12656, 9776},
                {23648, 18672, 13328, 10208},
        };

        for (int version = 1; version <= 40; version++) {
            if (dataBitLengthMap[version][ecl] == dataBitLength) {
                return version;
            }
        }

        throw new IllegalArgumentException("invalid data bit length");
    }

    public int calculateCellSize() {
        return 21 + (this.version - 1) * 4;
    }

    public int getVersion() {
        return this.version;
    }

    public String getHexContents() {
        return this.hexContents;
    }

    public boolean getHasResidualData() {
        return this.hasResidualData;
    }

    public String getResidualData() {
        return this.residualData;
    }

    public boolean getHasHiddenData() {
        return !this.hiddenData.equals("");
    }

    public String getHiddenData() {
        return this.hiddenData;
    }

    public int getEndIndex() {
        return this.end_index;
    }

    private int getLengthOfLength(String mode) {
        return getLengthOfLength(mode, this.version);
    }

    private static int getLengthOfLength(String mode, int version) throws IllegalArgumentException {
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
                throw new IllegalArgumentException("Invalid Mode");
        }
        throw new IllegalArgumentException("Invalid Version");
    }

    public String decode() {
        try {
            return this._decode();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String _decode() throws Exception {
        String b = this.bin();
        int i = 0;
        StringBuilder contents = new StringBuilder();
        StringBuilder hexContents = new StringBuilder();
        StringBuilder residualData = new StringBuilder();
        while (i < b.length() - 8 * 2) {
            String mode = b.substring(i, i + MODE_LENGTH);
            i += MODE_LENGTH;
            if (mode.equals("0000")) {
                break;
            }
            int lengthOfLength = this.getLengthOfLength(mode);
            int len = Integer.parseInt(b.substring(i, i + lengthOfLength), 2);
            i += lengthOfLength;
            switch (mode) {
                case "0001": // Number
                    for (int j = 0; j < len / 3; j++) {
                        int num = Integer.parseInt(b.substring(i, i + 10), 2);
                        i += 10;
                        contents.append(String.format(Locale.US, "%03d", num));
                    }
                    if (len % 3 == 2) {
                        int num = Integer.parseInt(b.substring(i, i + 7), 2);
                        i += 7;
                        contents.append(String.format(Locale.US, "%02d", num));
                    }
                    if (len % 3 == 1) {
                        int num = Integer.parseInt(b.substring(i, i + 4), 2);
                        i += 4;
                        contents.append(String.format(Locale.US, "%01d", num));
                    }
                    break;
                case "0010": // AlphaNum
                    for (int j = 0; j < len / 2; j++) {
                        int alphanum = Integer.parseInt(b.substring(i, i + 11), 2);
                        i += 11;
                        int upper = alphanum / 45;
                        int lower = alphanum % 45;
                        contents.append(ALPHANUMERIC_CHARS[upper]);
                        contents.append(ALPHANUMERIC_CHARS[lower]);
                    }
                    if (len % 2 == 1) {
                        int alphanum = Integer.parseInt(b.substring(i, i + 6), 2);
                        i += 6;
                        contents.append(ALPHANUMERIC_CHARS[alphanum]);
                    }
                    break;
                case "0100": // 8-bit
                    byte[] buf = new byte[len];
                    for (int j = 0; j < len; j++) {
                        buf[j] = (byte) Integer.parseInt(b.substring(i, i + 8), 2);
                        i += 8;
                        String h = String.format(Locale.US, "%02x", buf[j]);
                        hexContents.append(h);
                        if (j > 0 && buf[j - 1] == (byte) 0x00) {
                            hasResidualData = true;
                        }
                        if (hasResidualData) {
                            residualData.append(h);
                        }
                    }
                    try {
                        contents.append(new String(buf, "Shift-JIS"));

                    } catch (UnsupportedEncodingException e) {
                        /* DO NOTHING */
                    }
                    break;
                case "1000": // Kanji
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
                            throw new IllegalStateException("Invalid Value in Kanji Mode");
                        }
                        byte[] bk = new byte[2];
                        bk[0] = (byte) (ik / 0x100);
                        bk[1] = (byte) (ik % 0x100);
                        try {
                            contents.append(new String(bk, "Shift-JIS"));
                        } catch (UnsupportedEncodingException e) {
                            /* DO NOTHING */
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Invalid Mode");
            }
        }

        while (i % 8 != 0) {
            i++;
        }
        int i_h = i / 8;

        int j_h = rawBytes.length;
        j_h--;
        boolean flag = rawBytes[j_h] == (byte) 0x11;
        if (flag || rawBytes[j_h] == (byte) 0xec) {
            j_h--;
            while (j_h > 0) {
                if (flag) {
                    if (rawBytes[j_h] != (byte) 0xec) {
                        break;
                    }
                } else {
                    if (rawBytes[j_h] != (byte) 0x11) {
                        break;
                    }
                }
                flag = !flag;
                j_h--;
            }
        }

        StringBuilder hiddenData = new StringBuilder();
        for (; i_h <= j_h; i_h++) {
            String h = String.format(Locale.US, "%02x", this.rawBytes[i_h]);
            hiddenData.append(h);
        }

        // all data 8-bit
        if (b.startsWith("0100")) {
            this.hexContents = hexContents.toString();
        }

        this.residualData = residualData.toString();

        this.hiddenData = hiddenData.toString();

        end_index = i / 8;

        return contents.toString();
    }

    private static String byte2bin(byte b) {
        return String.format(Locale.US, "%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }

    private String bin() {
        StringBuilder ret = new StringBuilder();
        for (byte b : this.rawBytes) {
            ret.append(byte2bin(b));
        }
        return ret.toString();
    }
}