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

import com.github.fireduck64.sockthing.StratumServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;

public class FileBackedCheckpointer
implements Checkpointer, CheckpointListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileBackedCheckpointer.class);

    private static final String FILE_STORE_UNPROCESSED_DIRECTORY_PATH   = "filestore/unprocessed";
    private static final String FILE_STORE_PROCESSED_DIRECTORY_PATH     = "filestore/processed";

    private final StratumServer server;
    private final Set<Checkpointable> registeredCheckpointables;
    private final Map<CheckpointItem, File> knownCheckpointItems;

    public FileBackedCheckpointer(StratumServer server)
    {
        this.server                     = server;
        this.registeredCheckpointables  = new LinkedHashSet<>();
        this.knownCheckpointItems       = new HashMap<>();
    }

    @Override
    public void setupCheckpointing(Checkpointable... checkpointables)
    {
        this.registeredCheckpointables.addAll(Arrays.asList(checkpointables));

        for (Checkpointable checkpointable : checkpointables)
        {
            checkpointable.registerCheckpointListener(this);
        }
    }

    @Override
    public void restoreCheckpointsFromDisk()
    {
        File unprocessedFileStore = new File(FILE_STORE_UNPROCESSED_DIRECTORY_PATH);

        if (unprocessedFileStore.exists())
        {
            for (Checkpointable checkpointable : this.registeredCheckpointables)
            {
                Type                    itemType        = checkpointable.getCheckpointItemType();
                List<File>              checkpointFiles = this.getSavedCheckpoint(checkpointable);
                List<CheckpointItem>    restoredItems   = new LinkedList<>();

                for (File checkpointFile : checkpointFiles)
                {
                    try
                    {
                        CheckpointItem readCheckpointItem;

                        if (LOGGER.isInfoEnabled())
                            LOGGER.info(String.format("Reading in saved checkpoint '%s'.", checkpointFile.getPath()));

                        readCheckpointItem = this.readInCheckpointItem(itemType, checkpointFile);

                        restoredItems.add(readCheckpointItem);
                        this.knownCheckpointItems.put(readCheckpointItem, checkpointFile);
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

                checkpointable.restoreFromCheckpoint(restoredItems);
            }
        }
    }

    @Override
    public void onCheckpointItemCreated(Checkpointable checkpointable, CheckpointItem checkpointItem)
    {
        File checkpointFile =
            this.getCheckpointItemFile(FILE_STORE_UNPROCESSED_DIRECTORY_PATH, checkpointable, checkpointItem);

        try
        {
            if (LOGGER.isInfoEnabled())
                LOGGER.info(String.format("Writing out pre-save checkpoint item '%s'.", checkpointFile.getPath()));

            this.writeOutCheckpointItem(checkpointItem, checkpointFile);
            this.knownCheckpointItems.put(checkpointItem, checkpointFile);
        }

        catch (IOException ex)
        {
            String error =
                String.format(
                    "Failed to write out checkpoint item '%s': %s\n\n%s\n%s",
                    checkpointFile.toString(),
                    checkpointItem,
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
    public void onCheckpointItemExpired(Checkpointable checkpointable, CheckpointItem checkpointItem)
    {
        File checkpointFile = this.knownCheckpointItems.get(checkpointItem);

        if ((checkpointFile != null) && checkpointFile.exists())
        {
            File processedCheckpointFile =
                this.getCheckpointItemFile(FILE_STORE_PROCESSED_DIRECTORY_PATH, checkpointable, checkpointItem);

            // Remove the original (now processed) file
            checkpointFile.delete();

            try
            {
                if (LOGGER.isInfoEnabled())
                {
                    LOGGER.info(
                        String.format(
                            "Writing out post-save, processed checkpoint item '%s'.",
                            checkpointFile.getPath()));
                }

                // Write-out the final copy of the checkpoint
                this.writeOutCheckpointItem(checkpointItem, processedCheckpointFile);
            }

            catch (IOException ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Failed to write out post-save, processed checkpoint item '%s': %s\n%s",
                            checkpointFile.getPath(),
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }
        }

        // Stop tracking the processed item
        this.knownCheckpointItems.remove(checkpointItem);
    }

    protected void writeOutCheckpointItem(CheckpointItem checkpoint, File file)
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

    protected CheckpointItem readInCheckpointItem(Type checkpointType, File file)
    throws IOException
    {
        CheckpointItem  result;
        Gson            gson    = this.getGson();

        try (FileReader fileReader = new FileReader(file))
        {
            result = gson.fromJson(fileReader, checkpointType);
        }

        return result;
    }

    protected List<File> getSavedCheckpoint(Checkpointable checkpointable)
    {
        List<File>  results         = new LinkedList<>();
        Deque<File> pathsToExplore  = new LinkedList<>();

        pathsToExplore.add(
            this.getCheckpointStorageDirectory(FILE_STORE_UNPROCESSED_DIRECTORY_PATH, checkpointable));

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

    protected File getCheckpointStorageDirectory(String basePath, Checkpointable checkpointable)
    {
        return new File(
            basePath                                + File.separator +
            checkpointable.getCheckpointableName()  + File.separator);
    }

    protected File getCheckpointItemFile(String basePath, Checkpointable checkpointable, CheckpointItem checkpoint)
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
            .registerTypeAdapterFactory(new QueueItemTypeAdapterFactory())
            .registerTypeAdapter(PersistenceCallback.class, new PersistenceCallbackCreator(this.server))
//            .registerTypeAdapter(QueueItem.class, new QueueItemCreator())
            .create();
    }
}