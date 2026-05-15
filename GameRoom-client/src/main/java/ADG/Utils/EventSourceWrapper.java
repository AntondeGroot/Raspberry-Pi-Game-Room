package ADG.Utils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Timer;

public class EventSourceWrapper {

    public interface MessageCallback {
        void onMessage(String data);
    }

    private JavaScriptObject nativeEs;
    private MessageCallback messageCallback;
    private Runnable closeHandler;
    private String url;
    private boolean closed = false;

    public void open(String url, MessageCallback callback) {
        open(url, callback, null);
    }

    public void open(String url, MessageCallback callback, Runnable onServerClose) {
        this.url = url;
        this.messageCallback = callback;
        this.closeHandler = onServerClose;
        this.closed = false;
        connect();
    }

    public void close() {
        closed = true;
        if (nativeEs != null) {
            nativeClose(nativeEs);
            nativeEs = null;
        }
    }

    private void connect() {
        nativeEs = nativeOpen(url, messageCallback, this);
    }

    @SuppressWarnings("unused") // called by name from JSNI
    private void scheduleReconnect() {
        if (closed) return;
        new Timer() {
            @Override
            public void run() {
                if (!closed) connect();
            }
        }.schedule(3000);
    }

    @SuppressWarnings("unused") // called by name from JSNI
    private void onStreamClosed() {
        closed = true;
        if (nativeEs != null) {
            nativeClose(nativeEs);
            nativeEs = null;
        }
        if (closeHandler != null) closeHandler.run();
    }

    private native JavaScriptObject nativeOpen(String url, MessageCallback cb, EventSourceWrapper wrapper) /*-{
        var es = new EventSource(url);
        es.onmessage = $entry(function(event) {
            cb.@ADG.Utils.EventSourceWrapper.MessageCallback::onMessage(Ljava/lang/String;)(event.data);
        });
        es.addEventListener('stream_closed', $entry(function() {
            wrapper.@ADG.Utils.EventSourceWrapper::onStreamClosed()();
        }));
        es.onerror = $entry(function() {
            if (es.readyState === EventSource.CLOSED) {
                wrapper.@ADG.Utils.EventSourceWrapper::scheduleReconnect()();
            }
        });
        return es;
    }-*/;

    private native void nativeClose(JavaScriptObject es) /*-{
        es.close();
    }-*/;
}