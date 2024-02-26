package org.by1337.bairx.timer;

import org.by1337.bairx.airdrop.AirDrop;
import org.by1337.bairx.timer.strategy.TimerRegistry;
import org.by1337.blib.util.NameKey;

import java.util.Collection;

public interface Timer {
    NameKey name();

    void linkAirDrop(AirDrop airDrop);

    void unlinkAirDrop(AirDrop airDrop);

    Collection<AirDrop> linkedAirDrops();

    void tick(final long currentTick);

    TimerRegistry getType();

    void close();

}