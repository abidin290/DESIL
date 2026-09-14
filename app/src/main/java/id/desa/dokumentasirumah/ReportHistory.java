package id.desa.dokumentasirumah;
import android.content.*;
import org.json.*;
import java.io.IOException;

/** Local receipts only. A successful receipt is never downgraded by a resumed draft. */
final class ReportHistory {
    private final android.content.SharedPreferences prefs;
    ReportHistory(Context context){prefs=context.getSharedPreferences("report_history",Context.MODE_PRIVATE);}
    synchronized JSONArray entries()throws JSONException{return new JSONArray(prefs.getString("reports","[]"));}
    synchronized void save(String id,String name,String worker,String state,String detail)throws Exception{
        if(id==null || id.isEmpty())return;
        JSONArray previous=entries(), next=new JSONArray();JSONObject entry=null;
        for(int i=0;i<previous.length();i++)if(id.equals(previous.getJSONObject(i).optString("id")))entry=previous.getJSONObject(i);
        if(entry!=null && "Selesai".equals(entry.optString("state")) && !"Selesai".equals(state))return;
        if(entry==null)entry=new JSONObject().put("id",id).put("createdAt",System.currentTimeMillis());
        entry.put("name",name).put("worker",worker).put("state",state).put("detail",detail).put("updatedAt",System.currentTimeMillis());next.put(entry);
        for(int i=0;i<previous.length() && next.length()<200;i++)if(!id.equals(previous.getJSONObject(i).optString("id")))next.put(previous.getJSONObject(i));
        if(!prefs.edit().putString("reports",next.toString()).commit())throw new IOException("Riwayat belum dapat disimpan. Draf tetap tersedia.");
    }
}
