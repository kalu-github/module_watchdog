package lib.kalu.monitor;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;
 final class RomUtil {

    public static String getRomTotal() {
        try {
            File dataDir = Environment.getDataDirectory();
            StatFs stat = new StatFs(dataDir.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long size = totalBlocks * blockSize;
            long GB = 1024 * 1024 * 1024;
            final long[] deviceRomMemoryMap = {2 * GB, 4 * GB, 8 * GB, 16 * GB, 32 * GB, 64 * GB, 128 * GB, 256 * GB, 512 * GB, 1024 * GB, 2048 * GB};
            String[] displayRomSize = {"2GB", "4GB", "8GB", "16GB", "32GB", "64GB", "128GB", "256GB", "512GB", "1024GB", "2048GB"};
            int i;
            for (i = 0; i < deviceRomMemoryMap.length; i++) {
                if (size <= deviceRomMemoryMap[i]) {
                    break;
                }
                if (i == deviceRomMemoryMap.length) {
                    i--;
                }
            }
            return displayRomSize[i];
        } catch (Exception e) {
            return "NA";
        }
    }
}
