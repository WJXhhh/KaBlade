package com.wjx.kablade.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.fml.relauncher.FMLInjectionData;

/**
 * 测试功能的令牌校验。服务端按玩家 UUID 保存授权状态，客户端不能仅靠伪造交互包开启功能。
 */
public final class TestFeatureTokenAuth {
    private static final String TOKEN_URL = "https://raw.giteeusercontent.com/wjx4r/FML_GERB_NetData/raw/master/OtherMODUpdate/token";
    private static volatile String remoteToken;
    private static volatile boolean tokenLoaded;
    private static File tokenFile;
    private static final Set<UUID> AUTHORIZED_PLAYERS = ConcurrentHashMap.newKeySet();

    private TestFeatureTokenAuth() {
    }

    /**
     * Forge 注入的游戏目录，能自然适配启动器的版本隔离。
     */
    public static void init() {
        File gameDirectory = (File) FMLInjectionData.data()[6];
        tokenFile = new File(gameDirectory, "token.txt");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                remoteToken = readRemoteToken();
                tokenLoaded = true;
            }
        }, "KaBlade-TestFeature-Token");
        thread.setDaemon(true);
        thread.start();
    }

    public static boolean isEnabled(UUID playerId) {
        return AUTHORIZED_PLAYERS.contains(playerId);
    }

    /** 客户端仅在本地 token 已与远端内容一致时才发送认证请求。 */
    public static String getLocalTokenForAuth() {
        if (!tokenLoaded || remoteToken == null || tokenFile == null || !tokenFile.isFile()) {
            return null;
        }

        String localToken = readLocalToken();
        return localToken != null && remoteToken.equals(localToken) ? localToken : null;
    }

    public static void authorize(UUID playerId, String suppliedToken) {
        if (tokenLoaded && remoteToken != null && remoteToken.equals(suppliedToken)) {
            AUTHORIZED_PLAYERS.add(playerId);
        }
    }

    private static String readRemoteToken() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            return readToken(connection.getInputStream());
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readLocalToken() {
        try {
            return readToken(new java.io.FileInputStream(tokenFile));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String readToken(InputStream input) throws Exception {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();
            String token = content.toString().trim();
            if (token.startsWith("\uFEFF")) {
                token = token.substring(1);
            }
            return token.isEmpty() ? null : token;
        } finally {
            input.close();
        }
    }
}
