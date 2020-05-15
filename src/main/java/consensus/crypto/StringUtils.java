package consensus.crypto;

import com.google.gson.Gson;

public class StringUtils {
    public static String toHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static String toJson(Object object) {
        Gson gson = new Gson();
        return gson.toJson(object);
    }
}
