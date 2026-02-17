/*
 * Written in 2026 by Daniel Saukel
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software
 * to the public domain worldwide.
 *
 * This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package io.github.sataniel98.mvnbootstrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MavenBootstrapper {

    public static final String LINK_CHECK = "https://dlcdn.apache.org/maven/maven-3";
    public static final String LINK = "https://dlcdn.apache.org/maven/maven-%major/%v/binaries/apache-maven-%v-bin.zip";

    public static void main(String[] args) throws IOException {
        String version = null;
        String goal = "package";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--version") || args[i].equalsIgnoreCase("--v")) {
                if (args.length > i + 1) {
                    version = args[i + 1];
                }

            } else if (args[i].equalsIgnoreCase("--goal") || args[i].equalsIgnoreCase("--g")) {
                if (args.length <= i + 1) {
                    continue;
                }
                if (args[i + 1].equalsIgnoreCase("package") || args[i + 1].equalsIgnoreCase("install")) {
                    goal = args[i + 1];
                }
            }
        }

        if (version == null) {
            try {
                version = getLatestMvn();
            } catch (IOException exception) {
                System.out.println("Could not fetch latest Maven version.");
                return;
            }
        }

        Path bin = Paths.get("apache-maven-" + version, "bin");
        if (!Files.exists(bin)) {
            try {
                downloadMvn(version);
            } catch (Exception exception) {
                System.out.println("Could not download Maven.");
                return;
            }
        }

        if (!new File("pom.xml").exists()) {
            System.out.println("No project to build found.");
            return;
        }

        System.out.println("Building project...");
        String[] arguments = {bin.toString() + File.separatorChar + getMvnScript(), "clean", goal};
        Process process = new ProcessBuilder(arguments).start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            System.out.println(line);
            line = reader.readLine();
        }
        System.out.println("See \"target\" directory.");
    }

    static String getLatestMvn() throws IOException {
        URL url = new URL(LINK_CHECK);
        System.out.println("Fetching latest stable Maven version...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line = reader.readLine();
        List<String> versions = new ArrayList<>();
        while (line != null) {
            if (line.contains("/icons/folder.gif")) {
                line = line.substring(51);
                line = line.split("/")[0];
                versions.add(line);
            }
            line = reader.readLine();
        }
        reader.close();

        String version = getHighestVersion(versions, 1);
        System.out.println("Latest stable Maven version is " + version + ".");
        return version;
    }

    static String getHighestVersion(List<String> versions, int i) {
        int verNum = 0;
        List<String> versionsEqual = new ArrayList<>(versions.size());
        for (String compV : versions) {
            String[] numsToCompare = compV.split("\\.");
            if (numsToCompare.length <= i) {
                continue;
            }
            int compNum = Integer.parseInt(numsToCompare[i]);

            if (compNum < verNum) {
                continue;
            }
            if (compNum > verNum) {
                verNum = compNum;
                versionsEqual.clear();
            }
            versionsEqual.add(compV);
        }

        if (versionsEqual.size() == 1) {
            return versionsEqual.get(0);
        } else {
            return getHighestVersion(versionsEqual, ++i);
        }
    }

    static void downloadMvn(String version) throws Exception {
        String link = LINK.replace("%major", version.split("\\.")[0]).replace("%v", version);
        System.out.println("Downloading Apache Maven from " + link + "...");

        URL url = new URL(link);
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream("mvn.zip");
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        fileOutputStream.close();

        File zipFile = new File("mvn.zip").getAbsoluteFile();
        InputStream zipFileStream = new FileInputStream(zipFile);
        Path parent = Paths.get(zipFile.getParent());
        ZipInputStream zipInputStream = new ZipInputStream(zipFileStream);

        ZipEntry entry = zipInputStream.getNextEntry();
        while (entry != null) {
            Path target = parent.resolve(entry.getName()).normalize();
            if (entry.isDirectory()) {
                Files.createDirectories(target);
            } else {
                Files.createDirectories(target.getParent());
                Files.copy(zipInputStream, target);
            }
            entry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
        zipFileStream.close();
        zipFile.delete();
    }

    static String getMvnScript() {
        if (System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {
            return "mvn.cmd";
        } else {
            return "mvn";
        }
    }
}
