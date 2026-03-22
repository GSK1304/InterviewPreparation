package lld.spaceship;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class GameEventBus {
    private final List<Consumer<GameEvent>> listeners = new CopyOnWriteArrayList<>();
    public void subscribe(Consumer<GameEvent> l) { listeners.add(l); }
    public void publish(GameEvent event) {
        listeners.forEach(l -> { try { l.accept(event); } catch (Exception ignored) {} });
    }
}
