package jeresources.profiling;

import jeresources.json.WorldGenAdapter;
import net.minecraft.command.ICommandSource;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashMap;
import java.util.Map;

public class ProfilingTimer {
    private final ICommandSource sender;
    private int totalChunks;
    private final Map<Integer, DimensionCounters> dimensionsMap = new HashMap<>();

    private static class DimensionCounters {
        public final long start = System.currentTimeMillis();
        public int chunkCounter;
        public int threadCounter;
        public boolean completed;
    }

    public ProfilingTimer(ICommandSource sender, int chunkCount) {
        this.sender = sender;
        this.totalChunks = chunkCount;
    }

    public void startChunk(int dim) {
        DimensionCounters counters = this.dimensionsMap.get(dim);
        if (counters == null) {
            counters = new DimensionCounters();
            this.dimensionsMap.put(dim, counters);
            send("[" + getDimensionName(dim) + "] Started profiling");
        }
        counters.threadCounter++;
    }

    public void endChunk(int dim) {
        DimensionCounters counters = dimensionsMap.get(dim);
        counters.threadCounter--;
        if (++counters.chunkCounter % 100 == 0)
            sendSpeed(dim);
        if (this.totalChunks == counters.chunkCounter)
            counters.completed = true;
    }

    public void complete() {
        for (int dim : this.dimensionsMap.keySet()) {
            DimensionCounters counters = dimensionsMap.get(dim);
            counters.completed = true;
            send("[" + getDimensionName(dim) + "] Completed profiling of " +
                (getBlocksPerLayer(dim) * ChunkProfiler.CHUNK_HEIGHT) + " blocks in " +
                (System.currentTimeMillis() - counters.start) + " ms saved to " + WorldGenAdapter.getWorldGenFile());
        }
    }

    public synchronized boolean isCompleted() {
        for (DimensionCounters counters : dimensionsMap.values())
            if (!counters.completed)
                return false;

        return true;
    }

    private void send(String s) {
        this.sender.sendMessage(new TranslationTextComponent(s));
    }

    private void sendSpeed(int dim) {
        DimensionCounters counters = dimensionsMap.get(dim);
        float time = (System.currentTimeMillis() - counters.start) * 1.0F / counters.chunkCounter;
        String message = "[" + getDimensionName(dim) + "] Scanned " +
            counters.chunkCounter + " chunks at " + String.format("%3.2f", time) + " ms/chunk";
        send(message);
    }

    public long getBlocksPerLayer(int dim) {
        DimensionCounters counters = dimensionsMap.get(dim);
        return counters.chunkCounter * ChunkProfiler.CHUNK_SIZE * ChunkProfiler.CHUNK_SIZE;
    }

    private static String getDimensionName(int dim) {
        DimensionType dimensionType = DimensionType.getById(dim);
        if (dimensionType == null) {
            return "Dim " + dim;
        } else {
            return "Dim " + dim + ": " + dimensionType.getRegistryName();
        }
    }
}
