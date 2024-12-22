package ADG;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Message implements IsSerializable {
    String nameSender;
    String message;
    String timestamp;

    public Message(){}

    public Message(String timestamp, String nameSender, String message) {
        this.nameSender = nameSender;
        this.message = message;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return timestamp + " - " + nameSender + ": " + "\t" + message;
    }

    public String getNameSender() {
        return nameSender;
    }

    public void setNameSender(String nameSender) {
        this.nameSender = nameSender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
