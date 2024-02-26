package org.by1337.bairx.timer;

import org.by1337.bairx.BAirDropX;
import org.by1337.bairx.airdrop.AirDrop;
import org.by1337.bairx.event.Event;
import org.by1337.bairx.event.EventListener;
import org.by1337.bairx.event.EventListenerManager;
import org.by1337.bairx.event.EventType;
import org.by1337.bairx.exception.PluginInitializationException;
import org.by1337.bairx.timer.strategy.TimerRegistry;
import org.by1337.bairx.util.Validate;
import org.by1337.blib.configuration.YamlContext;
import org.by1337.blib.util.NameKey;
import org.by1337.blib.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Ticker implements Timer, EventListener {
    private static final Random random = new Random();
    private final List<Pair<NameKey, Integer>> airdrops = new ArrayList<>();
    @Nullable
    private AirDrop current;
    private final HashSet<AirDrop> bypassed = new HashSet<>();
    private final HashSet<NameKey> lickedAirDrops = new HashSet<>();
    private NameKey name;
    private int tickSpeed;
    private TickType tickType;

    public Ticker(YamlContext context) {
        name = Validate.notNull(context.getAsNameKey("name"), "Параметр `name` не указан!");
        tickSpeed = Validate.notNull(context.getAsInteger("tick-speed"), "Параметр `tick-speed` не указан!");
        String tickTypeString = Validate.notNull(context.getAsString("tick-type"), "Параметр `tick-type` не указан!");
        Validate.test(tickTypeString, str -> !str.equals("all") && !str.equals("by_chance"), () -> new PluginInitializationException("Параметр `tick-type` может быть только 'all' или 'by_chance'"));
        tickType = tickTypeString.equals("all") ? TickType.ALL : TickType.BY_CHANCE;

        Map<?, ?> airdropsRawMap = (Map<?, ?>) Validate.test(
                context.getAs("licked-airdrops", Object.class),
                obj -> !(obj instanceof Map<?, ?>),
                () -> new PluginInitializationException("Параметр `licked-airdrops` имеет не правильный тип!")
        );

        for (Map.Entry<?, ?> entry : airdropsRawMap.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            NameKey id = new NameKey(String.valueOf(key));
            if (tickType == TickType.ALL) {
                airdrops.add(Pair.of(id, 100));
            } else {
                airdrops.add(Pair.of(
                        id,
                        Validate.tryMap(value, (obj) -> Integer.parseInt(String.valueOf(obj)), "%s должен быть числом!", value)
                ));
            }
            lickedAirDrops.add(id);
        }
        airdrops.sort(Comparator.comparingInt(Pair::getValue));

        if (tickType == TickType.BY_CHANCE) {
            EventListenerManager.register(BAirDropX.getInstance(), this);
        }
    }

    @Override
    public void tick(final long currentTick) {
        if (currentTick % tickSpeed != 0) return;
        if (tickType == TickType.BY_CHANCE) {
            if (current == null) {
                current = nextAirDrop();
            }
            if (current != null) current.tick();
            bypassed.forEach(AirDrop::tick);
        } else {
            lickedAirDrops.stream().map(BAirDropX::getAirdropById).filter(Objects::nonNull).forEach(AirDrop::tick);
        }
    }

    private AirDrop nextAirDrop() {
        for (Pair<NameKey, Integer> airdrop : airdrops) {
            if (airdrop.getRight() >= random.nextInt(100)) {
                var air = BAirDropX.getAirdropById(airdrop.getLeft());
                if (air == null) {
                    BAirDropX.getMessage().warning("Таймер %s не найден аирдроп %s", name, airdrop.getLeft());
                } else if (!air.isStarted()) {
                    return air;
                }
            }
        }
        return null;
    }

    @Override
    public void onEvent(@NotNull Event event, @NotNull AirDrop airDrop) {
        if (tickType == TickType.ALL) return;
        if (!lickedAirDrops.contains(airDrop.getId())) return;
        if (event.getEventType() == EventType.END) {
            if (current == airDrop) {
                current = null;
            }
            bypassed.remove(airDrop);
        }
        if (event.getEventType() == EventType.START && this.equals(airDrop.getTimer())) {
            if (current != airDrop) {
                bypassed.add(airDrop);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticker ticker = (Ticker) o;
        return tickSpeed == ticker.tickSpeed && Objects.equals(airdrops, ticker.airdrops) && Objects.equals(current, ticker.current) && Objects.equals(bypassed, ticker.bypassed) && Objects.equals(lickedAirDrops, ticker.lickedAirDrops) && Objects.equals(name, ticker.name) && tickType == ticker.tickType;
    }


    @Override
    public NameKey name() {
        return name;
    }

    @Override
    public void linkAirDrop(AirDrop airDrop) {

    }

    @Override
    public void unlinkAirDrop(AirDrop airDrop) {

    }

    @Override
    public Collection<AirDrop> linkedAirDrops() {
        return null;
    }


    @Override
    public TimerRegistry getType() {
        return TimerRegistry.TICKER;
    }

    @Override
    public void close() {

    }

    private enum TickType {
        ALL,
        BY_CHANCE
    }
}