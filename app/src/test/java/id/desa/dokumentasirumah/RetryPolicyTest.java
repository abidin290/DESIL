package id.desa.dokumentasirumah;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.net.SocketTimeoutException;
public class RetryPolicyTest {
    @Test public void transientFailureRetriesSameOperationAndThenSucceeds()throws Exception{
        int[] attempts={0};List<Long> delays=new ArrayList<>();
        String result=RetryPolicy.run(()->{if(attempts[0]++<2)throw new SocketTimeoutException();return "done";},delays::add,(a,b,c)->{});
        assertEquals("done",result);assertEquals(3,attempts[0]);assertEquals(Arrays.asList(2000L,4000L),delays);
    }
    @Test public void permanentErrorStopsImmediately()throws Exception{
        int[] attempts={0};try{RetryPolicy.run(()->{attempts[0]++;throw new RetryPolicy.Failure("Kode ditolak",false);},ms->fail("Must not wait"),(a,b,c)->{});fail();}catch(RetryPolicy.Failure expected){}assertEquals(1,attempts[0]);
    }
    @Test public void retriesAreBoundedAndInterruptionPropagates()throws Exception{
        int[] attempts={0};try{RetryPolicy.run(()->{attempts[0]++;throw new RetryPolicy.Failure("busy",true);},ms->{},(a,b,c)->{});fail();}catch(RetryPolicy.Failure expected){}assertEquals(3,attempts[0]);
        try{RetryPolicy.run(()->{throw new SocketTimeoutException();},ms->{throw new InterruptedException();},(a,b,c)->{});fail();}catch(InterruptedException expected){}
    }
}
