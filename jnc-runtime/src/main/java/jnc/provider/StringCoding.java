package jnc.provider;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static jnc.provider.CharsetUtil.getTerminatorLength;

@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
final class StringCoding {

    // current the platform we supported are all little endian
    private static final Charset JAVA_CHAR_CHARSET
            = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN
            ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;

    static boolean isNativeUTF16(Charset charset) {
        // deliberately NullPointerException if charset is null
        return charset.equals(JAVA_CHAR_CHARSET);
    }

    static void put(MemoryAccessor ma, int offset, byte[] bytes, int terminatorLength) {
        int length = bytes.length;
        switch (terminatorLength) {
            case 1:
                ma.putByte(offset + length, (byte) 0);
                break;
            case 2:
                ma.putShort(offset + length, (short) 0);
                break;
            case 4:
                ma.putInt(offset + length, 0);
                break;
            default:
                throw new IllegalArgumentException();
        }
        ma.putBytes(offset, bytes, 0, length);
    }

    static String get(MemoryAccessor ma, int offset, Charset charset, long limit) {
        int terminatorLength = getTerminatorLength(charset);
        // all charsets with terminatorLength greater than 1 will encode
        // any supported character to multiple of the terminatorLength
        int length = ma.getStringLength(offset, limit, terminatorLength);
        if (length == 0) {
            return "";
        }
        byte[] bytes = new byte[length * terminatorLength];
        ma.getBytes(offset, bytes, 0, bytes.length);
        return new String(bytes, charset);
    }

    static int getUTFLength(String string) {
        int len = string.length(), pos;
        label:
        {
            for (int i = 0; i < len; ++i) {
                char ch = string.charAt(i);
                if (((127 - ch) | (ch - 1)) < 0) {
                    pos = i;
                    break label;
                }
            }
            return len;
        }
        int ans = len;
        for (; pos < len; ++pos) {
            char ch = string.charAt(pos);
            if (((127 - ch) | (ch - 1)) < 0) {
                if (ch < 2048) {
                    ++ans;
                } else {
                    ans += 2;
                }
            }
        }
        return ans;
    }

    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    static byte[] toUTFBytes(String string, boolean terminated) {
        byte[] bytes = new byte[getUTFLength(string) + (terminated ? 1 : 0)];
        int offset = 0;
        for (int i = 0, len = string.length(); i < len; ++i) {
            char ch = string.charAt(i);
            if (ch < 128 && ch != 0) {
                bytes[offset++] = (byte) ch;
            } else {
                if (ch < 2048) {
                    bytes[offset++] = (byte) (ch >> 6 | 192);
                } else {
                    bytes[offset++] = (byte) (ch >> 12 | 224);
                    bytes[offset++] = (byte) (ch >> 6 & 63 | 128);
                }
                bytes[offset++] = (byte) (ch & 63 | 128);
            }
        }
        return bytes;
    }

}
