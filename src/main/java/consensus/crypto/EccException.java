package consensus.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

public class EccException extends Exception {
    public EccException(NoSuchAlgorithmException cause) {
        super(cause);
    }
    public EccException(InvalidKeySpecException cause) {
        super(cause);
    }
    public EccException(InvalidKeyException cause) {
        super(cause);
    }
    public EccException(SignatureException cause) {
        super(cause);
    }
}
