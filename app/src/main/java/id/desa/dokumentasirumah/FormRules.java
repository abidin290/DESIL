package id.desa.dokumentasirumah;

public final class FormRules {
    public static final int REQUIRED_COUNT = 5;
    public static final String[] LABELS = {"DEPAN RUMAH", "DALAM RUMAH", "SAMPING KIRI", "SAMPING KANAN", "BELAKANG", "KTP", "KK", "IDPEL LISTRIK"};
    public static final String[] FILES = {"Depan_Rumah.jpg", "Dalam_Rumah.jpg", "Samping_Kiri.jpg", "Samping_Kanan.jpg", "Belakang.jpg", "KTP.jpg", "KK.jpg", "IDPEL_Listrik.jpg"};
    public static boolean ready(String name, boolean[] photos) {
        if (name == null || name.trim().isEmpty() || photos == null || photos.length < REQUIRED_COUNT) return false;
        for (int i=0;i<REQUIRED_COUNT;i++) if (!photos[i]) return false;
        return true;
    }
    private FormRules() {}
}
