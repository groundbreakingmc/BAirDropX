package org.by1337.bairx.summon;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.by1337.bairx.BAirDropX;
import org.by1337.bairx.airdrop.AirDrop;
import org.by1337.bairx.airdrop.Summonable;
import org.by1337.bairx.event.Event;
import org.by1337.bairx.event.EventType;
import org.by1337.bairx.exception.ConfigurationReadException;
import org.by1337.bairx.hook.wg.RegionManager;
import org.by1337.bairx.menu.ItemBuilder;
import org.by1337.bairx.random.WeightedAirDrop;
import org.by1337.bairx.random.WeightedRandomItemSelector;
import org.by1337.bairx.util.Validate;
import org.by1337.blib.configuration.YamlContext;
import org.by1337.blib.util.NameKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Summoner {
    private static final AtomicInteger counter = new AtomicInteger();

    private final int modelData;
    private final String material;
    private final String name;
    private final List<String> lore;
    private final boolean spawnMirror;
    private final boolean usePlayerLoc;
    private final boolean checkUpBlocks;
    private final boolean regionCheck;
    private final int minY;
    private final int maxY;
    private final List<WeightedAirDrop> airDrops;
    private final WeightedRandomItemSelector<WeightedAirDrop> airSelector;
    private final NameKey id;

    public Summoner(int modelData, String material, String name, List<String> lore, boolean spawnMirror, boolean usePlayerLoc, boolean checkUpBlocks, boolean regionCheck, int minY, int maxY, List<WeightedAirDrop> airDrops, NameKey id) {
        this.modelData = modelData;
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.spawnMirror = spawnMirror;
        this.usePlayerLoc = usePlayerLoc;
        this.checkUpBlocks = checkUpBlocks;
        this.regionCheck = regionCheck;
        this.minY = minY;
        this.maxY = maxY;
        this.airDrops = airDrops;
        this.id = id;
        airSelector = new WeightedRandomItemSelector<>(this.airDrops, new Random());
    }

    public Summoner(YamlContext context) {
        id = context.getAsNameKey("id");
        modelData = context.getAsInteger("modelData");
        material = context.getAsString("material");
        name = context.getAsString("name");
        lore = context.getList("lore", String.class);
        spawnMirror = context.getAsBoolean("clone");
        usePlayerLoc = context.getAsBoolean("usePlayerLoc");
        checkUpBlocks = context.getAsBoolean("checkUpBlocks");
        regionCheck = context.getAsBoolean("regionCheck");
        minY = context.getAsInteger("minY");
        maxY = context.getAsInteger("maxY");
        airDrops = new ArrayList<>();
        for (String air : context.getList("airDrops", String.class)) {
            String[] arr = air.split(":");
            if (arr.length != 2) {
                throw new ConfigurationReadException("Ожидалось `<airdrop>:<chance>`, а не " + air);
            }
            NameKey id = new NameKey(arr[0]);
            int chance = Validate.tryMap(arr[1], Integer::parseInt, "Ожидалось число, а не '%s'", arr[1]);
            airDrops.add(new WeightedAirDrop(id, chance));
        }
        airSelector = new WeightedRandomItemSelector<>(this.airDrops, new Random());
    }

    public ItemStack getItem() {
        return new ItemBuilder()
                .material(material)
                .lore(lore)
                .name(name)
                .putNbt(SummonerManager.NBT_KEY, id.getName())
                .modelData(modelData)
                .build();
    }

    public boolean isIt(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;
        var meta = itemStack.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        var str = pdc.get(SummonerManager.NBT_KEY, PersistentDataType.STRING);
        return id.getName().equals(str);
    }

    public Result summon(Player player, Location location) {
        NameKey id = airSelector.getRandomItem().getId();
        if (id == null) {
            return new Result(ResultStatus.FAILED, "&cНе получилось призвать не один аирдроп");
        }
        AirDrop airDrop = BAirDropX.getAirdropById(id);
        if (airDrop == null) {
            BAirDropX.getMessage().error("Неизвестный аирдроп %s", id);
            return new Result(ResultStatus.FAILED, "&cНе получилось призвать не один аирдроп");
        }
        if (!(airDrop instanceof Summonable summonable)) {
            BAirDropX.getMessage().error("Невозможно призвать аирдроп %s так как он не реализует интерфейс Summonable!", id);
            return new Result(ResultStatus.FAILED, "&cНе получилось призвать не один аирдроп");
        }
        if (location.getBlockY() < minY) {
            return new Result(ResultStatus.FAILED, "&cМинимальная высота для призыва аирдропа %s", minY);
        }
        if (location.getBlockY() > maxY) {
            return new Result(ResultStatus.FAILED, "&cМаксимальная высота для призыва аирдропа %s", maxY);
        }
        if (usePlayerLoc && checkUpBlocks) {
            if (location.getBlockY() -1 != location.getWorld().getHighestBlockAt(location).getY()) {
                return new Result(ResultStatus.FAILED, "&cВы можете призвать аирдроп только под открытым небом!");
            }
        }
        if (regionCheck) {
            if (!RegionManager.isRegionEmpty(airDrop.getGeneratorSetting().regionRadius, location)) {
                return new Result(ResultStatus.FAILED, "&cВы можете призвать аирдроп в чей-то регион!");
            }
        }

        var result = summonable.canBeSummoned(player, this);
        if (result.status == ResultStatus.FAILED) {
            return result;
        }

        AirDrop summon;
        if (spawnMirror) {
            summon = airDrop.createMirror(new NameKey(id.getName() + "-clone-" + counter.getAndIncrement()));
            BAirDropX.registerAirDrop(summon);
        } else {
            summon = airDrop;
        }
        Event event = new Event(summon, player, EventType.SUMMONED);
        event.registerPlaceholder("{summon_x}", location::getBlockX);
        event.registerPlaceholder("{summon_y}", location::getBlockY);
        event.registerPlaceholder("{summon_z}", location::getBlockZ);

        ((Summonable)summon).summon(player, location, this);
        summon.callEvent(event);

        return new Result(ResultStatus.SUCCESSFULLY, null);
    }

    public static class Result {
        public final ResultStatus status;
        @Nullable
        public final String message;

        public Result(ResultStatus status, @Nullable String message) {
            this.status = status;
            this.message = message;
        }

        public Result(ResultStatus status, @NotNull String message, Object... objects) {
            this.status = status;
            this.message = String.format(message, objects);
        }
    }

    public static enum ResultStatus {
        SUCCESSFULLY,
        FAILED
    }

    public int getModelData() {
        return modelData;
    }

    public String getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public boolean isSpawnMirror() {
        return spawnMirror;
    }

    public boolean isUsePlayerLoc() {
        return usePlayerLoc;
    }

    public boolean isCheckUpBlocks() {
        return checkUpBlocks;
    }

    public boolean isRegionCheck() {
        return regionCheck;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public List<WeightedAirDrop> getAirDrops() {
        return airDrops;
    }

    public WeightedRandomItemSelector<WeightedAirDrop> getAirSelector() {
        return airSelector;
    }

    public NameKey getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Summoner{" +
                "modelData=" + modelData +
                ", material='" + material + '\'' +
                ", name='" + name + '\'' +
                ", lore=" + lore +
                ", spawnMirror=" + spawnMirror +
                ", usePlayerLoc=" + usePlayerLoc +
                ", checkUpBlocks=" + checkUpBlocks +
                ", regionCheck=" + regionCheck +
                ", minY=" + minY +
                ", maxY=" + maxY +
                ", airDrops=" + airDrops +
                ", airSelector=" + airSelector +
                ", id=" + id +
                '}';
    }
}