package app.owlcms.utils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class ZipUtils {
    
    final static Logger logger = (Logger) LoggerFactory.getLogger(ZipUtils.class);

    /**
     * @param source zip stream
     * @param target target directory
     * @throws IOException extraction failed
     */
    public static void unzip(InputStream source, File target) throws IOException {
        final ZipInputStream zipStream = new ZipInputStream(source);
        ZipEntry nextEntry;
        while ((nextEntry = zipStream.getNextEntry()) != null) {
            final String name = nextEntry.getName();
            // only extract files
            if (!name.endsWith("/")) {
                final File nextFile = new File(target, name);
                logger.debug("unzipping {}", nextFile.getAbsolutePath());

                // create directories
                final File parent = nextFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }

                // write file
                try (OutputStream targetStream = new FileOutputStream(nextFile)) {
                    copy(zipStream, targetStream);
                }
            }
        }
    }

    private static void copy(final InputStream source, final OutputStream target) throws IOException {
        final int bufferSize = 4 * 1024;
        final byte[] buffer = new byte[bufferSize];

        int nextCount;
        while ((nextCount = source.read(buffer)) >= 0) {
            target.write(buffer, 0, nextCount);
        }
    }
}