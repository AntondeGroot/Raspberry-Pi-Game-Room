package ADG;

import com.google.gwt.user.client.rpc.IsSerializable;
import java.util.Objects;

public class Room implements IsSerializable {
    private String createdByUserId;
    private String name;
    private Integer nrOfPlayers = 0;

    public Room() {} // Default constructor

    public Room(String name, String createdByUserId) {
        setCreatedByUserId(createdByUserId);
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNrOfPlayers() {
        return nrOfPlayers;
    }

    public void setNrOfPlayers(Integer nrOfPlayers) {
        this.nrOfPlayers = nrOfPlayers;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return Objects.equals(name, room.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "Room{" +
                "name='" + name + '\'' +
                '}';
    }
}
