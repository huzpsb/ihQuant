import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.InflaterInputStream;

public class DataLoader {
    private static byte[] decrypt(byte[] encryptedBytes) {
        byte[] decrypted = new byte[encryptedBytes.length];
        byte[] predefinedXor = "ihq_exp_hello".getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < encryptedBytes.length; i++) {
            byte b = (byte) (~encryptedBytes[i]);
            decrypted[i] = (byte) (b ^ predefinedXor[i % predefinedXor.length]);
        }
        return decrypted;
    }

    public static void main(String[] args) throws Exception {
        long now = System.currentTimeMillis();
        FileOutputStream fos = new FileOutputStream("ihq.bin");
        HttpURLConnection conn = (HttpURLConnection) new java.net.URL("https://gh-proxy.com/https://github.com/huzpsb/ihq_bin/raw/refs/heads/main/ihq.bin").openConnection();
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        is.close();
        fos.close();


        FileInputStream fis = new FileInputStream("ihq.bin");
        byte[] encryptedBytes = fis.readAllBytes();
        byte[] decryptedBytes = decrypt(encryptedBytes);
        fis.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(decryptedBytes);
        InflaterInputStream iis = new InflaterInputStream(bais);
        decryptedBytes = iis.readAllBytes();
        iis.close();
        String content = new String(decryptedBytes, StandardCharsets.UTF_8);

        Scanner scanner = new Scanner(content);
        String[] legends = scanner.nextLine().split(",");
        assert legends.length == 31;

        String legendNow = null;
        List<Item> itemsNow = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        List<List<Item>> allItems = new ArrayList<>();
        s:
        while (true) {
            if (!scanner.hasNextLine()) throw new IOException("Unexpected end of file");
            String[] line = scanner.nextLine().split(",");
            if (line.length == 1) {
                switch (line[0]) {
                    case "EOF":
                        break s;
                    case "E":
                        if (legendNow != null && itemsNow.size() == 31) {
                            allItems.add(itemsNow);
                            codes.add(legendNow);
                            itemsNow = new ArrayList<>();
                            legendNow = null;
                            continue;
                        } else {
                            throw new IOException("Unexpected E");
                        }
                    default:
                        throw new IOException("Unexpected line: " + line[0]);
                }
            }
            if (line.length == 7) {
                if (legendNow == null) {
                    legendNow = getIfLegend(line[0]);
                    if (legendNow == null) {
                        throw new IOException("Expected legend, got: " + line[0]);
                    }
                } else {
                    if (!legendNow.equals(line[0])) {
                        throw new IOException("Legend mismatch: " + legendNow + " vs " + line[0]);
                    }
                }
                double open = Double.parseDouble(line[1]);
                double close = Double.parseDouble(line[2]);
                double high = Double.parseDouble(line[3]);
                double low = Double.parseDouble(line[4]);
                double volume = Double.parseDouble(line[5]);
                double amount = Double.parseDouble(line[6]);
                itemsNow.add(new Item(open, close, high, low, volume, amount));
                continue;
            }
            throw new IOException("Unexpected line length: " + line.length);
        }
        scanner.close();
        Item[][] items = new Item[codes.size()][31];
        for (int i = 0; i < codes.size(); i++) {
            items[i] = allItems.get(i).toArray(new Item[0]);
        }
        Map<String, Integer> codeToIndex = new HashMap<>();
        for (int i = 0; i < codes.size(); i++) {
            codeToIndex.put(codes.get(i), i);
        }
        Map<String, Integer> dateToIndex = new HashMap<>();
        for (int i = 0; i < legends.length; i++) {
            dateToIndex.put(legends[i], i);
        }
        System.out.println("Data loaded in " + (System.currentTimeMillis() - now) + " ms.");

        // Example usage:
        System.out.println("Loaded " + codes.size() + " codes.");
        System.out.println("Dates: " + Arrays.toString(legends));
        String exampleCode = codes.get(0);
        String exampleDate = legends[0];
        int codeIndex = codeToIndex.get(exampleCode);
        int dateIndex = dateToIndex.get(exampleDate);
        Item exampleItem = items[codeIndex][dateIndex];
        System.out.println("Example item for code " + exampleCode + " on date " + exampleDate + ":");
        System.out.println(exampleItem);
    }

    private static String getIfLegend(String part) throws IOException {
        if (part.length() != 12) {
            return null;
        }
        String a = part.substring(0, 6);
        String b = part.substring(6, 12);
        if (!a.equals(b)) {
            throw new IOException("Invalid legend part: " + part);
        }
        return a;
    }
}

class Item {
    final double open;
    final double close;
    final double high;
    final double low;
    final double volume;
    final double amount;

    Item(double open, double close, double high, double low, double volume, double amount) {
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Item{" +
                "open=" + open +
                ", close=" + close +
                ", high=" + high +
                ", low=" + low +
                ", volume=" + volume +
                ", amount=" + amount +
                '}';
    }
}
