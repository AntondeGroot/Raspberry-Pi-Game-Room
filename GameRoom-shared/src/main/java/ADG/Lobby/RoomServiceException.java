package ADG.Lobby;

import com.google.gwt.user.client.rpc.IsSerializable;

public class RoomServiceException extends Exception implements IsSerializable {

    public RoomServiceException() {
        super();
    }

    public RoomServiceException(String message) {
        super(message);
    }

    public RoomServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}