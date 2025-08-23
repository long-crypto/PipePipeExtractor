package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.*;

import org.brotli.dec.BrotliInputStream;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.*;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.Inflater;

import okio.ByteString;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.QUERY_USER_VIDEOS_CLIENT_API_URL;
import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.QUERY_USER_VIDEOS_WEB_API_URL;
import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.WBI_IMG_URL;
import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.WWW_REFERER;
import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders;


public class utils {
    private static final BigInteger XOR_CODE = new BigInteger("23442827791579");
    private static final BigInteger MASK_CODE = new BigInteger("2251799813685247");
    private static final BigInteger MAX_AID = BigInteger.ONE.shiftLeft(51);
    private static final BigInteger BASE = new BigInteger("58");

    private static final String table = "FcwAPNKTMug3GV5Lj7EJnHpWsx4tb8haYeviqBz6rkCy12mUSDQX9RdoZf";

    private static final Pattern RENDER_DATA_PATTERN = Pattern.compile("<script id=\"__RENDER_DATA__\" type=\"application/json\">(.*?)</script>", Pattern.DOTALL);

    private static String wbiMixinKey;
    private static LocalDate wbiMixinKeyDate;
    private static final Cache<String, String> webIdCache = new Cache2kBuilder<String, String>() {
    }.entryCapacity(256).expireAfterWrite(86400, TimeUnit.SECONDS).loader(utils::getWebId).build();

    public Map<Character, Integer> map = new HashMap<Character, Integer>();

    public utils() {
        for (int i = 0; i < 58; i++) {
            map.put(table.charAt(i), i);
        }
    }

    public static String av2bv(long aid) {
        char[] bytes = {'B', 'V', '1', '0', '0', '0', '0', '0', '0', '0', '0', '0'};
        int bvIndex = bytes.length - 1;
        BigInteger tmp = (MAX_AID.or(BigInteger.valueOf(aid))).xor(XOR_CODE);
        while (tmp.compareTo(BigInteger.ZERO) > 0) {
            bytes[bvIndex] = table.charAt(tmp.mod(BASE).intValue());
            tmp = tmp.divide(BASE);
            bvIndex -= 1;
        }
        char temp = bytes[3];
        bytes[3] = bytes[9];
        bytes[9] = temp;
        temp = bytes[4];
        bytes[4] = bytes[7];
        bytes[7] = temp;
        return new String(bytes);
    }

    public static long bv2av(String bvid) {
        char[] bvidArr = bvid.toCharArray();
        char temp = bvidArr[3];
        bvidArr[3] = bvidArr[9];
        bvidArr[9] = temp;
        temp = bvidArr[4];
        bvidArr[4] = bvidArr[7];
        bvidArr[7] = temp;
        String subString = new String(bvidArr, 3, bvidArr.length - 3);
        BigInteger tmp = BigInteger.ZERO;
        for (char bvidChar : subString.toCharArray()) {
            tmp = tmp.multiply(BASE).add(BigInteger.valueOf(table.indexOf(bvidChar)));
        }
        return tmp.and(MASK_CODE).xor(XOR_CODE).longValue();
    }

    public static boolean isFirstP(String url) {
        if (!url.contains("p=")) {
            return true;
        }
        String p = url.split("p=")[1].split("&")[0];
        return p.equals("1");
    }

    public static String getUrl(String url, String id) {
        String p = "1";
        if (url.contains("p=")) {
            p = url.split("p=")[1].split("&")[0];
        }
        return "https://api.bilibili.com/x/web-interface/view?bvid=" + id + "&p=" + p;
    }

    public static String getPureBV(String id) {
        return id.split("\\?")[0];
    }


    public static String buildUserVideosUrlClientAPI(String mid, long lastVideoAid) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vmid", mid);
        params.put("mobi_app", "android");
        if (lastVideoAid > 0) {
            params.put("aid", String.valueOf(lastVideoAid));
        }
        params.put("order", "pubdate");
        String newUrl = QUERY_USER_VIDEOS_CLIENT_API_URL + "?" + params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        return newUrl;
    }

    private static int[] getWh(int width, int height) {
        int res0 = width;
        int res1 = height;
        int rnd = ThreadLocalRandom.current().nextInt(114);
        return new int[]{2 * res0 + 2 * res1 + 3 * rnd, 4 * res0 - res1 + rnd, rnd};
    }

    private static int[] getOf(int scrollTop, int scrollLeft) {
        int res0 = scrollTop;
        int res1 = scrollLeft;
        int rnd = ThreadLocalRandom.current().nextInt(514);
        return new int[]{3 * res0 + 2 * res1 + rnd, 4 * res0 - 4 * res1 + 2 * rnd, rnd};
    }

    private static String getWebId(String mid) throws IOException, JsonParserException {
        Response response;
        LinkedHashMap<String, List<String>> headers = getHeaders(WWW_REFERER);
        headers.put("Upgrade-Insecure-Requests", Collections.singletonList("1"));
        headers.put("Accept", Collections.singletonList("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"));
        headers.put("Priority", Collections.singletonList("u=0, i"));
        try {
            response = NewPipe.getDownloader().get("https://space.bilibili.com/" + mid, headers);
        } catch (ReCaptchaException e) {
            throw new RuntimeException(e);
        }
        String renderData;
        Matcher matcher = RENDER_DATA_PATTERN.matcher(response.responseBody());
        if (matcher.find()) {
            renderData = matcher.group(1);
        } else {
            throw new IOException("Invalid space page response: " + response.responseBody());
        }

        String decodedRenderData = URLDecoder.decode(renderData, StandardCharsets.UTF_8.name());
        JsonObject json = JsonParser.object().from(decodedRenderData);

        return json.getString("access_id");
    }

    public static LinkedHashMap<String, String> getDmImgParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("dm_img_list", "[]");
        params.put("dm_img_str", DeviceForger.requireRandomDevice().getWebGlVersionBase64());
        params.put("dm_cover_img_str", DeviceForger.requireRandomDevice().getWebGLRendererInfoBase64());
        int[] wh = getWh(DeviceForger.requireRandomDevice().getInnerWidth(), DeviceForger.requireRandomDevice().getInnerHeight());
        int[] of = getOf(0, 0);
        params.put("dm_img_inter", "{\"ds\":[],\"wh\":["
                + Arrays.stream(wh).mapToObj(String::valueOf).collect(Collectors.joining(","))
                + "],\"of\":["
                + Arrays.stream(of).mapToObj(String::valueOf).collect(Collectors.joining(","))
                + "]}");
        return params;
    }

    public static String buildUserVideosUrlWebAPI(String baseUrl, String id) {
        // TODO: also video download
        LinkedHashMap<String, String> params = new LinkedHashMap<>();

        params.put("mid", id);
        params.put("order", "pubdate");
        params.put("ps", "25");

        String pn = "1";
        if (baseUrl.contains("pn=")) {
            pn = baseUrl.split("pn=")[1].split("&")[0];
        }
        params.put("pn", pn);

        params.put("order_avoided", "true");

        params.put("platform", "web");
        params.put("web_location", "333.1387");

        params.putAll(getDmImgParams());

        // no longer needed currently
        // params.put("w_webid", webIdCache.get(id));

        return getWbiResult(QUERY_USER_VIDEOS_WEB_API_URL, params);
    }

    public static String getWbiResult(String baseUrl, LinkedHashMap<String, String> params) {
        String[] wbiResults = utils.encWbi(params);

        params.put("w_rid", wbiResults[0]);
        params.put("wts", wbiResults[1]);
        return baseUrl + "?" + createQueryString(params);
    }

    public static String getRecordApiUrl(String url) {
        String pn = "1", sid, mid;
        if (url.contains("pn=")) {
            pn = url.split("pn=")[1].split("&")[0];
        }
        mid = Optional.of(url.split("space.bilibili.com/")[1].split("/")[0]).orElse(url.split("space.bilibili.com/")[1]);
        sid = url.split("sid=")[1];
        return String.format("https://api.bilibili.com/x/series/archives?mid=%s&series_id=%s&only_normal=true&sort=desc&pn=%s&ps=30", mid, sid, pn);
    }

    public static String getMidFromRecordUrl(String url) {
        return url.split("space.bilibili.com/")[1].split("/")[0];
    }

    public static String getMidFromRecordApiUrl(String url) {
        return url.split("mid=")[1].split("&")[0];
    }

    public static String bcc2srt(JsonObject bcc) {
        JsonArray array = bcc.getArray("body");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array.size(); i++) {
            JsonObject temp = array.getObject(i);
            result.append(i + 1).append("\n")
                    .append(sec2time(temp.getDouble("from")))
                    .append(" --> ")
                    .append(sec2time(temp.getDouble("to"))).append("\n")
                    .append(temp.getString("content")).append("\n\n");
        }
        return result.toString();
    }

    public static String sec2time(double sec) {
        int h = (int) (sec / 3600);
        int m = (int) (sec / 60 % 60);
        int s = (int) (sec % 60);
        int f = (int) ((sec * 1000) % 1000);
        return String.format("%02d:%02d:%02d,%03d", h, m, s, f);
    }

    public static byte[] decompress(byte[] data) throws IOException {
        byte[] decompressData = null;
        Inflater decompressor = new Inflater(true);
        decompressor.reset();
        decompressor.setInput(data);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
            byte[] buf = new byte[1024];
            while (!decompressor.finished()) {
                int i = decompressor.inflate(buf);
                outputStream.write(buf, 0, i);
            }
            decompressData = outputStream.toByteArray();
        } catch (Exception ignored) {
        }
        decompressor.end();
        return decompressData;
    }

    public static byte[] decompressZlib(byte[] data) {
        byte[] output = new byte[0];

        Inflater decompresser = new Inflater();
        decompresser.reset();
        decompresser.setInput(data);

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!decompresser.finished()) {
                int i = decompresser.inflate(buf);
                o.write(buf, 0, i);
            }
            output = o.toByteArray();
        } catch (Exception e) {
            output = data;
            e.printStackTrace();
        } finally {
            try {
                o.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        decompresser.end();
        return output;
    }

    public static String decompressBrotli(byte[] body) throws IOException {
        try {
            boolean test = BufferedReader.class.getMethod("lines") != null;
            return new BufferedReader(new InputStreamReader(new BrotliInputStream(
                    new ByteArrayInputStream(body)))).lines().collect(Collectors.joining());
        } catch (Exception e) {
            // if SDK <= 24
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            InputStream is = new BrotliInputStream(bais);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    //Get next page's url from current page
    public static String getNextPageFromCurrentUrl(String currentUrl, final String varName, final int addCount
            , final boolean shouldTryInit, final String initValue, final String urlType)
            throws ParsingException {
        String varString = String.format("&%s=", varName);
        String varStringVariant = String.format("?%s=", varName);
        if (shouldTryInit && !currentUrl.contains(varString) && !currentUrl.contains(varStringVariant)) {
            varString = varString.replace("&", urlType);
            currentUrl += varString + initValue;
        }
        if (currentUrl.contains(varStringVariant)) {
            varString = varStringVariant;
        } else if (!currentUrl.contains(varString)) {
            throw new ParsingException("Could not find " + varName + " in url: " + currentUrl);
        }

        final String offset = currentUrl.split(Pattern.quote(varString))[1].split(Pattern.quote("&"))[0];
        return currentUrl.replace(varString + offset, varString
                + (Integer.parseInt(offset) + addCount));
    }

    // Default value of getNextPageFromCurrentUrl, shouldTryInit = false, initValue = 1, urlType = &
    public static String getNextPageFromCurrentUrl(String currentUrl, final String varName, final int addCount)
            throws ParsingException {
        return getNextPageFromCurrentUrl(currentUrl, varName, addCount, false, "0", "&");
    }

    public static long getDurationFromString(String duration) {
        long result = 0;
        int len = duration.split(":").length;
        result += Integer.parseInt(duration.split(":")[len - 1]);
        if (len > 1) {
            result += Integer.parseInt(duration.split(":")[len - 2]) * 60L;
        }
        if (len > 2) {
            result += (long) Integer.parseInt(duration.split(":")[len - 3]) * 60 * 60;
        }
        return result;
    }

    private static String getMixinKey(String ae) {
        int[] oe = {46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41,
                13, 37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52};
        String le = Arrays.stream(oe)
                .mapToObj(i -> String.valueOf((char) ae.charAt(i)))
                .collect(Collectors.joining());
        return le.substring(0, 32);
    }

    public static String formatParamWithPercentSpace(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String createQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String createQueryStringWithPercentSpace(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> formatParamWithPercentSpace(entry.getKey()) + "=" + formatParamWithPercentSpace(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String[] encWbi(Map<String, String> params) {
        String wbiImgUrl = WBI_IMG_URL;
        String img_value;
        String sub_value;
        String img_url;
        String sub_url;
        int wts;
        String w_rid;

        try {
            LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Shanghai"));
            if (wbiMixinKey == null || wbiMixinKeyDate.isBefore(currentDate)) {
                Response response = NewPipe.getDownloader().get(wbiImgUrl, getHeaders(WWW_REFERER));
                if (response.responseCode() != 200) {
                    throw new RuntimeException("Failed to get wbi_img");
                }
                String responseBody = response.responseBody();
                img_url = responseBody.split("\"img_url\":\"")[1].split("\"")[0];
                sub_url = responseBody.split("\"sub_url\":\"")[1].split("\"")[0];
                img_value = img_url.split("/")[img_url.split("/").length - 1].split("\\.")[0];
                sub_value = sub_url.split("/")[sub_url.split("/").length - 1].split("\\.")[0];
                wbiMixinKey = getMixinKey(img_value + sub_value);
                wbiMixinKeyDate = currentDate;
            }
            wts = Math.round(System.currentTimeMillis() / 1000f);
            Map<String, String> sortedParams = new TreeMap<>(params);
            sortedParams.put("wts", String.valueOf(wts));
            String ae = createQueryStringWithPercentSpace(sortedParams);
            String toHash = ae + wbiMixinKey;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(toHash.getBytes(StandardCharsets.UTF_8));
            w_rid = bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new String[]{w_rid, String.valueOf(wts)};
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean isClientAPIMode = false;
    public static JsonObject requestUserSpaceResponse(
            Downloader downloader,
            String url,
            Map<String, List<String>> headers
    ) throws ParsingException, IOException, ReCaptchaException {
        int maxTry = 6;
        int currentTry = maxTry;

        String responseBody = "";

        while (currentTry > 0) {
            responseBody = downloader.get(url, headers).responseBody();
            try {
                JsonObject responseJson = JsonParser.object().from(responseBody);
                long code = responseJson.getLong("code");
                if (code != 0) {
                    if (code == -352) {
                        // blocked risk control
                        DeviceForger.regenerateRandomDevice(); // try to regenerate a new one
                        currentTry -= 1;
                    }
                } else {
                    return responseJson;
                }
            } catch (JsonParserException e) {
                e.printStackTrace();
                throw new ParsingException("Failed parse response body: " + responseBody);
            }
        }

        DeviceForger.Device device = DeviceForger.requireRandomDevice();
        String msg = "BiliBili blocked us, we retried " + maxTry + " times, the last forged device is:\n"
                + device.info()
                + "\nTry to refresh, or report this!\n"
                + responseBody;
        isClientAPIMode = !isClientAPIMode; // flip API mode
        DeviceForger.regenerateRandomDevice(); // try to regenerate a new one
        throw new ParsingException(msg);
    }

    public static String encodeToBase64SubString(String raw) {
        ByteString byteString = ByteString.encodeUtf8(raw);
        String encodedString = byteString.base64();
        return encodedString.substring(0, encodedString.length() - 2);
    }
}
