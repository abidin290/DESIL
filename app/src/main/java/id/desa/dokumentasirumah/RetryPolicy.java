package id.desa.dokumentasirumah;
import java.io.IOException;
import java.net.*;

final class RetryPolicy {
    interface Operation<T>{T run()throws Exception;}
    interface Pause{void sleep(long millis)throws InterruptedException;}
    interface Notice{void waiting(int retry,int total,long millis);}
    static final class Failure extends IOException {
        final boolean retryable;
        Failure(String message,boolean retryable){super(message);this.retryable=retryable;}
    }
    static boolean canRetry(Exception e){
        if(e instanceof Failure)return ((Failure)e).retryable;
        return e instanceof SocketTimeoutException || e instanceof UnknownHostException || e instanceof SocketException || e instanceof java.io.EOFException;
    }
    static <T>T run(Operation<T> action,Pause pause,Notice notice)throws Exception{
        for(int attempt=0;;attempt++){
            try{return action.run();}catch(Exception e){
                if(!canRetry(e)||attempt>=2)throw e;
                long delay=2000L*(attempt+1);notice.waiting(attempt+1,2,delay);pause.sleep(delay);
            }
        }
    }
    private RetryPolicy(){}
}
