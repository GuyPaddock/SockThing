package com.redbottledesign.bitcoin.pool.checkpoint;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FileBackedCheckpointer
implements CheckpointListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileBackedCheckpointer.class);

    private static final String FILE_STORE_UNPROCESSED_DIRECTORY_PATH   = "filestore/unprocessed";
    private static final String FILE_STORE_PROCESSED_DIRECTORY_PATH     = "filestore/processed";

    private final Set<Checkpointable> registeredCheckpointables;
    private final Map<Checkpoint, File> knownCheckpoints;

    public FileBackedCheckpointer()
    {
        this.registeredCheckpointables  = new LinkedHashSet<>();
        this.knownCheckpoints           = new HashMap<>();
    }

    public void setupCheckpointing(Checkpointable... checkpointables)
    {
        this.registeredCheckpointables.addAll(Arrays.asList(checkpointables));

        for (Checkpointable checkpointable : checkpointables)
        {
            checkpointable.registerCheckpointListener(this);
        }
    }

    public void restoreCheckpointsFromDisk()
    {
        File unprocessedFileStore = new File(FILE_STORE_UNPROCESSED_DIRECTORY_PATH);

        if (unprocessedFileStore.exists())
        {
            for (Checkpointable checkpointable : this.registeredCheckpointables)
            {
                Type                checkpointType      = checkpointable.getCheckpointType();
                List<File>          checkpointFiles     = this.getSavedCheckpoints(checkpointable);
                List<Checkpoint>    restoredCheckpoints = new LinkedList<>();

                for (File checkpointFile : checkpointFiles)
                {
                    try
                    {
                        if (LOGGER.isInfoEnabled())
                            LOGGER.info(String.format("Reading in saved checkpoint '%s'.", checkpointFile.getPath()));

                        restoredCheckpoints.add(this.readInCheckpoint(checkpointType, checkpointFile));
                    }

                    catch (IOException ex)
                    {
                        if (LOGGER.isErrorEnabled())
                        {
                            LOGGER.error(
                                String.format(
                                    "Failed to read-in checkpoint from '%s': %s\n%s",
                                    checkpointFile.toString(),
                                    ex.getMessage(),
                                    ExceptionUtils.getStackTrace(ex)));
                        }
                    }
                }

                checkpointable.restoreFromCheckpoints(restoredCheckpoints);
            }
        }
    }

    @Override
    public void onCheckpointItemCreated(Checkpointable checkpointable, Checkpoint checkpoint)
    {
        File checkpointFile =
            this.getFileForCheckpoint(FILE_STORE_UNPROCESSED_DIRECTORY_PATH, checkpointable, checkpoint);

        try
        {
            if (LOGGER.isInfoEnabled())
                LOGGER.info(String.format("Writing out pre-save checkpoint for '%s'.", checkpointFile.getPath()));

            this.writeOutCheckpoint(checkpoint, checkpointFile);
            this.knownCheckpoints.put(checkpoint, checkpointFile);
        }

        catch (IOException ex)
        {
            String error =
                String.format(
                    "Failed to write out checkpoint to '%s': %s\n\n%s\n%s",
                    checkpointFile.toString(),
                    checkpoint,
                    ex.getMessage(),
                    ExceptionUtils.getStackTrace(ex));

            if (LOGGER.isErrorEnabled())
                LOGGER.error(error);

            // This is pretty serious.
            System.err.printf(error);
            System.exit(-1);
        }
    }

    @Override
    public void onCheckpointItemExpired(Checkpointable checkpointable, Checkpoint checkpoint)
    {
        File checkpointFile = this.knownCheckpoints.get(checkpoint);

        if ((checkpointFile != null) && checkpointFile.exists())
        {
            File processedCheckpointFile =
                this.getFileForCheckpoint(FILE_STORE_PROCESSED_DIRECTORY_PATH, checkpointable, checkpoint);

            // Remove the original (now processed) file
            checkpointFile.delete();

            try
            {
                if (LOGGER.isInfoEnabled())
                {
                    LOGGER.info(
                        String.format(
                            "Writing out post-save, processed checkpoint for '%s'.",
                            checkpointFile.getPath()));
                }

                // Write-out the final copy of the checkpoint
                this.writeOutCheckpoint(checkpoint, processedCheckpointFile);
            }

            catch (IOException ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Failed to write out post-save, processed checkpoint for '%s': %s\n%s",
                            checkpointFile.getPath(),
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }
        }

        this.knownCheckpoints.remove(checkpoint);
    }

    protected void writeOutCheckpoint(Checkpoint checkpoint, File file)
    throws IOException
    {
        Gson    gson    = this.getGson();
        String  json    = gson.toJson(checkpoint);

        // Create directory structure
        file.getParentFile().mkdirs();

        try (FileWriter fileWriter = new FileWriter(file))
        {
            fileWriter.write(json);
        }
    }

    protected Checkpoint readInCheckpoint(Type checkpointType, File file)
    throws IOException
    {
        Checkpoint  result;
        Gson        gson    = this.getGson();

        try (FileReader fileReader = new FileReader(file))
        {
            result = gson.fromJson(fileReader, checkpointType);
        }

        return result;
    }

    protected List<File> getSavedCheckpoints(Checkpointable checkpointable)
    {
        List<File>  results         = new LinkedList<>();
        Deque<File> pathsToExplore  = new LinkedList<>();

        pathsToExplore.add(
            this.getCheckpointableStorageDirectory(FILE_STORE_UNPROCESSED_DIRECTORY_PATH, checkpointable));

        do
        {
            File currentPath = pathsToExplore.pop();

            if (currentPath.isFile())
                results.add(currentPath);

            else
                pathsToExplore.addAll(Arrays.asList(currentPath.listFiles()));
        }
        while (!pathsToExplore.isEmpty());

        Collections.sort(results);

        return results;
    }

    protected File getCheckpointableStorageDirectory(String basePath, Checkpointable checkpointable)
    {
        return new File(
            basePath                                + File.separator +
            checkpointable.getCheckpointableName()  + File.separator);
    }

    protected File getFileForCheckpoint(String basePath, Checkpointable checkpointable, Checkpoint checkpoint)
    {
        return new File(
            basePath                                + File.separator +
            checkpointable.getCheckpointableName()  + File.separator +
            checkpoint.getCheckpointType()          + File.separator +
            checkpoint.getCheckpointId()            + ".json");
    }

    protected Gson getGson()
    {
        return new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .registerTypeAdapterFactory(new PersistenceQueueItemTypeAdapterFactory())
            .create();
    }
}