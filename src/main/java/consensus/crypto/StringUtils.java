package consensus.crypto;

import blockchain.model.BlockchainMessage;
import blockchain.model.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import consensus.net.data.Message;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;

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

    public static <T> T fromJson(String json, Type typeOfT) {
        Gson gson = new Gson();
        return gson.fromJson(json, typeOfT);
    }
}
