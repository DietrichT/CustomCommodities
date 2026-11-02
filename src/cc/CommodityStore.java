package cc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Persists custom commodities by appending a row to this mod's data/campaign/commodities.csv.
 *
 * The game reads that file once, at startup, so a row written now is picked up by the vanilla
 * loader on the next launch. Writing is atomic (tmp file + move) because a half-written CSV
 * breaks spec loading for the whole mod.
 */
public class CommodityStore {

    public static final String MOD_ID = "customcommodities";

    public static final String HEADER = "name,id,demand class,base price,export value,price variability,"
            + "utility,origin,tags,stack size,cargo space,icon,sound id,sound id drop,order,economyTier,"
            + "econUnitOrigUnused,econUnit,baseRaidDanger,eU * base value,iconWidthMult,plugin,desc";

    private static final String CSV_REL = "data/campaign/commodities.csv";
    private static final Logger LOG = Global.getLogger(CommodityStore.class);

    private CommodityStore() {
    }

    public static Path csvPath() throws IOException {
        ModManagerAPI manager = Global.getSettings().getModManager();
        if (manager != null) {
            ModSpecAPI spec = manager.getModSpec(MOD_ID);
            if (spec != null && spec.getPath() != null) {
                return Paths.get(spec.getPath()).resolve(CSV_REL);
            }
        }
        return fromCodeSource().resolve(CSV_REL);
    }

    /** Fallback for the case where the mod manager is not up yet; the folder name has a space in it. */
    private static Path fromCodeSource() throws IOException {
        URL url = CommodityStore.class.getProtectionDomain().getCodeSource().getLocation();
        Path p;
        try {
            p = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            try {
                p = Paths.get(URLDecoder.decode(url.getPath(), "UTF-8"));
            } catch (UnsupportedEncodingException e2) {
                throw new IOException("cannot read code source " + url, e);
            }
        }
        for (int i = 0; i < 5 && p != null; i++) {
            Path dir = Files.isDirectory(p) ? p : p.getParent();
            if (dir == null) break;
            if (Files.exists(dir.resolve("mod_info.json"))) return dir;
            p = dir.getParent();
        }
        throw new IOException("could not locate the mod folder from " + url);
    }

    /** Append one commodity row, atomically, then read the file back to prove the row landed. */
    public static void append(CommodityParams p) throws IOException {
        appendTo(csvPath(), p);
    }

    /** Same write, with the target passed in so it can be exercised without a running game. */
    static void appendTo(Path target, CommodityParams p) throws IOException {
        Files.createDirectories(target.getParent());

        String body = Files.exists(target) ? new String(Files.readAllBytes(target), StandardCharsets.UTF_8) : "";
        if (body.startsWith("\uFEFF")) body = body.substring(1);
        if (body.isEmpty()) {
            body = HEADER + "\r\n";
        } else if (!body.endsWith("\n")) {
            body = body + "\r\n";
        }

        String row = p.toCsvRow();
        String updated = body + row + "\r\n";

        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        Files.write(tmp, updated.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String verify = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
        if (!verify.contains("," + p.id + ",")) {
            throw new IOException("row for " + p.id + " not found after writing " + target);
        }
        LOG.info("[CC] wrote '" + p.id + "' to " + target);
    }
}
