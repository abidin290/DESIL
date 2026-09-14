package id.desa.dokumentasirumah;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;

final class AccessStore {
    private final SharedPreferences prefs;
    AccessStore(Context c){prefs=c.getSharedPreferences("central_access",Context.MODE_PRIVATE);}
    private javax.crypto.SecretKey key()throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias("central-access")){KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("central-access",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
        return (javax.crypto.SecretKey)ks.getKey("central-access",null);
    }
    String get(String field){return prefs.getString(field,"");}
    String code()throws Exception{
        if(get("cipher").isEmpty())return "";
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(get("iv"),Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(get("cipher"),Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    void save(String url,String code,String id,String worker)throws Exception{
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());byte[] encrypted=cipher.doFinal(code.getBytes(StandardCharsets.UTF_8));
        if(!prefs.edit().putString("url",url).putString("workerId",id).putString("workerName",worker).putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).putString("cipher",Base64.encodeToString(encrypted,Base64.NO_WRAP)).commit())throw new java.io.IOException("Kode gagal disimpan.");
    }
}
