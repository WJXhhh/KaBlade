package com.wjx.kablade.update;

import com.wjx.kablade.Main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本检查 —— 仿照 1.12.2 旧版：后台线程拉取远端文本里的最新版本号，与当前版本比较，
 * 有更新就置 {@link #updateAvailable}，由 {@link com.wjx.kablade.event.UpdateNotifier} 在玩家进入世界时提示。
 * <p>
 * 比旧版多了「小版本号（后缀）」比较：版本形如 {@code 主.次.修订[-后缀]}，例如 {@code 2.0.0-a}。
 * 规则（贴近 semver 预发布语义）：
 * <ol>
 *   <li>先按 主/次/修订 数字逐段比较；</li>
 *   <li>三段相同时再比后缀：无后缀视为正式版，<b>高于</b>任何带后缀的预发布版；两者都带后缀时按字典序比较
 *       （{@code a < b < c}…），于是 {@code 2.0.0-a} 能检测到 {@code 2.0.0-b}。</li>
 * </ol>
 * 网络与解析失败一律静默（仅记日志），绝不阻塞游戏启动。
 */
public final class UpdateChecker {

    /** 远端版本文本：取其中第一处形如 X.Y.Z[-后缀] 的内容作为最新版本号。 */
    private static final String UPDATE_URL =
            "https://raw.giteeusercontent.com/wjx4r/FML_GERB_NetData/raw/master/OtherMODUpdate/KaBlade2.txt";

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.]+))?");

    private static final int TIMEOUT_MS = 12000;

    /** 远端最新版本号（解析成功后填充，用于提示文案）。 */
    public static volatile String latestVersion = null;
    /** 是否检测到更新。 */
    public static volatile boolean updateAvailable = false;

    private UpdateChecker() {
    }

    /** 启动后台检查（守护线程，不阻塞、不影响关服）。 */
    public static void start(String currentVersion) {
        Thread t = new Thread(() -> run(currentVersion), "kablade-update-check");
        t.setDaemon(true);
        t.start();
    }

    private static void run(String currentVersion) {
        try {
            String remote = fetchVersion();
            if (remote == null) {
                Main.LOGGER.info("[{}] 版本检查：远端未给出可识别的版本号。", Main.MODID);
                return;
            }
            latestVersion = remote;
            if (compare(remote, currentVersion) > 0) {
                updateAvailable = true;
                Main.LOGGER.info("[{}] 检测到新版本：{}（当前 {}）。", Main.MODID, remote, currentVersion);
            } else {
                Main.LOGGER.info("[{}] 已是最新版本（当前 {}，远端 {}）。", Main.MODID, currentVersion, remote);
            }
        } catch (Exception e) {
            Main.LOGGER.warn("[{}] 版本检查失败：{}", Main.MODID, e.toString());
        }
    }

    /** 拉取远端文本，返回首个形如 X.Y.Z[-后缀] 的版本串；失败返回 null。 */
    private static String fetchVersion() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(UPDATE_URL).openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", Main.MODID + "-update-check");
        conn.connect();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = VERSION.matcher(line);
                if (m.find()) {
                    return m.group();
                }
            }
        } finally {
            conn.disconnect();
        }
        return null;
    }

    /** 返回值 &gt;0 表示 remote 比 local 新、0 相等、&lt;0 更旧。 */
    static int compare(String remote, String local) {
        Matcher r = VERSION.matcher(remote);
        Matcher l = VERSION.matcher(local);
        if (!r.find() || !l.find()) {
            // 无法解析时按字符串兜底比较，避免误报。
            return remote.compareTo(local);
        }
        for (int g = 1; g <= 3; g++) {
            int cmp = Integer.compare(Integer.parseInt(r.group(g)), Integer.parseInt(l.group(g)));
            if (cmp != 0) {
                return cmp;
            }
        }
        String rs = r.group(4) == null ? "" : r.group(4);
        String ls = l.group(4) == null ? "" : l.group(4);
        if (rs.isEmpty() && ls.isEmpty()) {
            return 0;
        }
        if (rs.isEmpty()) {
            return 1;   // remote 为正式版，高于带后缀的预发布版
        }
        if (ls.isEmpty()) {
            return -1;  // local 为正式版，remote 仍是预发布版 → 不算更新
        }
        return rs.compareTo(ls);   // 同为预发布：后缀字典序，靠后者更新（a < b < c …）
    }
}
