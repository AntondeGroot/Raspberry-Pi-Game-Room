package ADG;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;

@SuppressWarnings("deprecation")
public class GreetingResponse_FieldSerializer implements com.google.gwt.user.client.rpc.impl.TypeHandler {
  private static native java.lang.String getGreeting(ADG.GreetingResponse instance) /*-{
    return instance.@ADG.GreetingResponse::greeting;
  }-*/;
  
  private static native void setGreeting(ADG.GreetingResponse instance, java.lang.String value) 
  /*-{
    instance.@ADG.GreetingResponse::greeting = value;
  }-*/;
  
  private static native java.lang.String getServerInfo(ADG.GreetingResponse instance) /*-{
    return instance.@ADG.GreetingResponse::serverInfo;
  }-*/;
  
  private static native void setServerInfo(ADG.GreetingResponse instance, java.lang.String value) 
  /*-{
    instance.@ADG.GreetingResponse::serverInfo = value;
  }-*/;
  
  private static native java.lang.String getUserAgent(ADG.GreetingResponse instance) /*-{
    return instance.@ADG.GreetingResponse::userAgent;
  }-*/;
  
  private static native void setUserAgent(ADG.GreetingResponse instance, java.lang.String value) 
  /*-{
    instance.@ADG.GreetingResponse::userAgent = value;
  }-*/;
  
  public static void deserialize(SerializationStreamReader streamReader, ADG.GreetingResponse instance) throws SerializationException {
    setGreeting(instance, streamReader.readString());
    setServerInfo(instance, streamReader.readString());
    setUserAgent(instance, streamReader.readString());
    
  }
  
  public static ADG.GreetingResponse instantiate(SerializationStreamReader streamReader) throws SerializationException {
    return new ADG.GreetingResponse();
  }
  
  public static void serialize(SerializationStreamWriter streamWriter, ADG.GreetingResponse instance) throws SerializationException {
    streamWriter.writeString(getGreeting(instance));
    streamWriter.writeString(getServerInfo(instance));
    streamWriter.writeString(getUserAgent(instance));
    
  }
  
  public Object create(SerializationStreamReader reader) throws SerializationException {
    return ADG.GreetingResponse_FieldSerializer.instantiate(reader);
  }
  
  public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {
    ADG.GreetingResponse_FieldSerializer.deserialize(reader, (ADG.GreetingResponse)object);
  }
  
  public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {
    ADG.GreetingResponse_FieldSerializer.serialize(writer, (ADG.GreetingResponse)object);
  }
  
}
