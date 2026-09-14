package id.desa.dokumentasirumah;
import okhttp3.mockwebserver.*;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;
public class CentralClientTest {
    @Test public void retriesBusyResponseButNeverInvalidCode()throws Exception{
        try(MockWebServer server=new MockWebServer()){
            server.start();CentralClient client=new CentralClient(server.url("/").toString(),"ACCESS-CODE");
            server.enqueue(new MockResponse().setBody("{\"ok\":false,\"retryable\":true,\"message\":\"busy\"}"));
            server.enqueue(new MockResponse().setBody("{\"ok\":true}"));
            assertTrue(client.callWithRetry(new JSONObject().put("action","begin"),(a,b,c)->{}).getBoolean("ok"));
            assertEquals(2,server.getRequestCount());
            String first=server.takeRequest().getBody().readUtf8();assertEquals(first,server.takeRequest().getBody().readUtf8());
            server.enqueue(new MockResponse().setBody("{\"ok\":false,\"retryable\":false,\"message\":\"Kode ditolak\"}"));
            try{client.callWithRetry(new JSONObject(),(a,b,c)->fail("No retry for denied code"));fail();}catch(IOException expected){}
            assertEquals(3,server.getRequestCount());
        }
    }
    @Test public void onlyAcceptsAppsScriptProductionUrl(){
        assertTrue(CentralClient.validEndpoint("https://script.google.com/macros/s/ABC_123/exec"));
        for(String url:new String[]{"http://script.google.com/macros/s/abc/exec","https://evil.com/exec","https://script.google.com/macros/s/abc/dev","https://script.google.com@evil.com/macros/s/abc/exec"})assertFalse(CentralClient.validEndpoint(url));
    }
    @Test public void postsCodeAndRejectsApplicationError()throws Exception{
        try(MockWebServer server=new MockWebServer()){
            server.start();CentralClient client=new CentralClient(server.url("/").toString(),"ACCESS-CODE");
            server.enqueue(new MockResponse().setBody("{\"ok\":true,\"workerId\":\"P1\"}"));
            assertEquals("P1",client.call(new JSONObject().put("action","auth")).getString("workerId"));
            RecordedRequest req=server.takeRequest();assertEquals("POST",req.getMethod());assertEquals("ACCESS-CODE",new JSONObject(req.getBody().readUtf8()).getString("code"));
            server.enqueue(new MockResponse().setBody("{\"ok\":false,\"message\":\"Kode dinonaktifkan\"}"));
            try{client.call(new JSONObject());fail();}catch(IOException e){assertEquals("Kode dinonaktifkan",e.getMessage());}
            server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location","https://evil.example/collect"));
            try{client.call(new JSONObject());fail();}catch(IOException expected){}
            server.enqueue(new MockResponse().setBody("<html>Google login</html>"));
            try{client.call(new JSONObject());fail();}catch(IOException expected){}
        }
    }
}
